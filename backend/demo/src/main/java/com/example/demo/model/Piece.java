package com.example.demo.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Piece {
    private String title;
    private String composer;
    private String notes;

    public Piece() {
    }

    public Piece(String title, String composer, String notes) {
        this.title = title;
        this.composer = composer;
        this.notes = notes;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getComposer() {
        return composer;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
