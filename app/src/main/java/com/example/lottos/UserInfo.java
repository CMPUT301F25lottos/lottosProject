package com.example.lottos;

public class UserInfo {
    private String name;
    private String password;
    private String email;
    private String phoneNumber;

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

    /**
     * Getter method for username
     *
     * @return The name of the user
     */
    public String getName() {
        return name;
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
     * Setter method for name
     *
     * @param name The new name to set
     */
    public void setName(String name) {
        this.name = name;
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
     *
     * @param phoneNumber The new phone number to be set
     */
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}