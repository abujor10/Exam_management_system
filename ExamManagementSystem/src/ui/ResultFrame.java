package ui;

import db.DBConnection;
import model.User;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ResultFrame extends JFrame {
    private int examId;
    private User currentUser;

    private JLabel scoreSummaryLabel;
    private JLabel teacherFeedbackLabel;
    private JLabel analyticsLabel;
    private JTable leaderboardTable;
    private DefaultTableModel leaderboardModel;

    // INPUT: int examId, User currentUser -> ACTION: Builds candidate score breakdown & pass/fail leaderboard -> OUTPUT: None
    public ResultFrame(int examId, User currentUser) {
        this.examId = examId;
        this.currentUser = currentUser;

        setTitle("Exam Analytics & Leaderboard - Exam #" + examId);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel personalCard = new JPanel(new GridLayout(2, 1, 5, 5));
        personalCard.setBorder(BorderFactory.createTitledBorder("Candidate Performance Breakdown"));

        scoreSummaryLabel = new JLabel("Loading your results...", SwingConstants.LEFT);
        scoreSummaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));

        teacherFeedbackLabel = new JLabel("Teacher Remarks: None", SwingConstants.LEFT);
        teacherFeedbackLabel.setFont(new Font("Segoe UI", Font.ITALIC, 13));

        personalCard.add(scoreSummaryLabel);
        personalCard.add(teacherFeedbackLabel);
        root.add(personalCard, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Cohort Leaderboard with Pass/Fail Results"));

        leaderboardModel = new DefaultTableModel(new String[]{"Rank", "Student Name", "Roll Number", "MCQ Score", "CQ Score", "Total Score", "Status (Pass/Fail)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        leaderboardTable = new JTable(leaderboardModel);
        leaderboardTable.setRowHeight(24);
        leaderboardTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        centerPanel.add(new JScrollPane(leaderboardTable), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new BorderLayout());
        analyticsLabel = new JLabel("Class Statistics: Calculating...", SwingConstants.CENTER);
        analyticsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        bottomBar.add(analyticsLabel, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close Window");
        closeBtn.addActionListener(e -> dispose());
        bottomBar.add(closeBtn, BorderLayout.EAST);

        root.add(centerPanel, BorderLayout.CENTER);
        root.add(bottomBar, BorderLayout.SOUTH);

        add(root);
    }

    private void loadData() {
        try (Connection conn = DBConnection.getConnection()) {
            double passMarks = 40.0;
            String checkPubSql = "SELECT title, pass_marks, is_published FROM exams WHERE id = ?";
            try (PreparedStatement ps = conn.prepareStatement(checkPubSql)) {
                ps.setInt(1, examId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        passMarks = rs.getDouble("pass_marks");
                        boolean isPub = rs.getBoolean("is_published");
                        if (!isPub && !"TEACHER".equals(currentUser.getRole()) && !"ADMIN".equals(currentUser.getRole())) {
                            scoreSummaryLabel.setText("Results for '" + rs.getString("title") + "' have not been published by the instructor yet.");
                            teacherFeedbackLabel.setText("Please check back later.");
                            analyticsLabel.setText("Cohorts results locked.");
                            return;
                        }
                    }
                }
            }

            // Student's personal score
            String scoreSql = "SELECT mcq_score, cq_score, total_score, teacher_feedback, status FROM submissions WHERE exam_id = ? AND student_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(scoreSql)) {
                ps.setInt(1, examId);
                ps.setInt(2, currentUser.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        double mcq = rs.getDouble("mcq_score");
                        double cq = rs.getDouble("cq_score");
                        double tot = rs.getDouble("total_score");
                        String outcome = (tot >= passMarks) ? "PASSED" : "FAILED";
                        String feedback = rs.getString("teacher_feedback");

                        scoreSummaryLabel.setText("Outcome: " + outcome + " (Pass Mark: " + passMarks + ") | MCQ: " + mcq + " | CQ: " + cq + " | Total: " + tot);
                        teacherFeedbackLabel.setText("Teacher Remarks: " + (feedback != null && !feedback.isEmpty() ? feedback : "No feedback attached."));
                    } else {
                        scoreSummaryLabel.setText("No submission record found for current student.");
                    }
                }
            }

            // Cohort Leaderboard & Pass/Fail Counts
            leaderboardModel.setRowCount(0);
            String lbSql = "SELECT u.full_name, u.roll_number, s.mcq_score, s.cq_score, s.total_score " +
                    "FROM submissions s " +
                    "JOIN users u ON s.student_id = u.id " +
                    "WHERE s.exam_id = ? AND s.status IN ('SUBMITTED', 'GRADED') " +
                    "ORDER BY s.total_score DESC";

            int rank = 1;
            double highest = Double.MIN_VALUE;
            double lowest = Double.MAX_VALUE;
            double sumTotal = 0;
            int count = 0;
            int passCount = 0;
            int failCount = 0;

            try (PreparedStatement ps = conn.prepareStatement(lbSql)) {
                ps.setInt(1, examId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double tot = rs.getDouble("total_score");
                        String resultStatus = (tot >= passMarks) ? "PASS" : "FAIL";
                        if (tot >= passMarks) passCount++; else failCount++;

                        leaderboardModel.addRow(new Object[]{
                                rank++,
                                rs.getString("full_name"),
                                rs.getString("roll_number"),
                                rs.getDouble("mcq_score"),
                                rs.getDouble("cq_score"),
                                tot,
                                resultStatus
                        });

                        if (tot > highest) highest = tot;
                        if (tot < lowest) lowest = tot;
                        sumTotal += tot;
                        count++;
                    }
                }
            }

            if (count > 0) {
                double avg = sumTotal / count;
                analyticsLabel.setText(String.format("Candidates: %d | Passed: %d | Failed: %d | Average: %.2f | Highest: %.2f | Lowest: %.2f", count, passCount, failCount, avg, highest, lowest));
            } else {
                analyticsLabel.setText("No candidate results available for calculation.");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}