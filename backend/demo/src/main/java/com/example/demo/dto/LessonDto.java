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
    private String homework;
    private List<String> pdfFileNames;

    public LessonDto(
            Long id,
            String instrument,
            LocalDate lessonDate,
            LocalTime startTime,
            LocalTime endTime,
            String homework,
            List<String> pdfFileNames) {
        this.id = id;
        this.instrument = instrument;
        this.lessonDate = lessonDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.homework = homework;
        this.pdfFileNames = pdfFileNames;
    }

    // getters only (no setters needed if you only send)
    public Long getId() {
        return id;
    }

    public String getInstrument() {
        return instrument;
    }

    public LocalDate getLessonDate() {
        return lessonDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getHomework() {
        return homework;
    }

    public List<String> getPdfFileNames() {
        return pdfFileNames;
    }
}
