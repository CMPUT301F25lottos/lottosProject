package com.example.lottos.entities;

/**
 * A data class representing the personal information of a user.
 *
 * Role: This class acts as a plain old Java object (POJO) to store core user details
 * such as their display name, password, email, and phone number. It is typically
 * embedded within a main User object in Firestore to encapsulate and organize
 * user-specific data. The empty constructor is essential for Firestore's automatic
 * data deserialization.
 */
public class UserInfo {
    private String displayName;
    private String password;
    private String email;
    private String phoneNumber;

    /**
     * Empty constructor required for Firestore deserialization.
     * Firestore uses this to create instances of UserInfo when converting documents back into Java objects.
     */
    public UserInfo() {
        // Firestore requires this
    }

    /**
     * Constructs a UserInfo object with the user's essential details.
     * @param displayName The user's public or display name.
     * @param password The user's password for authentication (note: storing plaintext passwords is not secure).
     * @param email The user's email address.
     * @param phoneNumber The user's phone number (can be optional).
     */
    public UserInfo(String displayName, String password, String email, String phoneNumber) {
        this.displayName = displayName;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    // --- Getters ---
    /**
     * Gets the user's display name.
     * @return The display name string.
     */
    public String getDisplayName() { return displayName; }
    /**
     * Gets the user's password.
     * @return The password string.
     */
    public String getPassword() { return password; }
    /**
     * Gets the user's email address.
     * @return The email address string.
     */
    public String getEmail() { return email; }
    /**
     * Gets the user's phone number.
     * @return The phone number string.
     */
    public String getPhoneNumber() { return phoneNumber; }

    // --- Setters ---
    /**
     * Sets the user's display name.
     * @param displayName The new display name to set.
     */
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    /**
     * Sets the user's password.
     * @param password The new password to set.
     */
    public void setPassword(String password) { this.password = password; }
    /**
     * Sets the user's email address.
     * @param email The new email address to set.
     */
    public void setEmail(String email) { this.email = email; }
    /**
     * Sets the user's phone number.
     * @param phoneNumber The new phone number to set.
     */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
}

