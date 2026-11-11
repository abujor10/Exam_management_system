package ui;

import db.DBConnection;
import model.User;
import util.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdminDashboard extends JFrame {
    private User currentUser;

    // Student Tab Components
    private JTable studentTable;
    private DefaultTableModel studentTableModel;
    private JTextField studentIdField;
    private JTextField studentUsernameField;
    private JTextField studentPasswordField;
    private JTextField studentFullNameField;
    private JTextField studentRollField;

    // Teacher Tab Components
    private JTable teacherTable;
    private DefaultTableModel teacherTableModel;
    private JTextField teacherIdField;
    private JTextField teacherUsernameField;
    private JTextField teacherPasswordField;
    private JTextField teacherFullNameField;

    // Teacher-Student Assignment Tab Components
    private JComboBox<String> assignTeacherCombo;
    private List<Integer> assignTeacherIds = new ArrayList<>();
    private JTable assignStudentTable;
    private DefaultTableModel assignStudentTableModel;

    // Invigilator Live Anti-Cheat Monitor Tab
    private JTable monitorTable;
    private DefaultTableModel monitorTableModel;
    private Timer liveRefreshTimer;

    // INPUT: User user -> ACTION: Sets up admin dashboard interface with complete CRUD, assignment, and anti-cheat monitor -> OUTPUT: None
    public AdminDashboard(User user) {
        this.currentUser = user;
        setTitle("Admin Console & Invigilator Suite - " + user.getFullName());
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("System Administrator & Exam Invigilator Console");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel headerControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton themeBtn = new JButton("Toggle Theme");
        themeBtn.setFocusPainted(false);
        themeBtn.addActionListener(e -> ThemeManager.toggleTheme(this));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> {
            if (liveRefreshTimer != null) liveRefreshTimer.stop();
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
        tabbedPane.addTab("Manage Students", createStudentPanel());
        tabbedPane.addTab("Manage Teachers", createTeacherPanel());
        tabbedPane.addTab("Assign Students to Teachers", createAssignmentPanel());
        tabbedPane.addTab("LIVE Anti-Cheat Invigilator Monitor", createInvigilatorPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel);

        refreshStudentTable();
        refreshTeacherTable();
        refreshAssignTeacherCombo();
        refreshMonitorTable();

        liveRefreshTimer = new Timer(4000, e -> refreshMonitorTable());
        liveRefreshTimer.start();
    }

    // INPUT: None -> ACTION: Constructs UI panel for assigning students to specific teachers -> OUTPUT: JPanel
    private JPanel createAssignmentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel topSelection = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topSelection.add(new JLabel("Select Instructor:"));
        assignTeacherCombo = new JComboBox<>();
        assignTeacherCombo.setPreferredSize(new Dimension(300, 30));
        assignTeacherCombo.addActionListener(e -> refreshAssignStudentTable());
        topSelection.add(assignTeacherCombo);

        JButton reloadBtn = new JButton("Reload Students");
        reloadBtn.addActionListener(e -> refreshAssignStudentTable());
        topSelection.add(reloadBtn);

        panel.add(topSelection, BorderLayout.NORTH);

        assignStudentTableModel = new DefaultTableModel(new String[]{"Assigned", "Student ID", "Full Name", "Roll Number"}, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0;
            }
        };
        assignStudentTable = new JTable(assignStudentTableModel);
        assignStudentTable.setRowHeight(26);
        panel.add(new JScrollPane(assignStudentTable), BorderLayout.CENTER);

        JPanel bottomActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton saveAssignmentsBtn = new JButton("Save Teacher Assignments");
        saveAssignmentsBtn.setBackground(new Color(24, 119, 242));
        saveAssignmentsBtn.setForeground(Color.WHITE);
        saveAssignmentsBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveAssignmentsBtn.setFocusPainted(false);
        saveAssignmentsBtn.addActionListener(e -> saveTeacherAssignments());
        bottomActions.add(saveAssignmentsBtn);

        panel.add(bottomActions, BorderLayout.SOUTH);
        return panel;
    }

    // INPUT: None -> ACTION: Populates teacher dropdown in assignment tab -> OUTPUT: None
    private void refreshAssignTeacherCombo() {
        assignTeacherCombo.removeAllItems();
        assignTeacherIds.clear();

        String sql = "SELECT id, full_name, username FROM users WHERE role = 'TEACHER' ORDER BY full_name ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                assignTeacherIds.add(rs.getInt("id"));
                assignTeacherCombo.addItem(rs.getString("full_name") + " (" + rs.getString("username") + ")");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (assignTeacherCombo.getItemCount() > 0) {
            assignTeacherCombo.setSelectedIndex(0);
        }
    }

    // INPUT: None -> ACTION: Reads all students and checks those currently assigned to selected teacher -> OUTPUT: None
    private void refreshAssignStudentTable() {
        assignStudentTableModel.setRowCount(0);
        int idx = assignTeacherCombo.getSelectedIndex();
        if (idx < 0 || idx >= assignTeacherIds.size()) return;

        int teacherId = assignTeacherIds.get(idx);

        String sql = "SELECT u.id, u.full_name, u.roll_number, " +
                "(SELECT COUNT(*) FROM teacher_students ts WHERE ts.teacher_id = ? AND ts.student_id = u.id) AS is_assigned " +
                "FROM users u WHERE u.role = 'STUDENT' ORDER BY u.roll_number ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, teacherId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    boolean assigned = rs.getInt("is_assigned") > 0;
                    assignStudentTableModel.addRow(new Object[]{
                            assigned,
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

    // INPUT: None -> ACTION: Persists checked student rows into teacher_students table -> OUTPUT: None
    private void saveTeacherAssignments() {
        int idx = assignTeacherCombo.getSelectedIndex();
        if (idx < 0 || idx >= assignTeacherIds.size()) {
            JOptionPane.showMessageDialog(this, "Select a teacher first.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int teacherId = assignTeacherIds.get(idx);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            String deleteSql = "DELETE FROM teacher_students WHERE teacher_id = ?";
            try (PreparedStatement psDel = conn.prepareStatement(deleteSql)) {
                psDel.setInt(1, teacherId);
                psDel.executeUpdate();
            }

            String insertSql = "INSERT INTO teacher_students (teacher_id, student_id) VALUES (?, ?)";
            try (PreparedStatement psIns = conn.prepareStatement(insertSql)) {
                for (int i = 0; i < assignStudentTableModel.getRowCount(); i++) {
                    Boolean isChecked = (Boolean) assignStudentTableModel.getValueAt(i, 0);
                    if (Boolean.TRUE.equals(isChecked)) {
                        int studentId = (Integer) assignStudentTableModel.getValueAt(i, 1);
                        psIns.setInt(1, teacherId);
                        psIns.setInt(2, studentId);
                        psIns.addBatch();
                    }
                }
                psIns.executeBatch();
            }

            conn.commit();
            conn.setAutoCommit(true);
            JOptionPane.showMessageDialog(this, "Teacher student roster updated successfully.", "Saved", JOptionPane.INFORMATION_MESSAGE);
            refreshAssignStudentTable();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save assignments: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // INPUT: None -> ACTION: Constructs live invigilator monitor tab -> OUTPUT: JPanel
    private JPanel createInvigilatorPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel info = new JLabel("Live Candidate Activity Monitor (Auto-refreshes every 4 seconds. Highlights focus switches / window changes):");
        info.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        panel.add(info, BorderLayout.NORTH);

        monitorTableModel = new DefaultTableModel(new String[]{"Exam Title", "Candidate Name", "Roll Number", "Status", "Focus Losses (Window Switches)", "Cheating Alert Flag"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        monitorTable = new JTable(monitorTableModel);
        monitorTable.setRowHeight(25);
        monitorTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        panel.add(new JScrollPane(monitorTable), BorderLayout.CENTER);

        JButton forceRefreshBtn = new JButton("Refresh Monitor Now");
        forceRefreshBtn.addActionListener(e -> refreshMonitorTable());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(forceRefreshBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // INPUT: None -> ACTION: Queries DB and refreshes anti-cheat monitoring table -> OUTPUT: None
    private void refreshMonitorTable() {
        monitorTableModel.setRowCount(0);
        String sql = "SELECT e.title, u.full_name, u.roll_number, s.status, s.focus_loss_count " +
                "FROM submissions s " +
                "JOIN users u ON s.student_id = u.id " +
                "JOIN exams e ON s.exam_id = e.id " +
                "ORDER BY s.focus_loss_count DESC, s.id DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int focusLosses = rs.getInt("focus_loss_count");
                String alert = "CLEAN";
                if (focusLosses >= 3) {
                    alert = "HIGH RISK (Window Switch Detected!)";
                } else if (focusLosses > 0) {
                    alert = "SUSPICIOUS";
                }

                monitorTableModel.addRow(new Object[]{
                        rs.getString("title"),
                        rs.getString("full_name"),
                        rs.getString("roll_number"),
                        rs.getString("status"),
                        focusLosses,
                        alert
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // INPUT: None -> ACTION: Constructs UI layout for student CRUD management -> OUTPUT: JPanel
    private JPanel createStudentPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Student Details"));

        studentIdField = new JTextField();
        studentIdField.setEditable(false);
        studentUsernameField = new JTextField();
        studentPasswordField = new JTextField();
        studentFullNameField = new JTextField();
        studentRollField = new JTextField();

        formPanel.add(new JLabel("Internal ID:"));
        formPanel.add(studentIdField);
        formPanel.add(new JLabel("Username:"));
        formPanel.add(studentUsernameField);
        formPanel.add(new JLabel("Password:"));
        formPanel.add(studentPasswordField);
        formPanel.add(new JLabel("Full Name:"));
        formPanel.add(studentFullNameField);
        formPanel.add(new JLabel("Roll Number:"));
        formPanel.add(studentRollField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton addBtn = new JButton("Add Student");
        JButton updateBtn = new JButton("Update Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        JButton clearBtn = new JButton("Clear Form");

        addBtn.addActionListener(e -> addStudent());
        updateBtn.addActionListener(e -> updateStudent());
        deleteBtn.addActionListener(e -> deleteStudent());
        clearBtn.addActionListener(e -> clearStudentForm());

        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(clearBtn);

        JPanel northContainer = new JPanel(new BorderLayout(5, 5));
        northContainer.add(formPanel, BorderLayout.CENTER);
        northContainer.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(northContainer, BorderLayout.NORTH);

        studentTableModel = new DefaultTableModel(new String[]{"ID", "Username", "Password", "Full Name", "Roll Number"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        studentTable = new JTable(studentTableModel);
        studentTable.setRowHeight(24);
        studentTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = studentTable.getSelectedRow();
                if (row != -1) {
                    studentIdField.setText(studentTableModel.getValueAt(row, 0).toString());
                    studentUsernameField.setText(studentTableModel.getValueAt(row, 1).toString());
                    studentPasswordField.setText(studentTableModel.getValueAt(row, 2).toString());
                    studentFullNameField.setText(studentTableModel.getValueAt(row, 3).toString());
                    studentRollField.setText(studentTableModel.getValueAt(row, 4) != null ? studentTableModel.getValueAt(row, 4).toString() : "");
                }
            }
        });

        panel.add(new JScrollPane(studentTable), BorderLayout.CENTER);
        return panel;
    }

    // INPUT: None -> ACTION: Constructs UI layout for teacher CRUD management -> OUTPUT: JPanel
    private JPanel createTeacherPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));

        JPanel formPanel = new JPanel(new GridLayout(4, 2, 8, 8));
        formPanel.setBorder(BorderFactory.createTitledBorder("Teacher Details"));

        teacherIdField = new JTextField();
        teacherIdField.setEditable(false);
        teacherUsernameField = new JTextField();
        teacherPasswordField = new JTextField();
        teacherFullNameField = new JTextField();

        formPanel.add(new JLabel("Internal ID:"));
        formPanel.add(teacherIdField);
        formPanel.add(new JLabel("Username:"));
        formPanel.add(teacherUsernameField);
        formPanel.add(new JLabel("Password:"));
        formPanel.add(teacherPasswordField);
        formPanel.add(new JLabel("Full Name:"));
        formPanel.add(teacherFullNameField);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton addBtn = new JButton("Add Teacher");
        JButton updateBtn = new JButton("Update Selected");
        JButton deleteBtn = new JButton("Delete Selected");
        JButton clearBtn = new JButton("Clear Form");

        addBtn.addActionListener(e -> addTeacher());
        updateBtn.addActionListener(e -> updateTeacher());
        deleteBtn.addActionListener(e -> deleteTeacher());
        clearBtn.addActionListener(e -> clearTeacherForm());

        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(clearBtn);

        JPanel northContainer = new JPanel(new BorderLayout(5, 5));
        northContainer.add(formPanel, BorderLayout.CENTER);
        northContainer.add(buttonPanel, BorderLayout.SOUTH);
        panel.add(northContainer, BorderLayout.NORTH);

        teacherTableModel = new DefaultTableModel(new String[]{"ID", "Username", "Password", "Full Name"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        teacherTable = new JTable(teacherTableModel);
        teacherTable.setRowHeight(24);
        teacherTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = teacherTable.getSelectedRow();
                if (row != -1) {
                    teacherIdField.setText(teacherTableModel.getValueAt(row, 0).toString());
                    teacherUsernameField.setText(teacherTableModel.getValueAt(row, 1).toString());
                    teacherPasswordField.setText(teacherTableModel.getValueAt(row, 2).toString());
                    teacherFullNameField.setText(teacherTableModel.getValueAt(row, 3).toString());
                }
            }
        });

        panel.add(new JScrollPane(teacherTable), BorderLayout.CENTER);
        return panel;
    }

    private void refreshStudentTable() {
        studentTableModel.setRowCount(0);
        String sql = "SELECT id, username, password, full_name, roll_number FROM users WHERE role = 'STUDENT' ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                studentTableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name"),
                        rs.getString("roll_number")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void refreshTeacherTable() {
        teacherTableModel.setRowCount(0);
        String sql = "SELECT id, username, password, full_name FROM users WHERE role = 'TEACHER' ORDER BY id ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                teacherTableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("full_name")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addStudent() {
        String username = studentUsernameField.getText().trim();
        String password = studentPasswordField.getText().trim();
        String fullName = studentFullNameField.getText().trim();
        String roll = studentRollField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty() || roll.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All student fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO users (username, password, role, full_name, roll_number) VALUES (?, ?, 'STUDENT', ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setString(4, roll);
            ps.executeUpdate();
            clearStudentForm();
            refreshStudentTable();
            refreshAssignStudentTable();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateStudent() {
        String idText = studentIdField.getText().trim();
        if (idText.isEmpty()) return;

        String username = studentUsernameField.getText().trim();
        String password = studentPasswordField.getText().trim();
        String fullName = studentFullNameField.getText().trim();
        String roll = studentRollField.getText().trim();

        String sql = "UPDATE users SET username = ?, password = ?, full_name = ?, roll_number = ? WHERE id = ? AND role = 'STUDENT'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setString(4, roll);
            ps.setInt(5, Integer.parseInt(idText));
            ps.executeUpdate();
            clearStudentForm();
            refreshStudentTable();
            refreshAssignStudentTable();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void deleteStudent() {
        String idText = studentIdField.getText().trim();
        if (idText.isEmpty()) return;

        String sql = "DELETE FROM users WHERE id = ? AND role = 'STUDENT'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idText));
            ps.executeUpdate();
            clearStudentForm();
            refreshStudentTable();
            refreshAssignStudentTable();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void clearStudentForm() {
        studentIdField.setText("");
        studentUsernameField.setText("");
        studentPasswordField.setText("");
        studentFullNameField.setText("");
        studentRollField.setText("");
        studentTable.clearSelection();
    }

    private void addTeacher() {
        String username = teacherUsernameField.getText().trim();
        String password = teacherPasswordField.getText().trim();
        String fullName = teacherFullNameField.getText().trim();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty()) return;

        String sql = "INSERT INTO users (username, password, role, full_name) VALUES (?, ?, 'TEACHER', ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.executeUpdate();
            clearTeacherForm();
            refreshTeacherTable();
            refreshAssignTeacherCombo();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateTeacher() {
        String idText = teacherIdField.getText().trim();
        if (idText.isEmpty()) return;

        String username = teacherUsernameField.getText().trim();
        String password = teacherPasswordField.getText().trim();
        String fullName = teacherFullNameField.getText().trim();

        String sql = "UPDATE users SET username = ?, password = ?, full_name = ? WHERE id = ? AND role = 'TEACHER'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, fullName);
            ps.setInt(4, Integer.parseInt(idText));
            ps.executeUpdate();
            clearTeacherForm();
            refreshTeacherTable();
            refreshAssignTeacherCombo();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void deleteTeacher() {
        String idText = teacherIdField.getText().trim();
        if (idText.isEmpty()) return;

        String sql = "DELETE FROM users WHERE id = ? AND role = 'TEACHER'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Integer.parseInt(idText));
            ps.executeUpdate();
            clearTeacherForm();
            refreshTeacherTable();
            refreshAssignTeacherCombo();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void clearTeacherForm() {
        teacherIdField.setText("");
        teacherUsernameField.setText("");
        teacherPasswordField.setText("");
        teacherFullNameField.setText("");
        teacherTable.clearSelection();
    }
}