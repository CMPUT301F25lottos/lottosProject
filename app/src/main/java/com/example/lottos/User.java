package com.example.lottos;

public abstract class User {
    private String userName;
    private UserInfo userInfo;

    /**
     * Constructs a User object
     * @param userName The name of the user
     * @param userInfo The UserInfo object containing the user's info
     */
    public User(String userName, UserInfo userInfo) {
        this.userName = userName;
        this.userInfo = userInfo;
    }

    /**
     * Getter method for username
     * @return The username of the user
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Getter method for userInfo
     * @return The UserInfo object containing the user's info
     */
    public UserInfo getUserInfo() {
        return userInfo;
    }

    /**
     * Setter method for username
     * @param userName The new username to set
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Setter method for userInfo
     * @param userInfo The new UserInfo object to be set
     */
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public boolean equals(Object user) {
        if (this == user) {
            return true;
        }
        if (user == null || (getClass() != user.getClass())) {
            return false;
        }
        else {
            User check = (User) user;
            return (userName.equals(check.getUserName()));
        }
    }

    @Override
    public int hashCode() {
        return userName.hashCode();
    }
}
