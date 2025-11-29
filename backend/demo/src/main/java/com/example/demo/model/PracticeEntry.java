package com.example.demo.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "practice_entry")
public class PracticeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "practice_log_id", nullable = false)
    private PracticeLog practiceLog;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PracticeType type;

    @Column(nullable = false)
    private LocalDateTime dateTime;

    @Column(nullable = false)
    private int minutes;

    public Long getId() {
        return id;
    }

    public PracticeLog getPracticeLog() {
        return practiceLog;
    }

    public void setPracticeLog(PracticeLog practiceLog) {
        this.practiceLog = practiceLog;
    }

    public PracticeType getType() {
        return type;
    }

    public void setType(PracticeType type) {
        this.type = type;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }
}
