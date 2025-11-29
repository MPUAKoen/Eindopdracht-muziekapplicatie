package com.example.demo.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "practice_log")
public class PracticeLog {

    @Id
    private Long id;  // same as user id

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "practiceLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PracticeEntry> entries = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<PracticeEntry> getEntries() {
        return entries;
    }

    public void addEntry(PracticeEntry entry) {
        entries.add(entry);
        entry.setPracticeLog(this);
    }

    public void removeEntry(PracticeEntry entry) {
        entries.remove(entry);
        entry.setPracticeLog(null);
    }
}
