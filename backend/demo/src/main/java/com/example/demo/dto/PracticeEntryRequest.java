package com.example.demo.dto;

import com.example.demo.model.PracticeType;

import java.time.LocalDateTime;

public class PracticeEntryRequest {

    private PracticeType type;
    private Integer minutes;
    private LocalDateTime dateTime;

    public PracticeType getType() {
        return type;
    }

    public void setType(PracticeType type) {
        this.type = type;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }
}
