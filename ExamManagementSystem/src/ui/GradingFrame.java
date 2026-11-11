package ui;

import db.DBConnection;
import model.User;

import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class GradingFrame extends JFrame {
    private int examId;
    private User teacher;

    private JComboBox<String> studentSelectorCombo;
    private List<Integer> submissionIds = new ArrayList<>();
    private int currentSubmissionId = -1;

    private List<Integer> cqQuestionIds = new ArrayList<>();
    private List<String> cqSubLabels = new ArrayList<>();
    private List<String> cqPrompts = new ArrayList<>();
    private List<String> cqModelAnswers = new ArrayList<>();
    private List<Double> cqMaxMarks = new ArrayList<>();
    private int currentCqIndex = 0;

    private JLabel studentInfoLabel;
    private JLabel focusLossIndicator;
    private JLabel mcqScoreLabel;
    private JLabel cqTrackerLabel;
    private JLabel maxMarksPromptLabel; // Explicitly tells teacher the maximum marks
    private JTextArea studentAnswerArea;
    private JTextArea modelAnswerArea;
    private JTextField marksAwardedField;
    private JTextArea teacherFeedbackArea;

    // INPUT: int examId, User teacher -> ACTION: Sets up CQ grading interface -> OUTPUT: None
    public GradingFrame(int examId, User teacher) {
        this.examId = examId;
        this.teacher = teacher;

        setTitle("Teacher Evaluation Suite - Exam ID: " + examId);
        setSize(1100, 750);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        loadSubmissions();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Top Candidate Selection & Anti-Cheat Monitor Alert
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        topBar.setBorder(BorderFactory.createTitledBorder("Candidate Evaluation Details"));
        topBar.add(new JLabel("Candidate:"));
        studentSelectorCombo = new JComboBox<>();
        studentSelectorCombo.setPreferredSize(new Dimension(280, 30));
        studentSelectorCombo.addActionListener(e -> onStudentSelected());
        topBar.add(studentSelectorCombo);

        studentInfoLabel = new JLabel("Roll: N/A");
        studentInfoLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        topBar.add(studentInfoLabel);

        focusLossIndicator = new JLabel("Focus Losses: 0");
        focusLossIndicator.setFont(new Font("Segoe UI", Font.BOLD, 13));
        focusLossIndicator.setForeground(new Color(220, 53, 69));
        topBar.add(focusLossIndicator);

        mcqScoreLabel = new JLabel("MCQ Marks: 0.0");
        mcqScoreLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        mcqScoreLabel.setForeground(new Color(40, 167, 69));
        topBar.add(mcqScoreLabel);

        root.add(topBar, BorderLayout.NORTH);

        // Center Grading Box
        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));

        JPanel navHeader = new JPanel(new BorderLayout());
        cqTrackerLabel = new JLabel("Creative Question Evaluation");
        cqTrackerLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        navHeader.add(cqTrackerLabel, BorderLayout.WEST);

        JPanel cqNavButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        JButton prevCqBtn = new JButton("◄ Previous Sub-Question");
        JButton nextCqBtn = new JButton("Next Sub-Question ►");
        prevCqBtn.addActionListener(e -> navigateCq(-1));
        nextCqBtn.addActionListener(e -> navigateCq(1));
        cqNavButtons.add(prevCqBtn);
        cqNavButtons.add(nextCqBtn);
        navHeader.add(cqNavButtons, BorderLayout.EAST);
        centerPanel.add(navHeader, BorderLayout.NORTH);

        // Comparison View
        JPanel comparePanel = new JPanel(new GridLayout(1, 2, 10, 10));

        JPanel studentBox = new JPanel(new BorderLayout(5, 5));
        studentBox.setBorder(BorderFactory.createTitledBorder("Student Submitted Answer"));
        studentAnswerArea = new JTextArea();
        studentAnswerArea.setEditable(false);
        studentAnswerArea.setLineWrap(true);
        studentAnswerArea.setWrapStyleWord(true);
        studentAnswerArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        studentBox.add(new JScrollPane(studentAnswerArea), BorderLayout.CENTER);

        JPanel modelBox = new JPanel(new BorderLayout(5, 5));
        modelBox.setBorder(BorderFactory.createTitledBorder("Instructor Reference / Model Answer"));
        modelAnswerArea = new JTextArea();
        modelAnswerArea.setEditable(false);
        modelAnswerArea.setLineWrap(true);
        modelAnswerArea.setWrapStyleWord(true);
        modelAnswerArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        modelBox.add(new JScrollPane(modelAnswerArea), BorderLayout.CENTER);

        comparePanel.add(studentBox);
        comparePanel.add(modelBox);
        centerPanel.add(comparePanel, BorderLayout.CENTER);

        // Marking Controls with Clear Max Marks Display
        JPanel gradeInputPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        gradeInputPanel.setBorder(BorderFactory.createTitledBorder("Assign Marks"));

        maxMarksPromptLabel = new JLabel("Enter Awarded Marks (Maximum: 0.0):");
        maxMarksPromptLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        marksAwardedField = new JTextField();
        marksAwardedField.setFont(new Font("Segoe UI", Font.BOLD, 15));

        gradeInputPanel.add(maxMarksPromptLabel);
        gradeInputPanel.add(marksAwardedField);
        gradeInputPanel.add(new JLabel("Instructor Feedback for this Candidate:"));
        teacherFeedbackArea = new JTextArea(2, 20);
        teacherFeedbackArea.setLineWrap(true);
        gradeInputPanel.add(new JScrollPane(teacherFeedbackArea));

        centerPanel.add(gradeInputPanel, BorderLayout.SOUTH);
        root.add(centerPanel, BorderLayout.CENTER);

        // Bottom Action Bar
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));

        JButton saveMarksBtn = new JButton("Save Mark for this CQ Part");
        saveMarksBtn.setBackground(new Color(0, 123, 255));
        saveMarksBtn.setForeground(Color.WHITE);
        saveMarksBtn.setFocusPainted(false);
        saveMarksBtn.addActionListener(e -> saveCurrentCqGrade());

        JButton finalizeStudentBtn = new JButton("Calculate Total & Finalize Candidate");
        finalizeStudentBtn.setBackground(new Color(40, 167, 69));
        finalizeStudentBtn.setForeground(Color.WHITE);
        finalizeStudentBtn.setFocusPainted(false);
        finalizeStudentBtn.addActionListener(e -> finalizeStudentGrade());

        JButton publishResultsBtn = new JButton("Publish All Exam Results");
        publishResultsBtn.setBackground(new Color(108, 117, 125));
        publishResultsBtn.setForeground(Color.WHITE);
        publishResultsBtn.setFocusPainted(false);
        publishResultsBtn.addActionListener(e -> publishExamResults());

        bottomBar.add(saveMarksBtn);
        bottomBar.add(finalizeStudentBtn);
        bottomBar.add(publishResultsBtn);
        root.add(bottomBar, BorderLayout.SOUTH);

        add(root);
    }

    private void loadSubmissions() {
        studentSelectorCombo.removeAllItems();
        submissionIds.clear();

        String sql = "SELECT s.id, u.full_name, u.roll_number " +
                "FROM submissions s " +
                "JOIN users u ON s.student_id = u.id " +
                "WHERE s.exam_id = ? AND s.status IN ('SUBMITTED', 'GRADED')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    submissionIds.add(rs.getInt("id"));
                    studentSelectorCombo.addItem(rs.getString("full_name") + " (" + rs.getString("roll_number") + ")");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (studentSelectorCombo.getItemCount() == 0) {
            studentSelectorCombo.addItem("No submissions to grade");
        }
    }

    private void onStudentSelected() {
        int idx = studentSelectorCombo.getSelectedIndex();
        if (idx < 0 || idx >= submissionIds.size()) return;

        currentSubmissionId = submissionIds.get(idx);

        String sql = "SELECT s.focus_loss_count, s.mcq_score, s.teacher_feedback, u.roll_number " +
                "FROM submissions s " +
                "JOIN users u ON s.student_id = u.id " +
                "WHERE s.id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentSubmissionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int focusCount = rs.getInt("focus_loss_count");
                    studentInfoLabel.setText("Roll: " + rs.getString("roll_number"));
                    focusLossIndicator.setText("Focus Losses: " + focusCount + (focusCount > 2 ? " [FLAGGED SUSPICIOUS]" : ""));
                    mcqScoreLabel.setText("MCQ Marks: " + rs.getDouble("mcq_score"));
                    teacherFeedbackArea.setText(rs.getString("teacher_feedback") != null ? rs.getString("teacher_feedback") : "");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        loadCqQuestions();
    }

    private void loadCqQuestions() {
        cqQuestionIds.clear();
        cqSubLabels.clear();
        cqPrompts.clear();
        cqModelAnswers.clear();
        cqMaxMarks.clear();

        String sql = "SELECT id, sub_label, question_text, model_answer, marks FROM questions WHERE exam_id = ? AND type = 'CQ' ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cqQuestionIds.add(rs.getInt("id"));
                    cqSubLabels.add(rs.getString("sub_label") != null ? rs.getString("sub_label") : "");
                    cqPrompts.add(rs.getString("question_text"));
                    cqModelAnswers.add(rs.getString("model_answer"));
                    cqMaxMarks.add(rs.getDouble("marks"));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        currentCqIndex = 0;
        displayActiveCq();
    }

    private void displayActiveCq() {
        if (cqQuestionIds.isEmpty()) {
            cqTrackerLabel.setText("No CQ Questions found for this assessment.");
            studentAnswerArea.setText("");
            modelAnswerArea.setText("");
            marksAwardedField.setText("0.0");
            maxMarksPromptLabel.setText("Enter Awarded Marks (Maximum: 0.0):");
            return;
        }

        int qId = cqQuestionIds.get(currentCqIndex);
        double maxMark = cqMaxMarks.get(currentCqIndex);
        String subLabel = cqSubLabels.get(currentCqIndex);
        String labelPart = subLabel.isEmpty() ? "CQ " + (currentCqIndex + 1) : subLabel;

        cqTrackerLabel.setText("Evaluating: " + labelPart + " | Prompt: " + cqPrompts.get(currentCqIndex));
        maxMarksPromptLabel.setText("Enter Awarded Marks for " + labelPart + " (Maximum Allowed: " + maxMark + "):");
        modelAnswerArea.setText(cqModelAnswers.get(currentCqIndex) != null ? cqModelAnswers.get(currentCqIndex) : "No model answer provided by teacher.");

        String sql = "SELECT answer_text, awarded_marks FROM student_answers WHERE submission_id = ? AND question_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentSubmissionId);
            ps.setInt(2, qId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    studentAnswerArea.setText(rs.getString("answer_text"));
                    marksAwardedField.setText(String.valueOf(rs.getDouble("awarded_marks")));
                } else {
                    studentAnswerArea.setText("[Candidate left this question blank]");
                    marksAwardedField.setText("0.0");
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void navigateCq(int delta) {
        int next = currentCqIndex + delta;
        if (next >= 0 && next < cqQuestionIds.size()) {
            saveCurrentCqGrade();
            currentCqIndex = next;
            displayActiveCq();
        }
    }

    private void saveCurrentCqGrade() {
        if (cqQuestionIds.isEmpty() || currentSubmissionId == -1) return;

        int qId = cqQuestionIds.get(currentCqIndex);
        double maxMark = cqMaxMarks.get(currentCqIndex);
        try {
            double awarded = Double.parseDouble(marksAwardedField.getText().trim());
            if (awarded < 0 || awarded > maxMark) {
                JOptionPane.showMessageDialog(this, "Awarded marks cannot exceed max marks of " + maxMark, "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String sql = "UPDATE student_answers SET awarded_marks = ? WHERE submission_id = ? AND question_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, awarded);
                ps.setInt(2, currentSubmissionId);
                ps.setInt(3, qId);
                ps.executeUpdate();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Please enter a valid numeric value.", "Invalid Input", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void finalizeStudentGrade() {
        if (currentSubmissionId == -1) return;
        saveCurrentCqGrade();

        try (Connection conn = DBConnection.getConnection()) {
            double cqSum = 0.0;
            String cqSumSql = "SELECT SUM(sa.awarded_marks) AS cq_total " +
                    "FROM student_answers sa " +
                    "JOIN questions q ON sa.question_id = q.id " +
                    "WHERE sa.submission_id = ? AND q.type = 'CQ'";
            try (PreparedStatement ps = conn.prepareStatement(cqSumSql)) {
                ps.setInt(1, currentSubmissionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) cqSum = rs.getDouble("cq_total");
                }
            }

            double mcqScore = 0.0;
            String mcqSql = "SELECT mcq_score FROM submissions WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(mcqSql)) {
                ps.setInt(1, currentSubmissionId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) mcqScore = rs.getDouble("mcq_score");
                }
            }

            double total = mcqScore + cqSum;
            String feedback = teacherFeedbackArea.getText().trim();

            String updateSubSql = "UPDATE submissions SET cq_score = ?, total_score = ?, status = 'GRADED', teacher_feedback = ? WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(updateSubSql)) {
                ps.setDouble(1, cqSum);
                ps.setDouble(2, total);
                ps.setString(3, feedback);
                ps.setInt(4, currentSubmissionId);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Finalized Successfully!\nMCQ: " + mcqScore + " + CQ: " + cqSum + " = Total: " + total, "Grading Completed", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void publishExamResults() {
        int confirm = JOptionPane.showConfirmDialog(this, "Publish results for this exam? Students will now see their total marks, pass/fail status, and leaderboard.", "Confirm Publication", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "UPDATE exams SET is_published = TRUE WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Results are now LIVE to all candidates.", "Published", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}