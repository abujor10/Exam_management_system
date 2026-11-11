package model;

public class Submission {
    private int id;
    private int examId;
    private int studentId;
    private int remainingSeconds;
    private int focusLossCount;
    private String status;
    private double mcqScore;
    private double cqScore;
    private double totalScore;
    private String teacherFeedback;

    // INPUT: Submission state fields -> ACTION: Constructs Submission instance -> OUTPUT: None
    public Submission(int id, int examId, int studentId, int remainingSeconds, int focusLossCount,
                      String status, double mcqScore, double cqScore, double totalScore, String teacherFeedback) {
        this.id = id;
        this.examId = examId;
        this.studentId = studentId;
        this.remainingSeconds = remainingSeconds;
        this.focusLossCount = focusLossCount;
        this.status = status;
        this.mcqScore = mcqScore;
        this.cqScore = cqScore;
        this.totalScore = totalScore;
        this.teacherFeedback = teacherFeedback;
    }

    // INPUT: None -> ACTION: Returns submission primary ID -> OUTPUT: int id
    public int getId() { return id; }

    // INPUT: None -> ACTION: Returns associated exam ID -> OUTPUT: int examId
    public int getExamId() { return examId; }

    // INPUT: None -> ACTION: Returns submitting student ID -> OUTPUT: int studentId
    public int getStudentId() { return studentId; }

    // INPUT: None -> ACTION: Returns remaining duration -> OUTPUT: int remainingSeconds
    public int getRemainingSeconds() { return remainingSeconds; }

    // INPUT: None -> ACTION: Returns focus deviation count -> OUTPUT: int focusLossCount
    public int getFocusLossCount() { return focusLossCount; }

    // INPUT: None -> ACTION: Returns submission state -> OUTPUT: String status
    public String getStatus() { return status; }

    // INPUT: None -> ACTION: Returns calculated MCQ score -> OUTPUT: double mcqScore
    public double getMcqScore() { return mcqScore; }

    // INPUT: None -> ACTION: Returns graded CQ score -> OUTPUT: double cqScore
    public double getCqScore() { return totalScore - mcqScore; }

    // INPUT: None -> ACTION: Returns overall accumulated score -> OUTPUT: double totalScore
    public double getTotalScore() { return totalScore; }

    // INPUT: None -> ACTION: Returns instructor feedback -> OUTPUT: String teacherFeedback
    public String getTeacherFeedback() { return teacherFeedback; }
}
