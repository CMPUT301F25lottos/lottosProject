package com.example.lottos.entities;

public class Entrant extends User {
    private String userName;
    private UserInfo userInfo;
    private EventList waitListedEvents;
    private EventList selectedEvents;
    private EventList notSelectedEvents;
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
     * Getter method for selected events list
     * @return The EventList object for selected events
     */
    public EventList getselectedEvents() {
        return selectedEvents;
    }

    /**
     * Getter method for notSelected events list
     * @return The EventList object for notSelected events
     */
    public EventList getnotSelectedEvents() {
        return notSelectedEvents;
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
     * Setter method for selected events list
     * @param selectedEvents The new EventList to be set
     */
    public void setselectedEvents(EventList selectedEvents) {
        this.selectedEvents = selectedEvents;
    }

    /**
     * Setter method for notSelected events
     * @param notSelectedEvents The new EventList to be set
     */
    public void setnotSelectedEvents(EventList notSelectedEvents) {
        this.notSelectedEvents = notSelectedEvents;
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
