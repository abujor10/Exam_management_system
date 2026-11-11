package ui;

import db.DBConnection;
import model.User;
import util.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class TeacherDashboard extends JFrame {
    private User currentUser;

    // Exam Management Components
    private JTable examTable;
    private DefaultTableModel examTableModel;
    private JTextField examTitleField;
    private JTextField examDurationField;
    private JTextField examDateField;
    private JTextField examPassMarksField;

    // Candidate Enrollment Tab Components
    private JComboBox<String> enrollExamCombo;
    private JTable enrollStudentTable;
    private DefaultTableModel enrollStudentTableModel;

    // Question Management Components
    private JComboBox<String> examSelectorCombo;
    private JComboBox<String> questionTypeCombo;
    private JPanel dynamicCardPanel;
    private CardLayout cardLayout;

    // MCQ
    private JTextArea mcqQuestionArea;
    private JTextField optAField, optBField, optCField, optDField;
    private JComboBox<String> correctOptCombo;
    private JTextField mcqMarksField;

    // CQ Multi-Part Builder (a, b, c, d)
    private JTextArea cqStemArea;
    private JComboBox<String> cqSubPartCombo;
    private JTextArea cqQuestionArea;
    private JTextArea cqModelAnswerArea;
    private JTextField cqMarksField;
    private JTextField cqWordLimitField;

    private JTable questionTable;
    private DefaultTableModel questionTableModel;

    // INPUT: User user -> ACTION: Sets up teacher workspace with exam creation, student enrollment, and question authoring -> OUTPUT: None
    public TeacherDashboard(User user) {
        this.currentUser = user;
        setTitle("Teacher Workspace - " + user.getFullName());
        setSize(1150, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Teacher Workspace: " + user.getFullName());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel headerControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton themeBtn = new JButton("Toggle Theme");
        themeBtn.setFocusPainted(false);
        themeBtn.addActionListener(e -> ThemeManager.toggleTheme(this));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });

        headerControls.add(themeBtn);
        headerControls.add(logoutBtn);
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(headerControls, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.addTab("1. My Exams", createExamsPanel());
        tabbedPane.addTab("2. Enroll My Students to Exam", createEnrollmentPanel());
        tabbedPane.addTab("3. Question Builder (MCQ / ABCD CQ)", createQuestionBuilderPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);

        refreshExamTable();
        refreshExamSelectors();
    }

    private JPanel createExamsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Create New Examination"));

        examTitleField = new JTextField();
        examDurationField = new JTextField("30");
        examDateField = new JTextField(new Date(System.currentTimeMillis()).toString());
        examPassMarksField = new JTextField("40.0");

        formPanel.add(new JLabel("Exam Title:"));
        formPanel.add(examTitleField);
        formPanel.add(new JLabel("Duration (Minutes):"));
        formPanel.add(examDurationField);
        formPanel.add(new JLabel("Exam Date (YYYY-MM-DD):"));
        formPanel.add(examDateField);
        formPanel.add(new JLabel("Passing Marks:"));
        formPanel.add(examPassMarksField);

        JButton createExamBtn = new JButton("Save New Exam");
        createExamBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        createExamBtn.addActionListener(e -> saveExam());
        formPanel.add(new JLabel(""));
        formPanel.add(createExamBtn);

        panel.add(formPanel, BorderLayout.NORTH);

        examTableModel = new DefaultTableModel(new String[]{"ID", "Title", "Duration (Min)", "Date", "Pass Mark", "Published Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        examTable = new JTable(examTableModel);
        examTable.setRowHeight(24);

        panel.add(new JScrollPane(examTable), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton openGradingBtn = new JButton("Open Grading Console for Selected Exam");
        openGradingBtn.setBackground(new Color(40, 167, 69));
        openGradingBtn.setForeground(Color.WHITE);
        openGradingBtn.setFocusPainted(false);
        openGradingBtn.addActionListener(e -> openGradingForSelectedExam());

        bottomBar.add(openGradingBtn);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    // INPUT: None -> ACTION: Constructs UI panel allowing teacher to enroll their assigned students into a chosen exam -> OUTPUT: JPanel
    private JPanel createEnrollmentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topBar.add(new JLabel("Target Exam:"));
        enrollExamCombo = new JComboBox<>();
        enrollExamCombo.setPreferredSize(new Dimension(300, 30));
        enrollExamCombo.addActionListener(e -> refreshEnrollStudentTable());
        topBar.add(enrollExamCombo);

        JButton selectAllBtn = new JButton("Select All My Students");
        selectAllBtn.addActionListener(e -> setAllEnrollmentCheckboxes(true));
        JButton deselectAllBtn = new JButton("Clear All");
        deselectAllBtn.addActionListener(e -> setAllEnrollmentCheckboxes(false));

        topBar.add(selectAllBtn);
        topBar.add(deselectAllBtn);
        panel.add(topBar, BorderLayout.NORTH);

        enrollStudentTableModel = new DefaultTableModel(new String[]{"Enroll", "Student ID", "Full Name", "Roll Number"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        enrollStudentTable = new JTable(enrollStudentTableModel);
        enrollStudentTable.setRowHeight(26);
        panel.add(new JScrollPane(enrollStudentTable), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton saveEnrollmentBtn = new JButton("Save Candidate Enrollment to Exam");
        saveEnrollmentBtn.setBackground(new Color(40, 167, 69));
        saveEnrollmentBtn.setForeground(Color.WHITE);
        saveEnrollmentBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveEnrollmentBtn.setFocusPainted(false);
        saveEnrollmentBtn.addActionListener(e -> saveExamEnrollments());
        bottomBar.add(saveEnrollmentBtn);

        panel.add(bottomBar, BorderLayout.SOUTH);
        return panel;
    }

    // INPUT: boolean checked -> ACTION: Checks or unchecks all checkboxes in enrollment table -> OUTPUT: None
    private void setAllEnrollmentCheckboxes(boolean checked) {
        for (int i = 0; i < enrollStudentTableModel.getRowCount(); i++) {
            enrollStudentTableModel.setValueAt(checked, i, 0);
        }
    }

    // INPUT: None -> ACTION: Loads only students assigned to this teacher and indicates whether enrolled in the selected exam -> OUTPUT: None
    private void refreshEnrollStudentTable() {
        enrollStudentTableModel.setRowCount(0);
        int examId = getComboSelectedId(enrollExamCombo);
        if (examId == -1) return;

        String sql = "SELECT u.id, u.full_name, u.roll_number, " +
                "(SELECT COUNT(*) FROM exam_students es WHERE es.exam_id = ? AND es.student_id = u.id) AS is_enrolled " +
                "FROM users u " +
                "JOIN teacher_students ts ON u.id = ts.student_id " +
                "WHERE ts.teacher_id = ? ORDER BY u.roll_number ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ps.setInt(2, currentUser.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean enrolled = rs.getInt("is_enrolled") > 0;
                    enrollStudentTableModel.addRow(new Object[]{
                            enrolled,
                            rs.getInt("id"),
                            rs.getString("full_name"),
                            rs.getString("roll_number")
                    });
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // INPUT: None -> ACTION: Persists checked student rows into exam_students table -> OUTPUT: None
    private void saveExamEnrollments() {
        int examId = getComboSelectedId(enrollExamCombo);
        if (examId == -1) {
            JOptionPane.showMessageDialog(this, "Select an exam first.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            String deleteSql = "DELETE FROM exam_students WHERE exam_id = ?";
            try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                psDel.setInt(1, examId);
                psDel.executeUpdate();
            }

            String insertSql = "INSERT INTO exam_students (exam_id, student_id) VALUES (?, ?)";
            try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < enrollStudentTableModel.getRowCount(); i++) {
                    Boolean isChecked = (Boolean) enrollStudentTableModel.getValueAt(i, 0);
                    if (Boolean.TRUE.equals(isChecked)) {
                        int studentId = (Integer) enrollStudentTableModel.getValueAt(i, 1);
                        psIns.setInt(1, examId);
                        psIns.setInt(2, studentId);
                        psIns.addBatch();
                    }
                }
                psIns.executeBatch();
            }

            conn.commit();
            conn.setAutoCommit(true);
            JOptionPane.showMessageDialog(this, "Exam enrollment updated successfully. Only checked students can now see or take this exam.", "Enrollment Saved", JOptionPane.INFORMATION_MESSAGE);
            refreshEnrollStudentTable();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to update enrollment: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createQuestionBuilderPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel selectorPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        selectorPanel.setBorder(BorderFactory.createTitledBorder("Target Examination & Format"));

        examSelectorCombo = new JComboBox<>();
        examSelectorCombo.addActionListener(e -> refreshQuestionTable());

        questionTypeCombo = new JComboBox<>(new String[]{"MCQ", "CQ"});
        questionTypeCombo.addActionListener(e -> {
            String selected = (String) questionTypeCombo.getSelectedItem();
            cardLayout.show(dynamicCardPanel, selected);
        });

        selectorPanel.add(new JLabel("Assign to Exam:"));
        selectorPanel.add(examSelectorCombo);
        selectorPanel.add(new JLabel("Question Format:"));
        selectorPanel.add(questionTypeCombo);

        cardLayout = new CardLayout();
        dynamicCardPanel = new JPanel(cardLayout);

        // MCQ Card
        JPanel mcqCard = new JPanel(new BorderLayout(5, 5));
        JPanel mcqInputGrid = new JPanel(new GridLayout(6, 2, 6, 6));

        mcqQuestionArea = new JTextArea(3, 20);
        mcqQuestionArea.setLineWrap(true);
        optAField = new JTextField();
        optBField = new JTextField();
        optCField = new JTextField();
        optDField = new JTextField();
        correctOptCombo = new JComboBox<>(new String[]{"A", "B", "C", "D"});
        mcqMarksField = new JTextField("2.0");

        mcqInputGrid.add(new JLabel("Option A:"));
        mcqInputGrid.add(optAField);
        mcqInputGrid.add(new JLabel("Option B:"));
        mcqInputGrid.add(optBField);
        mcqInputGrid.add(new JLabel("Option C:"));
        mcqInputGrid.add(optCField);
        mcqInputGrid.add(new JLabel("Option D:"));
        mcqInputGrid.add(optDField);
        mcqInputGrid.add(new JLabel("Correct Option:"));
        mcqInputGrid.add(correctOptCombo);
        mcqInputGrid.add(new JLabel("Marks:"));
        mcqInputGrid.add(mcqMarksField);

        JPanel mcqTopContainer = new JPanel(new BorderLayout(5, 5));
        mcqTopContainer.add(new JLabel("Question Prompt:"), BorderLayout.NORTH);
        mcqTopContainer.add(new JScrollPane(mcqQuestionArea), BorderLayout.CENTER);
        mcqCard.add(mcqTopContainer, BorderLayout.NORTH);
        mcqCard.add(mcqInputGrid, BorderLayout.CENTER);

        // CQ Card (supports a, b, c, d pattern with separate marks & word limits)
        JPanel cqCard = new JPanel(new GridLayout(6, 2, 6, 6));
        cqStemArea = new JTextArea(2, 20);
        cqStemArea.setLineWrap(true);
        cqSubPartCombo = new JComboBox<>(new String[]{"Standalone (No Part)", "(a)", "(b)", "(c)", "(d)"});
        cqQuestionArea = new JTextArea(2, 20);
        cqQuestionArea.setLineWrap(true);
        cqModelAnswerArea = new JTextArea(2, 20);
        cqModelAnswerArea.setLineWrap(true);
        cqMarksField = new JTextField("4.0");
        cqWordLimitField = new JTextField("100");

        cqCard.add(new JLabel("Scenario Context / Stem Header:"));
        cqCard.add(new JScrollPane(cqStemArea));
        cqCard.add(new JLabel("Sub-Question Part:"));
        cqCard.add(cqSubPartCombo);
        cqCard.add(new JLabel("Question Text:"));
        cqCard.add(new JScrollPane(cqQuestionArea));
        cqCard.add(new JLabel("Teacher Model Answer:"));
        cqCard.add(new JScrollPane(cqModelAnswerArea));
        cqCard.add(new JLabel("Marks for this specific part:"));
        cqCard.add(cqMarksField);
        cqCard.add(new JLabel("Word Limit:"));
        cqCard.add(cqWordLimitField);

        dynamicCardPanel.add(mcqCard, "MCQ");
        dynamicCardPanel.add(cqCard, "CQ");

        JPanel formContainer = new JPanel(new BorderLayout(5, 5));
        formContainer.add(selectorPanel, BorderLayout.NORTH);
        formContainer.add(dynamicCardPanel, BorderLayout.CENTER);

        JButton addQuestionBtn = new JButton("Append Question Part to Exam");
        addQuestionBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addQuestionBtn.addActionListener(e -> saveQuestion());
        formContainer.add(addQuestionBtn, BorderLayout.SOUTH);

        panel.add(formContainer, BorderLayout.NORTH);

        questionTableModel = new DefaultTableModel(new String[]{"ID", "Type", "Part", "Question", "Marks", "Word Limit"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        questionTable = new JTable(questionTableModel);
        questionTable.setRowHeight(22);
        panel.add(new JScrollPane(questionTable), BorderLayout.CENTER);

        return panel;
    }

    private void saveExam() {
        String title = examTitleField.getText().trim();
        String durationStr = examDurationField.getText().trim();
        String dateStr = examDateField.getText().trim();
        String passMarksStr = examPassMarksField.getText().trim();

        if (title.isEmpty() || durationStr.isEmpty() || dateStr.isEmpty() || passMarksStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter all exam fields.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int duration = Integer.parseInt(durationStr);
            Date examDate = Date.valueOf(dateStr);
            double passMarks = Double.parseDouble(passMarksStr);

            String sql = "INSERT INTO exams (title, duration_minutes, exam_date, pass_marks, created_by_teacher_id, is_published) VALUES (?, ?, ?, ?, ?, FALSE)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                ps.setString(1, title);
                ps.setInt(2, duration);
                ps.setDate(3, examDate);
                ps.setDouble(4, passMarks);
                ps.setInt(5, currentUser.getId());
                ps.executeUpdate();

                // Auto-enroll all students currently assigned to this teacher into the newly created exam
                try (ResultSet genKeys = ps.getGeneratedKeys()) {
                    if (genKeys.next()) {
                        int newExamId = genKeys.getInt(1);
                        String autoEnrollSql = "INSERT INTO exam_students (exam_id, student_id) " +
                                "SELECT ?, student_id FROM teacher_students WHERE teacher_id = ?";
                        try (PreparedStatement psAuto = conn.prepareStatement(autoEnrollSql)) {
                            psAuto.setInt(1, newExamId);
                            psAuto.setInt(2, currentUser.getId());
                            psAuto.executeUpdate();
                        }
                    }
                }

                JOptionPane.showMessageDialog(this, "Exam created successfully and assigned to your student roster.");
                examTitleField.setText("");
                refreshExamTable();
                refreshExamSelectors();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to save exam: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveQuestion() {
        int examId = getComboSelectedId(examSelectorCombo);
        if (examId == -1) {
            JOptionPane.showMessageDialog(this, "No exam selected.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String qType = (String) questionTypeCombo.getSelectedItem();

        try (Connection conn = DBConnection.getConnection()) {
            if ("MCQ".equals(qType)) {
                String qText = mcqQuestionArea.getText().trim();
                String opA = optAField.getText().trim();
                String opB = optBField.getText().trim();
                String opC = optCField.getText().trim();
                String opD = optDField.getText().trim();
                String correct = (String) correctOptCombo.getSelectedItem();
                double marks = Double.parseDouble(mcqMarksField.getText().trim());

                if (qText.isEmpty() || opA.isEmpty() || opB.isEmpty() || opC.isEmpty() || opD.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "All MCQ options and question prompt are required.", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String sql = "INSERT INTO questions (exam_id, type, question_text, option_a, option_b, option_c, option_d, correct_option, marks) " +
                        "VALUES (?, 'MCQ', ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, examId);
                    ps.setString(2, qText);
                    ps.setString(3, opA);
                    ps.setString(4, opB);
                    ps.setString(5, opC);
                    ps.setString(6, opD);
                    ps.setString(7, correct);
                    ps.setDouble(8, marks);
                    ps.executeUpdate();
                }

                mcqQuestionArea.setText("");
                optAField.setText("");
                optBField.setText("");
                optCField.setText("");
                optDField.setText("");
            } else {
                String stem = cqStemArea.getText().trim();
                String subLabel = (String) cqSubPartCombo.getSelectedItem();
                if ("Standalone (No Part)".equals(subLabel)) subLabel = "";

                String qText = cqQuestionArea.getText().trim();
                String modelAns = cqModelAnswerArea.getText().trim();
                double marks = Double.parseDouble(cqMarksField.getText().trim());
                int wordLimit = Integer.parseInt(cqWordLimitField.getText().trim());

                if (qText.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Question Text is required.", "Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String sql = "INSERT INTO questions (exam_id, type, stem, sub_label, question_text, model_answer, marks, word_limit) " +
                        "VALUES (?, 'CQ', ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, examId);
                    ps.setString(2, stem.isEmpty() ? null : stem);
                    ps.setString(3, subLabel);
                    ps.setString(4, qText);
                    ps.setString(5, modelAns.isEmpty() ? null : modelAns);
                    ps.setDouble(6, marks);
                    ps.setInt(7, wordLimit);
                    ps.executeUpdate();
                }

                cqQuestionArea.setText("");
                cqModelAnswerArea.setText("");
            }

            JOptionPane.showMessageDialog(this, "Question saved successfully.");
            refreshQuestionTable();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error inserting question: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshExamTable() {
        examTableModel.setRowCount(0);
        String sql = "SELECT id, title, duration_minutes, exam_date, pass_marks, is_published FROM exams WHERE created_by_teacher_id = ? ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUser.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    examTableModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getInt("duration_minutes"),
                            rs.getDate("exam_date"),
                            rs.getDouble("pass_marks"),
                            rs.getBoolean("is_published") ? "Published" : "Draft / Unpublished"
                    });
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshExamSelectors() {
        examSelectorCombo.removeAllItems();
        enrollExamCombo.removeAllItems();

        String sql = "SELECT id, title FROM exams WHERE created_by_teacher_id = ? ORDER BY id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUser.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String item = rs.getInt("id") + " - " + rs.getString("title");
                    examSelectorCombo.addItem(item);
                    enrollExamCombo.addItem(item);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        refreshQuestionTable();
        refreshEnrollStudentTable();
    }

    private int getComboSelectedId(JComboBox<String> combo) {
        String item = (String) combo.getSelectedItem();
        if (item == null) return -1;
        return Integer.parseInt(item.split(" - ")[0]);
    }

    private void refreshQuestionTable() {
        questionTableModel.setRowCount(0);
        int examId = getComboSelectedId(examSelectorCombo);
        if (examId == -1) return;

        String sql = "SELECT id, type, sub_label, question_text, marks, word_limit FROM questions WHERE exam_id = ? ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    questionTableModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("type"),
                            rs.getString("sub_label") != null ? rs.getString("sub_label") : "-",
                            rs.getString("question_text"),
                            rs.getDouble("marks"),
                            rs.getInt("word_limit")
                    });
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void openGradingForSelectedExam() {
        int selectedRow = examTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an exam from the table first.", "Selection Needed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int examId = (int) examTableModel.getValueAt(selectedRow, 0);
        new GradingFrame(examId, currentUser).setVisible(true);
    }
}