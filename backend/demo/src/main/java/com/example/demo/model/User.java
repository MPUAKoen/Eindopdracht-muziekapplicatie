package com.example.demo.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    private static final long serialVersionUID = 1L;

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
    private String role = "USER";

    @Column(nullable = true)
    private String instrument;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Piece> workingOnPieces;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Piece> repertoire;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Piece> wishlist;

    // New collection for favorite pieces.
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Piece> favoritePieces;

    // Constructors, getters and setters

    public User() {
    }

    public User(String name, String email, String password, String role, String instrument,
            List<Piece> workingOnPieces, List<Piece> repertoire, List<Piece> wishlist, List<Piece> favoritePieces) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.instrument = instrument;
        this.workingOnPieces = workingOnPieces;
        this.repertoire = repertoire;
        this.wishlist = wishlist;
        this.favoritePieces = favoritePieces;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // name, email, password, role, instrument getters/setters…

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

    @Override
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

    public List<Piece> getWorkingOnPieces() {
        return workingOnPieces;
    }

    public void setWorkingOnPieces(List<Piece> workingOnPieces) {
        this.workingOnPieces = workingOnPieces;
    }

    public List<Piece> getRepertoire() {
        return repertoire;
    }

    public void setRepertoire(List<Piece> repertoire) {
        this.repertoire = repertoire;
    }

    public List<Piece> getWishlist() {
        return wishlist;
    }

    public void setWishlist(List<Piece> wishlist) {
        this.wishlist = wishlist;
    }

    public List<Piece> getFavoritePieces() {
        return favoritePieces;
    }

    public void setFavoritePieces(List<Piece> favoritePieces) {
        this.favoritePieces = favoritePieces;
    }

    // UserDetails methods

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
    }

    @Override
    public String getUsername() {
        return email;
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
