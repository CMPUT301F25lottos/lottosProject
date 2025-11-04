package com.example.lottos;

public class Entrant extends User{
    private String userName;
    private UserInfo userInfo;
    private EventList waitListedEvents;
    private EventList invitedEvents;
    private EventList uninvitedEvents;
    private EventList declinedEvents;
    private EventList enrolledEvents;

    /**
     * Constructs an Entrant object
     * @param userName The userName of the entrant
     * @param userInfo The UserInfo object for the entrant
     */
    public Entrant(String userName, UserInfo userInfo) {
        super(userName, userInfo);
    }

    /**
     * Getter method for waitlisted events list
     * @return The EventList object for waitlisted events
     */
    public EventList getWaitListedEvents() {
        return waitListedEvents;
    }

    /**
     * Getter method for invited events list
     * @return The EventList object for invited events
     */
    public EventList getInvitedEvents() {
        return invitedEvents;
    }

    /**
     * Getter method for uninvited events list
     * @return The EventList object for uninvited events
     */
    public EventList getUninvitedEvents() {
        return uninvitedEvents;
    }

    /**
     * Getter method for declined events list
     * @return The EventList object for declined events
     */
    public EventList getDeclinedEvents() {
        return declinedEvents;
    }

    /**
     * Getter method for enrolled events list
     * @return The EventList object for enrolled events
     */
    public EventList getEnrolledEvents() {
        return enrolledEvents;
    }

    /**
     * Setter method for waitlisted events list
     * @param waitListedEvents The new EventList to be set
     */
    public void setWaitListedEvents(EventList waitListedEvents) {
        this.waitListedEvents = waitListedEvents;
    }

    /**
     * Setter method for invited events list
     * @param invitedEvents The new EventList to be set
     */
    public void setInvitedEvents(EventList invitedEvents) {
        this.invitedEvents = invitedEvents;
    }

    /**
     * Setter method for uninvited events
     * @param uninvitedEvents The new EventList to be set
     */
    public void setUninvitedEvents(EventList uninvitedEvents) {
        this.uninvitedEvents = uninvitedEvents;
    }

    /**
     * Setter method for declined events
     * @param declinedEvents The new EventList to set
     */
    public void setDeclinedEvents(EventList declinedEvents) {
        this.declinedEvents = declinedEvents;
    }

    /**
     * Setter method for enrolled events
     * @param enrolledEvents The new EventList to be set
     */
    public void setEnrolledEvents(EventList enrolledEvents) {
        this.enrolledEvents = enrolledEvents;
    }
}
