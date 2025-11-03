package com.example.lottos;

import android.security.keystore.UserPresenceUnavailableException;

import java.util.ArrayList;

/**
 * This is a class that keeps a list of User objects
 */
public class UserList {
    private ArrayList<User> users = new ArrayList<User>();

    /**
     * Getter method for user list
     * @return The list of users
     */
    public ArrayList<User> getUsers() {
        return users;
    }

    /**
     * Method to add user to user list
     * @param user The user to add to the user list
     */
    public void addUser(User user) {
        users.add(user);
    }

    /**
     * Method to remove user from user list
     * @param user The user to remove from user list
     */
    public void removeUser(User user) {
        users.remove(user);
    }
}
