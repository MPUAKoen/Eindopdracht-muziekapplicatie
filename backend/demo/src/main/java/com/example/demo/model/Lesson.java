// src/main/java/com/example/demo/model/Lesson.java
package com.example.demo.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "lessons")
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String instrument;
    private String studentName;
    private String teacherName;

    private LocalDate lessonDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @Lob
    private String homework;

    @ElementCollection
    @CollectionTable(name = "lesson_files", joinColumns = @JoinColumn(name = "lesson_id"))
    @Column(name = "filename")
    private List<String> pdfFileNames;

    public Lesson() {}

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

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
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
