package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class LessonCreateRequest {
    @NotBlank(message = "instrument is required")
    @Size(max = 255, message = "instrument must be at most 255 characters")
    private String instrument;

    @NotNull(message = "studentId is required")
    private Long studentId;

    @NotBlank(message = "lessonDate is required")
    private String lessonDate;

    @NotBlank(message = "startTime is required")
    private String startTime;

    @NotBlank(message = "endTime is required")
    private String endTime;

    @Size(max = 5000, message = "homework must be at most 5000 characters")
    private String homework;

    private List<MultipartFile> pdfFiles;

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
    public List<MultipartFile> getPdfFiles() { return pdfFiles; }
    public void setPdfFiles(List<MultipartFile> pdfFiles) { this.pdfFiles = pdfFiles; }
}
