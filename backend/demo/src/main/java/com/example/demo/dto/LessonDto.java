// src/main/java/com/example/demo/dto/LessonDto.java
package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class LessonDto {
    private Long id;
    private String instrument;
    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private Long studentId;
    private String studentName;

    private Long teacherId;
    private String teacherName;

    private String homework;
    private List<String> pdfFileNames;

    public LessonDto(
            Long id,
            String instrument,
            LocalDate lessonDate,
            LocalTime startTime,
            LocalTime endTime,
            Long studentId,
            String studentName,
            Long teacherId,
            String teacherName,
            String homework,
            List<String> pdfFileNames
    ) {
        this.id = id;
        this.instrument = instrument;
        this.lessonDate = lessonDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.studentId = studentId;
        this.studentName = studentName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.homework = homework;
        this.pdfFileNames = pdfFileNames;
    }

    public Long getId() { return id; }
    public String getInstrument() { return instrument; }
    public LocalDate getLessonDate() { return lessonDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public Long getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public Long getTeacherId() { return teacherId; }
    public String getTeacherName() { return teacherName; }
    public String getHomework() { return homework; }
    public List<String> getPdfFileNames() { return pdfFileNames; }
}
