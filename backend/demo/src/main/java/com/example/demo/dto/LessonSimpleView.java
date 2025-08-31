// src/main/java/com/example/demo/dto/LessonSimpleView.java
package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Interface projection so Spring Data returns only scalars.
 * No lazy loading, no recursion, no 500s.
 */
public interface LessonSimpleView {
    Long getId();
    String getInstrument();
    LocalDate getLessonDate();
    LocalTime getStartTime();
    LocalTime getEndTime();
    Long getStudentId();
    String getStudentName();
}
