package com.example.lottos.entities;

import java.util.ArrayList;

/**
 * Represents a collection of users, where each user is identified by their unique username.
 *
 * Role: This class acts as a wrapper around an {@code ArrayList<String>} to manage
 * a list of user identifiers. It provides simple methods to add, remove, and retrieve
 * the usernames in the list. This is typically used within an Event object
 * to track its participants (e.g., waitList, enrolledList).
 */
public class UserList {
    /**
     * The underlying list that stores the unique usernames of the users.
     */
    private ArrayList<String> users = new ArrayList<String>();

    /**
     * Gets the complete list of user identifiers.
     * @return An {@code ArrayList<String>} containing the usernames.
     */
    public ArrayList<String> getUsers() {
        return users;
    }

    /**
     * Adds a user's username to the list.
     * @param userName The unique username of the user to add.
     */
    public void addUser(String userName) {
        users.add(userName);
    }

    /**
     * Removes a user's username from the list.
     * If the specified username is not in the list, this method does nothing.
     * @param userName The unique username of the user to remove.
     */
    public void removeUser(String userName) {
        users.remove(userName);
    }
}
