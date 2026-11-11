package model;

import java.sql.Date;

public class Exam {
    private int id;
    private String title;
    private int durationMinutes;
    private Date examDate;
    private int createdByTeacherId;
    private boolean isPublished;

    // INPUT: All exam attributes -> ACTION: Initializes an Exam record instance -> OUTPUT: None
    public Exam(int id, String title, int durationMinutes, Date examDate, int createdByTeacherId, boolean isPublished) {
        this.id = id;
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.examDate = examDate;
        this.createdByTeacherId = createdByTeacherId;
        this.isPublished = isPublished;
    }

    // INPUT: None -> ACTION: Returns exam primary key -> OUTPUT: int id
    public int getId() {
        return id;
    }

    // INPUT: None -> ACTION: Returns exam title -> OUTPUT: String title
    public String getTitle() {
        return title;
    }

    // INPUT: None -> ACTION: Returns duration limit -> OUTPUT: int durationMinutes
    public int getDurationMinutes() {
        return durationMinutes;
    }

    // INPUT: None -> ACTION: Returns scheduled date -> OUTPUT: Date examDate
    public Date getExamDate() {
        return examDate;
    }

    // INPUT: None -> ACTION: Returns creator ID -> OUTPUT: int createdByTeacherId
    public int getCreatedByTeacherId() {
        return createdByTeacherId;
    }

    // INPUT: None -> ACTION: Returns published status -> OUTPUT: boolean isPublished
    public boolean isPublished() {
        return isPublished;
    }

    // INPUT: boolean status -> ACTION: Sets published status flag -> OUTPUT: None
    public void setPublished(boolean published) {
        isPublished = published;
    }
}
