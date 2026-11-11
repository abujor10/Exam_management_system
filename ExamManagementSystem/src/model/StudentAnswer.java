package model;

public class StudentAnswer {
    private int id;
    private int submissionId;
    private int questionId;
    private String answerText;
    private double awardedMarks;

    // INPUT: Response attributes -> ACTION: Instantiates student answer entry -> OUTPUT: None
    public StudentAnswer(int id, int submissionId, int questionId, String answerText, double awardedMarks) {
        this.id = id;
        this.submissionId = submissionId;
        this.questionId = questionId;
        this.answerText = answerText;
        this.awardedMarks = awardedMarks;
    }

    // INPUT: None -> ACTION: Returns response entry ID -> OUTPUT: int id
    public int getId() { return id; }

    // INPUT: None -> ACTION: Returns parent submission ID -> OUTPUT: int submissionId
    public int getSubmissionId() { return submissionId; }

    // INPUT: None -> ACTION: Returns referenced question ID -> OUTPUT: int questionId
    public int getQuestionId() { return questionId; }

    // INPUT: None -> ACTION: Returns written response text -> OUTPUT: String answerText
    public String getAnswerText() { return answerText; }

    // INPUT: None -> ACTION: Returns score credited -> OUTPUT: double awardedMarks
    public double getAwardedMarks() { return awardedMarks; }
}