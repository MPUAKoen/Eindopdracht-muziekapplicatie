package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PieceRequest {
    @NotBlank(message = "title is required", groups = CompletePiece.class)
    @Size(max = 255, message = "title must be at most 255 characters")
    private String title;

    @NotBlank(message = "composer is required", groups = CompletePiece.class)
    @Size(max = 255, message = "composer must be at most 255 characters")
    private String composer;

    @Size(max = 1000, message = "notes must be at most 1000 characters")
    private String notes;

    @NotBlank(message = "category is required", groups = CompletePiece.class)
    @Size(max = 50, message = "category must be at most 50 characters")
    private String category;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getComposer() { return composer; }
    public void setComposer(String composer) { this.composer = composer; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
