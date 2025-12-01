package com.example.lottos.entities;

/**
 * Represents a user who creates and manages events, known as an Organizer.
 * This class extends the base {@code User} class and adds specific event lists
 * to track the events this user has organized, categorized into 'open' and 'closed' states.
 */
public class Organizer extends User {
    /**
     * The unique username of the organizer. This field shadows the one in the parent class and is initialized through the constructor.
     */
    private String userName;
    /**
     * The personal information of the organizer. This field shadows the one in the parent class and is initialized through the constructor.
     */
    private UserInfo userInfo;
    /**
     * A list of events created by this organizer that are currently open for registration or are ongoing.
     */
    private EventList openEvents = new EventList();
    /**
     * A list of events created by this organizer that have concluded or been manually closed.
     */
    private EventList closedEvents = new EventList();

    /**
     * Constructs an Organizer object.
     * @param userName The username of the organizer.
     * @param userInfo The UserInfo object for the Organizer.
     */
    public Organizer(String userName, UserInfo userInfo) {
        super(userName, userInfo);
    }

    /**
     * Getter method for the list of open events.
     * @return The EventList object for events that are currently open.
     */
    public EventList getOpenEvents() {
        return openEvents;
    }

    /**
     * Getter method for the list of closed events.
     * @return The EventList object for events that have been closed.
     */
    public EventList getClosedEvents() {
        return closedEvents;
    }

    /**
     * Setter method for the list of open events.
     * @param openEvents The new EventList object to set for open events.
     */
    public void setOpenEvents(EventList openEvents) {
        this.openEvents = openEvents;
    }

    /**
     * Setter method for the list of closed events.
     * @param closedEvents The new EventList object to be set for closed events.
     */
    public void setClosedEvents(EventList closedEvents) {
        this.closedEvents = closedEvents;
    }
}
