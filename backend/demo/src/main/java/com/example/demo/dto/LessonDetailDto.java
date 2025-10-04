// src/main/java/com/example/demo/dto/LessonDetailDto.java
package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class LessonDetailDto {
    private Long id;
    private String instrument;
    private String teacherName;
    private String studentName;
    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String homework;
    private List<String> pdfFileNames;

    // Constructor
    public LessonDetailDto(Long id, String instrument, String teacherName, String studentName,
                           LocalDate lessonDate, LocalTime startTime, LocalTime endTime,
                           String homework, List<String> pdfFileNames) {
        this.id = id;
        this.instrument = instrument;
        this.teacherName = teacherName;
        this.studentName = studentName;
        this.lessonDate = lessonDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.homework = homework;
        this.pdfFileNames = pdfFileNames;
    }

    // Empty constructor (needed for Jackson sometimes)
    public LessonDetailDto() {}

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public LocalDate getLessonDate() {
        return lessonDate;
    }

    public void setLessonDate(LocalDate lessonDate) {
        this.lessonDate = lessonDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getHomework() {
        return homework;
    }

    public void setHomework(String homework) {
        this.homework = homework;
    }

    public List<String> getPdfFileNames() {
        return pdfFileNames;
    }

    public void setPdfFileNames(List<String> pdfFileNames) {
        this.pdfFileNames = pdfFileNames;
    }
}
