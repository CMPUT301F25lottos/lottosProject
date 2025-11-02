package com.example.lottos;

public abstract class User {
    private UserInfo userInfo;

    /**
     * Constructs a User object
     * @param userInfo The UserInfo object containing the user's info
     */
    public User(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    /**
     * Getter method for userInfo
     * @return The UserInfo object containing the user's info
     */
    public UserInfo getUserInfo() {
        return userInfo;
    }

    /**
     * Setter method for userInfo
     * @param userInfo The new UserInfo object to be set
     */
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }
}
