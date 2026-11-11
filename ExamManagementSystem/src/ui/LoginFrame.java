package ui;

import db.DBConnection;
import model.User;
import util.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;

    // INPUT: None -> ACTION: Builds the authentication window and binds login handlers -> OUTPUT: LoginFrame instance
    public LoginFrame() {
        setTitle("Exam Management System - Authentication");
        setSize(450, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));

        // Header Panel with title and theme switch
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("System Login", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        JButton themeBtn = new JButton("Toggle Theme");
        themeBtn.setFocusPainted(false);
        themeBtn.addActionListener(e -> ThemeManager.toggleTheme(this));

        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(themeBtn, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Input Form Panel
        JPanel formPanel = new JPanel(new GridLayout(6, 1, 8, 8));

        JLabel roleLabel = new JLabel("Select Role:");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        roleCombo = new JComboBox<>(new String[]{"STUDENT", "TEACHER", "ADMIN"});
        roleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel userLabel = new JLabel("Username / Roll Number:");
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        formPanel.add(roleLabel);
        formPanel.add(roleCombo);
        formPanel.add(userLabel);
        formPanel.add(usernameField);
        formPanel.add(passLabel);
        formPanel.add(passwordField);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Action Panel
        JPanel actionPanel = new JPanel(new GridLayout(2, 1, 10, 10));
        JButton loginBtn = new JButton("Authenticate & Enter");
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        loginBtn.setBackground(new Color(24, 119, 242));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.addActionListener(this::handleLogin);

        JLabel hintLabel = new JLabel("Demo Logins: admin/admin123, teacher1/teach123, student1/stud123", SwingConstants.CENTER);
        hintLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));

        actionPanel.add(loginBtn);
        actionPanel.add(hintLabel);

        mainPanel.add(actionPanel, BorderLayout.SOUTH);
        add(mainPanel);
    }

    // INPUT: ActionEvent e -> ACTION: Validates credentials against DB and opens destination frame -> OUTPUT: None
    private void handleLogin(ActionEvent e) {
        String inputIdentifier = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String selectedRole = (String) roleCombo.getSelectedItem();

        if (inputIdentifier.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please provide all required login credentials.", "Validation Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "SELECT * FROM users WHERE (username = ? OR roll_number = ?) AND password = ? AND role = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, inputIdentifier);
            ps.setString(2, inputIdentifier);
            ps.setString(3, password);
            ps.setString(4, selectedRole);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"),
                            rs.getString("full_name"),
                            rs.getString("roll_number")
                    );

                    JOptionPane.showMessageDialog(this, "Welcome, " + user.getFullName() + "!", "Login Success", JOptionPane.INFORMATION_MESSAGE);
                    routeUser(user);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials or unauthorized role access.", "Access Denied", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Database Error: " + ex.getMessage(), "Connection Fault", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    // INPUT: User authenticatedUser -> ACTION: Instantiates target dashboard based on user role -> OUTPUT: None
    private void routeUser(User user) {
        switch (user.getRole()) {
            case "ADMIN":
                new AdminDashboard(user).setVisible(true);
                break;
            case "TEACHER":
                new TeacherDashboard(user).setVisible(true);
                break;
            case "STUDENT":
                new StudentDashboard(user).setVisible(true);
                break;
            default:
                JOptionPane.showMessageDialog(this, "Unknown role assigned.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}