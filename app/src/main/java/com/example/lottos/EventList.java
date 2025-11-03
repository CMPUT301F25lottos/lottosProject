package com.example.lottos;

import java.util.ArrayList;

/**
 * This is a class that keeps a list of Event objects
 */
public class EventList {
    private ArrayList<String> events = new ArrayList<String>();

    /**
     * Getter method for event list
     * @return The list of events
     */
    public ArrayList<String> getEvents() {
        return events;
    }

    /**
     * Method to add event to event list
     * @param eventName The event to add to the event list
     */
    public void addEvent(String eventName) {
        events.add(eventName);
    }

    /**
     * Method to remove event from event list
     * @param eventName The event to remove from the event list
     */
    public void removeEvent(String eventName) {
        events.remove(eventName);
    }


}
