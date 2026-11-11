package model;

public class User {
    private int id;
    private String username;
    private String password;
    private String role;
    private String fullName;
    private String rollNumber;

    // INPUT: All field parameters -> ACTION: Initializes a complete User entity -> OUTPUT: None
    public User(int id, String username, String password, String role, String fullName, String rollNumber) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.rollNumber = rollNumber;
    }

    // INPUT: None -> ACTION: Returns user ID -> OUTPUT: int id
    public int getId() {
        return id;
    }

    // INPUT: None -> ACTION: Returns username -> OUTPUT: String username
    public String getUsername() {
        return username;
    }

    // INPUT: None -> ACTION: Returns user password -> OUTPUT: String password
    public String getPassword() {
        return password;
    }

    // INPUT: None -> ACTION: Returns user role -> OUTPUT: String role (ADMIN, TEACHER, STUDENT)
    public String getRole() {
        return role;
    }

    // INPUT: None -> ACTION: Returns full display name -> OUTPUT: String fullName
    public String getFullName() {
        return fullName;
    }

    // INPUT: None -> ACTION: Returns student roll number -> OUTPUT: String rollNumber
    public String getRollNumber() {
        return rollNumber;
    }
}