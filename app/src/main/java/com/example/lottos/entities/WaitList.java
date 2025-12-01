package com.example.lottos.entities;

/**
 * A model class representing a waitlist for an event.
 *
 * Role: This class serves as a specific data holder for a list of entrants who are
 * waiting for a spot in an event. It encapsulates a {@link UserList}, providing a
 * clear, domain-specific name for this collection of users. It is used by other
 * classes (such as Event-related components) to organize and access waitlisted
 * users in a structured way.
 */
public class WaitList {

    /**
     * The list of users (entrants) on the waitlist, identified by their usernames.
     */
    private UserList entrants;

    /**
     * Default constructor. Initializes a new, empty WaitList object.
     * This is also required for frameworks like Firestore for object deserialization.
     */
    public WaitList() {
        // Initialize the list to prevent null pointer exceptions
        this.entrants = new UserList();
    }


    /**
     * Gets the list of entrants currently on the waitlist.
     * @return The {@link UserList} containing the usernames of the waitlisted entrants.
     */
    public UserList getEntrants() {
        return entrants;
    }

    /**
     * Sets or replaces the list of entrants on the waitlist.
     * @param entrants The new {@link UserList} to be set as the waitlist.
     */
    public void setEntrants(UserList entrants) {
        this.entrants = entrants;
    }
}
