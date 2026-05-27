package com.example.demo.dto;

import jakarta.validation.constraints.Size;

public class LessonUpdateRequest {
    @Size(max = 255, message = "instrument must be at most 255 characters")
    private String instrument;

    private Long studentId;
    private String lessonDate;
    private String startTime;
    private String endTime;

    @Size(max = 5000, message = "homework must be at most 5000 characters")
    private String homework;

    public String getInstrument() { return instrument; }
    public void setInstrument(String instrument) { this.instrument = instrument; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getLessonDate() { return lessonDate; }
    public void setLessonDate(String lessonDate) { this.lessonDate = lessonDate; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getHomework() { return homework; }
    public void setHomework(String homework) { this.homework = homework; }
}
