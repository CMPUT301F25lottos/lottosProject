package com.example.lottos.entities;

import java.util.ArrayList;

/**
 * This is a class that keeps a list of User objects
 */
public class UserList {
    private ArrayList<String> users = new ArrayList<String>();

    /**
     * Getter method for user list
     * @return The list of users
     */
    public ArrayList<String> getUsers() {
        return users;
    }

    /**
     * Method to add user to user list
     * @param userName The user to add to the user list
     */
    public void addUser(String userName) {
        users.add(userName);
    }

    /**
     * Method to remove user from user list
     * @param userName The user to remove from user list
     */
    public void removeUser(String userName) {
        users.remove(userName);
    }
}
