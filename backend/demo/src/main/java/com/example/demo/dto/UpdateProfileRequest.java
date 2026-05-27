package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {
    @Size(max = 255, message = "Name must be at most 255 characters")
    private String name;

    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    @Size(max = 255, message = "Instrument must be at most 255 characters")
    private String instrument;

    @Size(min = 6, max = 72, message = "Password must be between 6 and 72 characters")
    private String password;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getInstrument() { return instrument; }
    public void setInstrument(String instrument) { this.instrument = instrument; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
