package com.example.lottos;

public class UserInfo {
    private String username;
    private String password;
    private String email;
    private String phoneNumber;

    /**
     * Constructs a UserInfo object
     * @param username    The username of the user
     * @param password    The password of the user
     * @param email       The email of the user
     * @param phoneNumber The phone number of the user (optional)
     */
    public UserInfo(String username, String password, String email, String phoneNumber) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.phoneNumber = phoneNumber;
    }

    /**
     * Getter method for username
     *
     * @return The username of the user
     */
    public String getUsername() {
        return username;
    }

    /**
     * Getter method for password
     *
     * @return The password of the user
     */
    public String getPassword() {
        return password;
    }

    /**
     * Getter method for email
     *
     * @return The email of the user
     */
    public String getEmail() {
        return email;
    }

    /**
     * Getter method for phone number
     *
     * @return Phone number of the user
     */
    public String getPhoneNumber() {
        return phoneNumber;
    }


    /**
     * Setter method for username
     *
     * @param username The new username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Setter method for password
     *
     * @param password The new password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Setter method for email
     *
     * @param email The new email to be set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Setter method for phone number
     * @param phoneNumber The new phone number to be set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}