package com.example.lottos;

/**
 * model class representing a waitlist for an event.
 *
 * Role: Data holder for a list of entrants who are waiting for a spot in an event.
 *
 * Shows a list of entrants who are waiting for a spot in an event.
 * Used by other classes (such as Event-related components) to organize and
 * access waitlisted users in a structured way.
 */

public class WaitList {

    private UserList entrants;

    public WaitList() {

    }


    public UserList getEntrants() {
        return entrants;
    }

    public void setEntrants(UserList entrants) {
        this.entrants = entrants;
    }
}
