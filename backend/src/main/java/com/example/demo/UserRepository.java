package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;

// Repository interface for accessing User entities in the database
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);  // Check if an email already exists in the database
}
