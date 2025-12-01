package com.example.lottos.entities;

/**
 * Represents a user who participates in events, known as an Entrant.
 * This class extends the base {@code User} class and adds specific event lists
 * to track the user's status across various events (e.g., waitlisted, selected, enrolled).
 */
public class Entrant extends User {
    /**
     * The unique username of the entrant.
     */
    private String userName;
    /**
     * The personal information of the entrant.
     */
    private UserInfo userInfo;
    /**
     * A list of events for which the entrant is currently on the waitlist.
     */
    private EventList waitListedEvents;
    /**
     * A list of events for which the entrant has been selected but has not yet accepted or declined.
     */
    private EventList selectedEvents;
    /**
     * A list of events for which the entrant was on the waitlist but was not selected.
     */
    private EventList notSelectedEvents;
    /**
     * A list of events for which the entrant declined an invitation.
     */
    private EventList declinedEvents;
    /**
     * A list of events in which the entrant is officially enrolled.
     */
    private EventList enrolledEvents;

    /**
     * Constructs an Entrant object.
     * @param userName The userName of the entrant.
     * @param userInfo The UserInfo object for the entrant.
     */
    public Entrant(String userName, UserInfo userInfo) {
        super(userName, userInfo);
    }

    /**
     * Getter method for the waitlisted events list.
     * @return The EventList object for waitlisted events.
     */
    public EventList getWaitListedEvents() {
        return waitListedEvents;
    }

    /**
     * Getter method for the selected events list.
     * @return The EventList object for selected events.
     */
    public EventList getselectedEvents() {
        return selectedEvents;
    }

    /**
     * Getter method for the not selected events list.
     * @return The EventList object for not selected events.
     */
    public EventList getnotSelectedEvents() {
        return notSelectedEvents;
    }

    /**
     * Getter method for the declined events list.
     * @return The EventList object for declined events.
     */
    public EventList getDeclinedEvents() {
        return declinedEvents;
    }

    /**
     * Getter method for the enrolled events list.
     * @return The EventList object for enrolled events.
     */
    public EventList getEnrolledEvents() {
        return enrolledEvents;
    }

    /**
     * Setter method for the waitlisted events list.
     * @param waitListedEvents The new EventList to be set.
     */
    public void setWaitListedEvents(EventList waitListedEvents) {
        this.waitListedEvents = waitListedEvents;
    }

    /**
     * Setter method for the selected events list.
     * @param selectedEvents The new EventList to be set.
     */
    public void setselectedEvents(EventList selectedEvents) {
        this.selectedEvents = selectedEvents;
    }

    /**
     * Setter method for the not selected events list.
     * @param notSelectedEvents The new EventList to be set.
     */
    public void setnotSelectedEvents(EventList notSelectedEvents) {
        this.notSelectedEvents = notSelectedEvents;
    }

    /**
     * Setter method for the declined events list.
     * @param declinedEvents The new EventList to set.
     */
    public void setDeclinedEvents(EventList declinedEvents) {
        this.declinedEvents = declinedEvents;
    }

    /**
     * Setter method for the enrolled events list.
     * @param enrolledEvents The new EventList to be set.
     */
    public void setEnrolledEvents(EventList enrolledEvents) {
        this.enrolledEvents = enrolledEvents;
    }
}
