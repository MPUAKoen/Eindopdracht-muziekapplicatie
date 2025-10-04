package com.example.demo.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
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

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Piece> favoritePieces;

    // === Relationship to a teacher (many users -> one teacher)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    @JsonBackReference
    private User teacher;

    // === Relationship to users (students) (one teacher -> many users)
    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<User> users = new ArrayList<>();

    // === Constructors ===
    public User() {}

    public User(String name, String email, String password, String role, String instrument,
                List<Piece> workingOnPieces, List<Piece> repertoire,
                List<Piece> wishlist, List<Piece> favoritePieces, User teacher) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.instrument = instrument;
        this.workingOnPieces = workingOnPieces;
        this.repertoire = repertoire;
        this.wishlist = wishlist;
        this.favoritePieces = favoritePieces;
        this.teacher = teacher;
    }

    // === Getters and Setters ===
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    @Override
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getInstrument() { return instrument; }
    public void setInstrument(String instrument) { this.instrument = instrument; }

    public List<Piece> getWorkingOnPieces() { return workingOnPieces; }
    public void setWorkingOnPieces(List<Piece> workingOnPieces) { this.workingOnPieces = workingOnPieces; }

    public List<Piece> getRepertoire() { return repertoire; }
    public void setRepertoire(List<Piece> repertoire) { this.repertoire = repertoire; }

    public List<Piece> getWishlist() { return wishlist; }
    public void setWishlist(List<Piece> wishlist) { this.wishlist = wishlist; }

    public List<Piece> getFavoritePieces() { return favoritePieces; }
    public void setFavoritePieces(List<Piece> favoritePieces) { this.favoritePieces = favoritePieces; }

    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    // === Utility helpers ===
    public void addUser(User user) {
        users.add(user);
        user.setTeacher(this);
    }

    public void removeUser(User user) {
        users.remove(user);
        user.setTeacher(null);
    }

    // === Spring Security ===
    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
    }

    @Override
    public String getUsername() { return email; }

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
