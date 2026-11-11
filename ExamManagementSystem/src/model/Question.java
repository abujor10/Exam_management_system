package model;

public class Question {
    private int id;
    private int examId;
    private String type; // "MCQ" or "CQ"
    private String stem;
    private String stemGroupId;
    private String subLabel; // e.g., "(a)", "(b)", "(c)", "(d)"
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctOption;
    private String modelAnswer;
    private double marks;
    private int wordLimit;

    // INPUT: Complete question attributes -> ACTION: Initializes Question model -> OUTPUT: None
    public Question(int id, int examId, String type, String stem, String stemGroupId, String subLabel,
                    String questionText, String optionA, String optionB, String optionC, String optionD,
                    String correctOption, String modelAnswer, double marks, int wordLimit) {
        this.id = id;
        this.examId = examId;
        this.type = type;
        this.stem = stem;
        this.stemGroupId = stemGroupId;
        this.subLabel = subLabel;
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
        this.modelAnswer = modelAnswer;
        this.marks = marks;
        this.wordLimit = wordLimit;
    }

    public int getId() { return id; }
    public int getExamId() { return examId; }
    public String getType() { return type; }
    public String getStem() { return stem; }
    public String getStemGroupId() { return stemGroupId; }
    public String getSubLabel() { return subLabel != null ? subLabel : ""; }
    public String getQuestionText() { return questionText; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
    public String getCorrectOption() { return correctOption; }
    public String getModelAnswer() { return modelAnswer; }
    public double getMarks() { return marks; }
    public int getWordLimit() { return wordLimit; }
}