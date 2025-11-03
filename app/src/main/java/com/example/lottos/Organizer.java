package com.example.lottos;

import java.util.ArrayList;

public class Organizer extends User{
    private String userName;
    private UserInfo userInfo;
    private EventList openEvents = new EventList();
    private EventList closedEvents = new EventList();

    /**
     * Constructs an Organizer object
     * @param userName The username of the organizer
     * @param userInfo The UserInfo object for the Organizer
     */
    public Organizer(String userName, UserInfo userInfo) {
        super(userName, userInfo);
    }

    /**
     * Getter method for open events list
     * @return The EventList object for open Events
     */
    public EventList getOpenEvents() {
        return openEvents;
    }

    /**
     * Getter method for closed events list
     * @return The EventList object for closed events
     */
    public EventList getClosedEvents() {
        return closedEvents;
    }

    /**
     * Setter method for open events list
     * @param openEvents The new EventList object to set
     */
    public void setOpenEvents(EventList openEvents) {
        this.openEvents = openEvents;
    }

    /**
     * Setter method for closed events list
     * @param closedEvents The new EventList object to be set
     */
    public void setClosedEvents(EventList closedEvents) {
        this.closedEvents = closedEvents;
    }
}
