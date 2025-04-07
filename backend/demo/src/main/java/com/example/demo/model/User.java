package com.example.demo.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = true)
    private String instrument; // Instrument the user is playing (nullable)

    @ElementCollection
    @Column(nullable = true)
    private List<String> workingOnPieces; // List of pieces the user is working on (nullable)

    @ElementCollection
    @Column(nullable = true)
    private List<String> repertoire; // List of repertoire the user has (nullable)

    @ElementCollection
    @Column(nullable = true)
    private List<String> wishlist; // List of pieces the user wants to play (nullable)

    // Constructors
    public User() {
    }

    public User(String name, String email, String password, String instrument, List<String> workingOnPieces,
            List<String> repertoire, List<String> wishlist) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.instrument = instrument;
        this.workingOnPieces = workingOnPieces;
        this.repertoire = repertoire;
        this.wishlist = wishlist;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }

    public List<String> getWorkingOnPieces() {
        return workingOnPieces;
    }

    public void setWorkingOnPieces(List<String> workingOnPieces) {
        this.workingOnPieces = workingOnPieces;
    }

    public List<String> getRepertoire() {
        return repertoire;
    }

    public void setRepertoire(List<String> repertoire) {
        this.repertoire = repertoire;
    }

    public List<String> getWishlist() {
        return wishlist;
    }

    public void setWishlist(List<String> wishlist) {
        this.wishlist = wishlist;
    }
}
