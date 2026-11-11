package ui;

import db.DBConnection;
import model.Exam;
import model.Question;
import model.User;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamFrame extends JFrame {
    private Exam exam;
    private User student;
    private int submissionId;
    private int remainingSeconds;
    private int focusLossCount = 0;

    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private Map<Integer, String> studentAnswers = new HashMap<>();

    private JLabel timerLabel;
    private JLabel focusLossLabel;
    private JButton submitBtn;

    private JPanel questionContainer;
    private JSplitPane splitPane;
    private JPanel leftStemContainer;
    private JTextArea stemTextArea;

    private JPanel rightAnswerContainer;
    private JLabel questionTitleLabel;
    private JLabel questionMarksLabel;
    private JTextArea questionPromptArea;

    private JPanel mcqOptionsPanel;
    private ButtonGroup mcqButtonGroup;
    private JRadioButton optARadio, optBRadio, optCRadio, optDRadio;

    private JPanel cqAnswerPanel;
    private JTextArea cqAnswerTextArea;
    private JLabel wordCountLabel;

    private JPanel navigatorPanel;
    private List<JButton> navButtons = new ArrayList<>();

    private Timer countdownTimer;
    private Timer autoSaveTimer;

    // INPUT: Exam exam, User student -> ACTION: Boots up active exam room -> OUTPUT: None
    public ExamFrame(Exam exam, User student) {
        this.exam = exam;
        this.student = student;

        setTitle("Exam Room: " + exam.getTitle() + " | Candidate: " + student.getFullName());
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        initDatabaseSubmission();
        loadExamQuestions();
        loadExistingAnswers();

        buildUI();
        setupAntiCheatListener();
        setupTimers();

        if (!questions.isEmpty()) {
            displayQuestion(0);
        }
    }

    private void initDatabaseSubmission() {
        try (Connection conn = DBConnection.getConnection()) {
            String checkSql = "SELECT id, remaining_seconds, focus_loss_count FROM submissions WHERE exam_id = ? AND student_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                ps.setInt(1, exam.getId());
                ps.setInt(2, student.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        this.submissionId = rs.getInt("id");
                        this.remainingSeconds = rs.getInt("remaining_seconds");
                        this.focusLossCount = rs.getInt("focus_loss_count");
                    } else {
                        this.remainingSeconds = exam.getDurationMinutes() * 60;
                        this.focusLossCount = 0;
                        String insertSql = "INSERT INTO submissions (exam_id, student_id, remaining_seconds, focus_loss_count, status) VALUES (?, ?, ?, 0, 'IN_PROGRESS')";
                        try (PreparedStatement insPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                            insPs.setInt(1, exam.getId());
                            insPs.setInt(2, student.getId());
                            insPs.setInt(3, remainingSeconds);
                            insPs.executeUpdate();
                            try (ResultSet genKeys = insPs.getGeneratedKeys()) {
                                if (genKeys.next()) {
                                    this.submissionId = genKeys.getInt(1);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadExamQuestions() {
        questions.clear();
        String sql = "SELECT * FROM questions WHERE exam_id = ? ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, exam.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questions.add(new Question(
                            rs.getInt("id"),
                            rs.getInt("exam_id"),
                            rs.getString("type"),
                            rs.getString("stem"),
                            rs.getString("stem_group_id"),
                            rs.getString("sub_label"),
                            rs.getString("question_text"),
                            rs.getString("option_a"),
                            rs.getString("option_b"),
                            rs.getString("option_c"),
                            rs.getString("option_d"),
                            rs.getString("correct_option"),
                            rs.getString("model_answer"),
                            rs.getDouble("marks"),
                            rs.getInt("word_limit")
                    ));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadExistingAnswers() {
        studentAnswers.clear();
        String sql = "SELECT question_id, answer_text FROM student_answers WHERE submission_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, submissionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    studentAnswers.put(rs.getInt("question_id"), rs.getString("answer_text"));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void buildUI() {
        JPanel rootPanel = new JPanel(new BorderLayout(8, 8));
        rootPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top Bar
        JPanel topBar = new JPanel(new BorderLayout(15, 15));
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel examTitle = new JLabel(exam.getTitle());
        examTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));

        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        focusLossLabel = new JLabel("Warning: Focus Losses: " + focusLossCount);
        focusLossLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        focusLossLabel.setForeground(new Color(220, 53, 69));

        timerLabel = new JLabel("Time Remaining: 00:00");
        timerLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        submitBtn = new JButton("Submit Exam");
        submitBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        submitBtn.setBackground(new Color(40, 167, 69));
        submitBtn.setForeground(Color.WHITE);
        submitBtn.setFocusPainted(false);
        submitBtn.addActionListener(e -> promptManualSubmit());

        statusPanel.add(focusLossLabel);
        statusPanel.add(timerLabel);
        statusPanel.add(submitBtn);

        topBar.add(examTitle, BorderLayout.WEST);
        topBar.add(statusPanel, BorderLayout.EAST);
        rootPanel.add(topBar, BorderLayout.NORTH);

        // Center SplitPane
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.40);

        leftStemContainer = new JPanel(new BorderLayout(5, 5));
        leftStemContainer.setBorder(BorderFactory.createTitledBorder("Scenario Stem / Context"));
        stemTextArea = new JTextArea();
        stemTextArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        stemTextArea.setLineWrap(true);
        stemTextArea.setWrapStyleWord(true);
        stemTextArea.setEditable(false);
        leftStemContainer.add(new JScrollPane(stemTextArea), BorderLayout.CENTER);

        rightAnswerContainer = new JPanel(new BorderLayout(10, 10));
        rightAnswerContainer.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 5));

        JPanel qHeader = new JPanel(new BorderLayout());
        questionTitleLabel = new JLabel("Question 1");
        questionTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        questionMarksLabel = new JLabel("[Marks: 0.0]");
        questionMarksLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        qHeader.add(questionTitleLabel, BorderLayout.WEST);
        qHeader.add(questionMarksLabel, BorderLayout.EAST);

        questionPromptArea = new JTextArea(4, 20);
        questionPromptArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        questionPromptArea.setLineWrap(true);
        questionPromptArea.setWrapStyleWord(true);
        questionPromptArea.setEditable(false);
        questionPromptArea.setOpaque(false);

        JPanel promptHolder = new JPanel(new BorderLayout(5, 5));
        promptHolder.add(qHeader, BorderLayout.NORTH);
        promptHolder.add(new JScrollPane(questionPromptArea), BorderLayout.CENTER);
        rightAnswerContainer.add(promptHolder, BorderLayout.NORTH);

        questionContainer = new JPanel(new CardLayout());

        // MCQ
        mcqOptionsPanel = new JPanel(new GridLayout(4, 1, 8, 8));
        mcqOptionsPanel.setBorder(BorderFactory.createTitledBorder("Select Choice"));
        mcqButtonGroup = new ButtonGroup();
        optARadio = new JRadioButton();
        optBRadio = new JRadioButton();
        optCRadio = new JRadioButton();
        optDRadio = new JRadioButton();

        optARadio.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        optBRadio.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        optCRadio.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        optDRadio.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        ItemListener radioSaveListener = e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                if (optARadio.isSelected()) studentAnswers.put(questions.get(currentIndex).getId(), "A");
                else if (optBRadio.isSelected()) studentAnswers.put(questions.get(currentIndex).getId(), "B");
                else if (optCRadio.isSelected()) studentAnswers.put(questions.get(currentIndex).getId(), "C");
                else if (optDRadio.isSelected()) studentAnswers.put(questions.get(currentIndex).getId(), "D");
                updateNavigatorButtons();
            }
        };

        optARadio.addItemListener(radioSaveListener);
        optBRadio.addItemListener(radioSaveListener);
        optCRadio.addItemListener(radioSaveListener);
        optDRadio.addItemListener(radioSaveListener);

        mcqButtonGroup.add(optARadio);
        mcqButtonGroup.add(optBRadio);
        mcqButtonGroup.add(optCRadio);
        mcqButtonGroup.add(optDRadio);

        mcqOptionsPanel.add(optARadio);
        mcqOptionsPanel.add(optBRadio);
        mcqOptionsPanel.add(optCRadio);
        mcqOptionsPanel.add(optDRadio);

        // CQ
        cqAnswerPanel = new JPanel(new BorderLayout(5, 5));
        cqAnswerPanel.setBorder(BorderFactory.createTitledBorder("Student Written Answer"));

        JPanel mathBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        String[] symbols = {"²", "³", "√", "÷", "×", "π", "≤", "≥", "≠", "±", "θ", "Δ", "∑", "∞"};
        for (String sym : symbols) {
            JButton symBtn = new JButton(sym);
            symBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            symBtn.setMargin(new Insets(2, 6, 2, 6));
            symBtn.setFocusPainted(false);
            symBtn.addActionListener(e -> cqAnswerTextArea.insert(sym, cqAnswerTextArea.getCaretPosition()));
            mathBar.add(symBtn);
        }

        cqAnswerTextArea = new JTextArea();
        cqAnswerTextArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        cqAnswerTextArea.setLineWrap(true);
        cqAnswerTextArea.setWrapStyleWord(true);

        cqAnswerTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { checkWordLimit(); }
            @Override
            public void removeUpdate(DocumentEvent e) { checkWordLimit(); }
            @Override
            public void changedUpdate(DocumentEvent e) { checkWordLimit(); }
        });

        JPanel cqBottomBar = new JPanel(new BorderLayout());
        wordCountLabel = new JLabel("Word Count: 0 / 0");
        wordCountLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cqBottomBar.add(wordCountLabel, BorderLayout.WEST);

        cqAnswerPanel.add(mathBar, BorderLayout.NORTH);
        cqAnswerPanel.add(new JScrollPane(cqAnswerTextArea), BorderLayout.CENTER);
        cqAnswerPanel.add(cqBottomBar, BorderLayout.SOUTH);

        questionContainer.add(mcqOptionsPanel, "MCQ");
        questionContainer.add(cqAnswerPanel, "CQ");
        rightAnswerContainer.add(questionContainer, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftStemContainer);
        splitPane.setRightComponent(rightAnswerContainer);
        rootPanel.add(splitPane, BorderLayout.CENTER);

        // Navigator
        JPanel bottomNav = new JPanel(new BorderLayout(5, 5));
        navigatorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));

        for (int i = 0; i < questions.size(); i++) {
            final int idx = i;
            String label = "Q" + (i + 1);
            if (!questions.get(i).getSubLabel().isEmpty()) {
                label = questions.get(i).getSubLabel();
            }
            JButton btn = new JButton(label);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setFocusPainted(false);
            btn.setPreferredSize(new Dimension(55, 35));
            btn.addActionListener(e -> {
                saveCurrentAnswerToMemory();
                displayQuestion(idx);
            });
            navButtons.add(btn);
            navigatorPanel.add(btn);
        }

        JPanel arrowControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 2));
        JButton prevBtn = new JButton("◄ Previous");
        JButton nextBtn = new JButton("Next ►");

        prevBtn.addActionListener(e -> {
            if (currentIndex > 0) {
                saveCurrentAnswerToMemory();
                displayQuestion(currentIndex - 1);
            }
        });

        nextBtn.addActionListener(e -> {
            if (currentIndex < questions.size() - 1) {
                saveCurrentAnswerToMemory();
                displayQuestion(currentIndex + 1);
            }
        });

        arrowControls.add(prevBtn);
        arrowControls.add(nextBtn);

        bottomNav.add(arrowControls, BorderLayout.NORTH);
        bottomNav.add(new JScrollPane(navigatorPanel), BorderLayout.CENTER);
        rootPanel.add(bottomNav, BorderLayout.SOUTH);

        add(rootPanel);
        updateNavigatorButtons();
    }

    private void checkWordLimit() {
        if (currentIndex < 0 || currentIndex >= questions.size()) return;
        Question q = questions.get(currentIndex);
        if (!"CQ".equals(q.getType())) return;

        String text = cqAnswerTextArea.getText().trim();
        int words = text.isEmpty() ? 0 : text.split("\\s+").length;
        int limit = q.getWordLimit();

        if (limit > 0) {
            wordCountLabel.setText("Word Count: " + words + " / " + limit);
            if (words > limit) {
                wordCountLabel.setForeground(Color.RED);
                String[] split = text.split("\\s+");
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < limit; i++) {
                    sb.append(split[i]).append(i == limit - 1 ? "" : " ");
                }
                SwingUtilities.invokeLater(() -> cqAnswerTextArea.setText(sb.toString()));
            } else {
                wordCountLabel.setForeground(Color.BLACK);
            }
        } else {
            wordCountLabel.setText("Word Count: " + words + " (No Limit)");
            wordCountLabel.setForeground(Color.BLACK);
        }
    }

    private void displayQuestion(int index) {
        this.currentIndex = index;
        Question q = questions.get(index);

        String title = "Question " + (index + 1);
        if (!q.getSubLabel().isEmpty()) {
            title += " " + q.getSubLabel();
        }
        questionTitleLabel.setText(title + " (" + q.getType() + ")");
        questionMarksLabel.setText("[Marks: " + q.getMarks() + "]");
        questionPromptArea.setText(q.getQuestionText());

        boolean hasStem = (q.getStem() != null && !q.getStem().trim().isEmpty());
        if (hasStem) {
            stemTextArea.setText(q.getStem());
            leftStemContainer.setVisible(true);
            splitPane.setDividerLocation(0.40);
            splitPane.setDividerSize(6);
        } else {
            leftStemContainer.setVisible(false);
            splitPane.setDividerLocation(0.0);
            splitPane.setDividerSize(0);
        }

        CardLayout cl = (CardLayout) questionContainer.getLayout();

        if ("MCQ".equals(q.getType())) {
            cl.show(questionContainer, "MCQ");
            optARadio.setText("A) " + q.getOptionA());
            optBRadio.setText("B) " + q.getOptionB());
            optCRadio.setText("C) " + q.getOptionC());
            optDRadio.setText("D) " + q.getOptionD());

            mcqButtonGroup.clearSelection();
            String saved = studentAnswers.get(q.getId());
            if ("A".equals(saved)) optARadio.setSelected(true);
            else if ("B".equals(saved)) optBRadio.setSelected(true);
            else if ("C".equals(saved)) optCRadio.setSelected(true);
            else if ("D".equals(saved)) optDRadio.setSelected(true);
        } else {
            cl.show(questionContainer, "CQ");
            String saved = studentAnswers.get(q.getId());
            cqAnswerTextArea.setText(saved != null ? saved : "");
            checkWordLimit();
        }

        updateNavigatorButtons();
    }

    private void saveCurrentAnswerToMemory() {
        if (currentIndex < 0 || currentIndex >= questions.size()) return;
        Question q = questions.get(currentIndex);
        if ("CQ".equals(q.getType())) {
            studentAnswers.put(q.getId(), cqAnswerTextArea.getText().trim());
        }
        updateNavigatorButtons();
    }

    private void updateNavigatorButtons() {
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            JButton btn = navButtons.get(i);
            String ans = studentAnswers.get(q.getId());
            boolean isAnswered = (ans != null && !ans.trim().isEmpty());

            if (isAnswered) {
                btn.setBackground(new Color(40, 167, 69));
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(Color.LIGHT_GRAY);
                btn.setForeground(Color.BLACK);
            }

            if (i == currentIndex) {
                btn.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
            } else {
                btn.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            }
        }
    }

    private synchronized void autoSaveToDatabase() {
        saveCurrentAnswerToMemory();
        try (Connection conn = DBConnection.getConnection()) {
            String subSql = "UPDATE submissions SET remaining_seconds = ?, focus_loss_count = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(subSql)) {
                ps.setInt(1, remainingSeconds);
                ps.setInt(2, focusLossCount);
                ps.setInt(3, submissionId);
                ps.executeUpdate();
            }

            String ansSql = "INSERT INTO student_answers (submission_id, question_id, answer_text) VALUES (?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE answer_text = VALUES(answer_text)";
            try (PreparedStatement ps = conn.prepareStatement(ansSql)) {
                for (Map.Entry<Integer, String> entry : studentAnswers.entrySet()) {
                    ps.setInt(1, submissionId);
                    ps.setInt(2, entry.getKey());
                    ps.setString(3, entry.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Anti-Cheat: window focus loss listener with immediate DB notification
    private void setupAntiCheatListener() {
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                focusLossCount++;
                focusLossLabel.setText("Warning: Focus Losses: " + focusLossCount);
                // Immediately update database so invigilator / teacher sees it live
                autoSaveToDatabase();
            }
        });
    }

    private void setupTimers() {
        countdownTimer = new Timer(1000, e -> {
            remainingSeconds--;
            int mins = remainingSeconds / 60;
            int secs = remainingSeconds % 60;
            timerLabel.setText(String.format("Time Remaining: %02d:%02d", mins, secs));

            if (remainingSeconds <= 300) {
                timerLabel.setForeground(Color.RED);
            }

            if (remainingSeconds <= 0) {
                countdownTimer.stop();
                autoSaveTimer.stop();
                JOptionPane.showMessageDialog(this, "Time has expired! Submitting your exam automatically.", "Time Out", JOptionPane.WARNING_MESSAGE);
                finalizeSubmission();
            }
        });
        countdownTimer.start();

        autoSaveTimer = new Timer(10000, e -> autoSaveToDatabase());
        autoSaveTimer.start();
    }

    private void promptManualSubmit() {
        saveCurrentAnswerToMemory();
        int unanswered = 0;
        for (Question q : questions) {
            String ans = studentAnswers.get(q.getId());
            if (ans == null || ans.trim().isEmpty()) {
                unanswered++;
            }
        }

        String msg = (unanswered > 0)
                ? "You have " + unanswered + " unanswered question(s). Are you sure you want to finalize submission?"
                : "Are you sure you want to submit your exam? You cannot modify answers after submission.";

        int res = JOptionPane.showConfirmDialog(this, msg, "Confirm Final Submission", JOptionPane.YES_NO_OPTION);
        if (res == JOptionPane.YES_OPTION) {
            finalizeSubmission();
        }
    }

    private void finalizeSubmission() {
        if (countdownTimer != null) countdownTimer.stop();
        if (autoSaveTimer != null) autoSaveTimer.stop();

        saveCurrentAnswerToMemory();
        autoSaveToDatabase();

        double mcqTotal = 0.0;
        try (Connection conn = DBConnection.getConnection()) {
            for (Question q : questions) {
                if ("MCQ".equals(q.getType())) {
                    String studentAns = studentAnswers.get(q.getId());
                    if (studentAns != null && studentAns.equalsIgnoreCase(q.getCorrectOption())) {
                        mcqTotal += q.getMarks();
                        String updateAnsSql = "UPDATE student_answers SET awarded_marks = ? WHERE submission_id = ? AND question_id = ?";
                        try (PreparedStatement ps = conn.prepareStatement(updateAnsSql)) {
                            ps.setDouble(1, q.getMarks());
                            ps.setInt(2, submissionId);
                            ps.setInt(3, q.getId());
                            ps.executeUpdate();
                        }
                    }
                }
            }

            String lockSql = "UPDATE submissions SET status = 'SUBMITTED', mcq_score = ?, total_score = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setDouble(1, mcqTotal);
                ps.setDouble(2, mcqTotal);
                ps.setInt(3, submissionId);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Exam submitted successfully! All responses are locked.", "Completed", JOptionPane.INFORMATION_MESSAGE);
            new StudentDashboard(student).setVisible(true);
            dispose();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}