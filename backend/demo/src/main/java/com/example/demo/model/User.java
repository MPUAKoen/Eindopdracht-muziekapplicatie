package com.example.demo.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

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
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role = "USER"; // Default value

    @Column(nullable = true)
    private String instrument;

    @ElementCollection
    @Column(nullable = true)
    private List<String> workingOnPieces;

    @ElementCollection
    @Column(nullable = true)
    private List<String> repertoire;

    @ElementCollection
    @Column(nullable = true)
    private List<String> wishlist;

    // Constructors
    public User() {
    }

    public User(String name, String email, String password, String role, String instrument,
            List<String> workingOnPieces,
            List<String> repertoire, List<String> wishlist) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    // Implementing UserDetails methods

    @Override
    public List<GrantedAuthority> getAuthorities() {
        // Map the role to a SimpleGrantedAuthority
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
    }

    @Override
    public String getUsername() {
        return email; // Or use any other unique identifier for your user
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
