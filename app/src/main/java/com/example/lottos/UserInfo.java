package com.example.lottos;

public class UserInfo {
    private String name;
    private String password;
    private String email;
    private String phoneNumber;

    /**
     * Empty constructor required for Firestore deserialization.
     */
    public UserInfo() {
        // Firestore requires this
    }

    /**
     * Constructs a UserInfo object
     * @param name The username of the user
     * @param password The password of the user
     * @param email The email of the user
     * @param phoneNumber The phone number of the user (optional)
     */
    public UserInfo(String name, String password, String email, String phoneNumber) {
        this.name = name;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // --- Getters ---
    public String getName() { return name; }
    public String getPassword() { return password; }
    public String getEmail() { return email; }
    public String getPhoneNumber() { return phoneNumber; }

    // --- Setters ---
    public void setName(String name) { this.name = name; }
    public void setPassword(String password) { this.password = password; }
    public void setEmail(String email) { this.email = email; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}
