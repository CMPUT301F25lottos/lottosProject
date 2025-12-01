package com.example.lottos.entities;

/**
 * Represents a generic user in the Lottos application, combining both Entrant and Organizer functionalities.
 *
 * Role: This class serves as a unified data model for a user, storing their unique username,
 * personal details (UserInfo), and all event-related lists. By consolidating properties
 * for organizing events (openEvents, closedEvents) and participating in them (waitListedEvents,
 * selectedEvents, etc.), it simplifies data management in Firestore, allowing a single
 * user document to represent all possible roles.
 */
public class User {
    /**
     * The unique identifier for the user.
     */
    private String userName;
    /**
     * An object containing personal details of the user, such as name, email, and phone number.
     */
    private UserInfo userInfo;

    // Combined event lists for both entrant and organizer functionality
    /**
     * A list of events created by this user that are currently open or ongoing.
     */
    private EventList openEvents;
    /**
     * A list of events created by this user that have concluded.
     */
    private EventList closedEvents;
    /**
     * A list of events for which the user is on the waitlist.
     */
    private EventList waitListedEvents;
    /**
     * A list of events for which the user has been selected but has not yet responded.
     */
    private EventList selectedEvents;
    /**
     * A list of events for which the user was on the waitlist but was not chosen.
     */
    private EventList notSelectedEvents;
    /**
     * A list of events for which the user declined an invitation.
     */
    private EventList declinedEvents;
    /**
     * A list of events in which the user is officially enrolled.
     */
    private EventList enrolledEvents;

    /**
     * Empty constructor required for Firestore data mapping.
     * Firestore uses this to deserialize documents back into User objects.
     */
    public User() { }

    /**
     * Constructs a User object with a specified username and user information.
     * All event lists are initialized to prevent null pointer exceptions.
     *
     * @param userName The unique username for the user.
     * @param userInfo The UserInfo object containing personal details.
     */
    public User(String userName, UserInfo userInfo) {
        this.userName = userName;
        this.userInfo = userInfo;

        // Initialize all lists to avoid null pointers
        this.openEvents = new EventList();
        this.closedEvents = new EventList();
        this.waitListedEvents = new EventList();
        this.selectedEvents = new EventList();
        this.notSelectedEvents = new EventList();
        this.declinedEvents = new EventList();
        this.enrolledEvents = new EventList();
    }

    // --- Getters and setters ---
    /**
     * Gets the user's unique username.
     * @return The username string.
     */
    public String getUserName() { return userName; }
    /**
     * Sets the user's unique username.
     * @param userName The new username string.
     */
    public void setUserName(String userName) { this.userName = userName; }

    /**
     * Gets the user's personal information object.
     * @return The UserInfo object.
     */
    public UserInfo getUserInfo() { return userInfo; }
    /**
     * Sets the user's personal information object.
     * @param userInfo The new UserInfo object.
     */
    public void setUserInfo(UserInfo userInfo) { this.userInfo = userInfo; }

    /**
     * Gets the list of open events organized by the user.
     * @return The EventList of open events.
     */
    public EventList getOpenEvents() { return openEvents; }
    /**
     * Sets the list of open events organized by the user.
     * @param openEvents The new EventList for open events.
     */
    public void setOpenEvents(EventList openEvents) { this.openEvents = openEvents; }

    /**
     * Gets the list of closed events organized by the user.
     * @return The EventList of closed events.
     */
    public EventList getClosedEvents() { return closedEvents; }
    /**
     * Sets the list of closed events organized by the user.
     * @param closedEvents The new EventList for closed events.
     */
    public void setClosedEvents(EventList closedEvents) { this.closedEvents = closedEvents; }

    /**
     * Gets the list of events the user is waitlisted for.
     * @return The EventList of waitlisted events.
     */
    public EventList getWaitListedEvents() { return waitListedEvents; }
    /**
     * Sets the list of events the user is waitlisted for.
     * @param waitListedEvents The new EventList for waitlisted events.
     */
    public void setWaitListedEvents(EventList waitListedEvents) { this.waitListedEvents = waitListedEvents; }

    /**
     * Gets the list of events the user has been selected for.
     * @return The EventList of selected events.
     */
    public EventList getselectedEvents() { return selectedEvents; }
    /**
     * Sets the list of events the user has been selected for.
     * @param selectedEvents The new EventList for selected events.
     */
    public void setselectedEvents(EventList selectedEvents) { this.selectedEvents = selectedEvents; }

    /**
     * Gets the list of events the user was not selected for.
     * @return The EventList of not-selected events.
     */
    public EventList getnotSelectedEvents() { return notSelectedEvents; }
    /**
     * Sets the list of events the user was not selected for.
     * @param notSelectedEvents The new EventList for not-selected events.
     */
    public void setnotSelectedEvents(EventList notSelectedEvents) { this.notSelectedEvents = notSelectedEvents; }

    /**
     * Gets the list of events the user has declined.
     * @return The EventList of declined events.
     */
    public EventList getDeclinedEvents() { return declinedEvents; }
    /**
     * Sets the list of events the user has declined.
     * @param declinedEvents The new EventList for declined events.
     */
    public void setDeclinedEvents(EventList declinedEvents) { this.declinedEvents = declinedEvents; }

    /**
     * Gets the list of events the user is enrolled in.
     * @return The EventList of enrolled events.
     */
    public EventList getEnrolledEvents() { return enrolledEvents; }
    /**
     * Sets the list of events the user is enrolled in.
     * @param enrolledEvents The new EventList for enrolled events.
    s*/
    public void setEnrolledEvents(EventList enrolledEvents) { this.enrolledEvents = enrolledEvents; }

    // --- Equality check based on unique username ---
    /**
     * Compares this User object to another object for equality.
     * Two User objects are considered equal if their usernames are identical.
     *
     * @param user The object to compare with this user.
     * @return true if the objects are of the same class and have the same username, false otherwise.
     */
    @Override
    public boolean equals(Object user) {
        if (this == user) return true;
        if (user == null || getClass() != user.getClass()) return false;
        User check = (User) user;
        return userName.equals(check.getUserName());
    }

    /**
     * Generates a hash code for the User object.
     * The hash code is based on the user's unique username.
     *
     * @return An integer hash code.
     */
    @Override
    public int hashCode() {
        return userName.hashCode();
    }


}
