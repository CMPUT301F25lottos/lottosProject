package com.example.lottos;

public class User {
    private String userName;
    private UserInfo userInfo;

    // Combined event lists for both entrant and organizer functionality
    private EventList openEvents;
    private EventList closedEvents;
    private EventList waitListedEvents;
    private EventList selectedEvents;
    private EventList notSelectedEvents;
    private EventList declinedEvents;
    private EventList enrolledEvents;

    /**
     * Empty constructor for Firestore
     */
    public User() { }

    /**
     * Constructs a User object
     * @param userName The unique username
     * @param userInfo The UserInfo object for personal details
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
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public UserInfo getUserInfo() { return userInfo; }
    public void setUserInfo(UserInfo userInfo) { this.userInfo = userInfo; }

    public EventList getOpenEvents() { return openEvents; }
    public void setOpenEvents(EventList openEvents) { this.openEvents = openEvents; }

    public EventList getClosedEvents() { return closedEvents; }
    public void setClosedEvents(EventList closedEvents) { this.closedEvents = closedEvents; }

    public EventList getWaitListedEvents() { return waitListedEvents; }
    public void setWaitListedEvents(EventList waitListedEvents) { this.waitListedEvents = waitListedEvents; }

    public EventList getselectedEvents() { return selectedEvents; }
    public void setselectedEvents(EventList selectedEvents) { this.selectedEvents = selectedEvents; }

    public EventList getnotSelectedEvents() { return notSelectedEvents; }
    public void setnotSelectedEvents(EventList notSelectedEvents) { this.notSelectedEvents = notSelectedEvents; }

    public EventList getDeclinedEvents() { return declinedEvents; }
    public void setDeclinedEvents(EventList declinedEvents) { this.declinedEvents = declinedEvents; }

    public EventList getEnrolledEvents() { return enrolledEvents; }
    public void setEnrolledEvents(EventList enrolledEvents) { this.enrolledEvents = enrolledEvents; }

    // --- Equality check based on unique username ---
    @Override
    public boolean equals(Object user) {
        if (this == user) return true;
        if (user == null || getClass() != user.getClass()) return false;
        User check = (User) user;
        return userName.equals(check.getUserName());
    }

    @Override
    public int hashCode() {
        return userName.hashCode();
    }


}
