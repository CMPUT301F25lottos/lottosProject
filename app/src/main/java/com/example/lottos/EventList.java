package com.example.lottos;

import java.util.ArrayList;

/**
 * This is a class that keeps a list of Event objects
 */
public class EventList {
    private ArrayList<Event> events = new ArrayList<Event>();

    /**
     * Getter method for event list
     * @return The list of events
     */
    public ArrayList<Event> getEvents() {
        return events;
    }

    /**
     * Method to add event to event list
     * @param event The event to add to the event list
     */
    public void addEvent(Event event) {
        events.add(event);
    }

    /**
     * Method to remove event from event list
     * @param event The event to remove from the event list
     */
    public void removeEvent(Event event) {
        events.remove(event);
    }


}
