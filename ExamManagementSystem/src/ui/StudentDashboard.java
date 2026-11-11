package ui;

import db.DBConnection;
import model.Exam;
import model.User;
import util.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class StudentDashboard extends JFrame {
    private User currentUser;
    private JTable examTable;
    private DefaultTableModel examTableModel;

    // INPUT: User user -> ACTION: Sets up student view listing strictly assigned and published exams -> OUTPUT: None
    public StudentDashboard(User user) {
        this.currentUser = user;
        setTitle("Student Portal - " + user.getFullName() + " (" + user.getRollNumber() + ")");
        setSize(950, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Candidate Workspace: " + user.getFullName() + " | Roll: " + user.getRollNumber());
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));

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

        examTableModel = new DefaultTableModel(new String[]{"Exam ID", "Exam Title", "Duration", "Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        examTable = new JTable(examTableModel);
        examTable.setRowHeight(25);
        mainPanel.add(new JScrollPane(examTable), BorderLayout.CENTER);

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton takeExamBtn = new JButton("Take / Resume Exam");
        takeExamBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        takeExamBtn.setBackground(new Color(24, 119, 242));
        takeExamBtn.setForeground(Color.WHITE);
        takeExamBtn.setFocusPainted(false);
        takeExamBtn.addActionListener(e -> handleTakeExam());

        JButton viewResultBtn = new JButton("View Exam Result / Leaderboard");
        viewResultBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        viewResultBtn.setFocusPainted(false);
        viewResultBtn.addActionListener(e -> handleViewResult());

        bottomBar.add(takeExamBtn);
        bottomBar.add(viewResultBtn);
        mainPanel.add(bottomBar, BorderLayout.SOUTH);

        add(mainPanel);
        refreshExams();
    }

    // INPUT: None -> ACTION: Queries exams ONLY enrolled to this student via exam_students -> OUTPUT: None
    private void refreshExams() {
        examTableModel.setRowCount(0);

        String sql = "SELECT e.id, e.title, e.duration_minutes, e.exam_date, s.status " +
                "FROM exams e " +
                "JOIN exam_students es ON e.id = es.exam_id " +
                "LEFT JOIN submissions s ON e.id = s.exam_id AND s.student_id = ? " +
                "WHERE es.student_id = ? " +
                "ORDER BY e.id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUser.getId());
            ps.setInt(2, currentUser.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = rs.getString("status");
                    if (status == null) status = "NOT_STARTED";

                    examTableModel.addRow(new Object[]{
                            rs.getInt("id"),
                            rs.getString("title"),
                            rs.getInt("duration_minutes") + " Mins",
                            rs.getDate("exam_date"),
                            status
                    });
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // INPUT: None -> ACTION: Performs server-side validation against exam_students and launches ExamFrame -> OUTPUT: None
    private void handleTakeExam() {
        int selectedRow = examTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an exam from the list.", "Selection Needed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int examId = (int) examTableModel.getValueAt(selectedRow, 0);
        String status = (String) examTableModel.getValueAt(selectedRow, 4);

        if ("SUBMITTED".equals(status) || "GRADED".equals(status)) {
            JOptionPane.showMessageDialog(this, "You have already completed and submitted this exam.", "Access Locked", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            // Strict server-side validation: verify candidate enrollment in exam_students
            String verifySql = "SELECT COUNT(*) FROM exam_students WHERE exam_id = ? AND student_id = ?";
            try (PreparedStatement psVerify = conn.prepareStatement(verifySql)) {
                psVerify.setInt(1, examId);
                psVerify.setInt(2, currentUser.getId());
                try (ResultSet rsVerify = psVerify.executeQuery()) {
                    if (!rsVerify.next() || rsVerify.getInt(1) == 0) {
                        JOptionPane.showMessageDialog(this, "Access Denied: You are not enrolled in this exam.", "Security Exception", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            // Load Exam metadata
            String examSql = "SELECT * FROM exams WHERE id = ?";
            try (PreparedStatement psExam = conn.prepareStatement(examSql)) {
                psExam.setInt(1, examId);
                try (ResultSet rs = psExam.executeQuery()) {
                    if (rs.next()) {
                        Exam exam = new Exam(
                                rs.getInt("id"),
                                rs.getString("title"),
                                rs.getInt("duration_minutes"),
                                rs.getDate("exam_date"),
                                rs.getInt("created_by_teacher_id"),
                                rs.getBoolean("is_published")
                        );
                        new ExamFrame(exam, currentUser).setVisible(true);
                        dispose();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleViewResult() {
        int selectedRow = examTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Select an exam first.", "Selection Needed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int examId = (int) examTableModel.getValueAt(selectedRow, 0);
        new ResultFrame(examId, currentUser).setVisible(true);
    }
}