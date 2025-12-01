package com.example.lottos.entities;

import java.util.ArrayList;

/**
 * Represents a collection of events, where each event is identified by its name or ID.
 * Role: This class acts as a wrapper around an {@code ArrayList<String>} to manage
 * a list of event identifiers. It provides simple methods to add, remove, and retrieve
 * the events in the list. This is typically used within User or Entrant objects
 * to track their association with various events (e.g., enrolledEvents, organizedEvents).
 */
public class EventList {
    /**
     * The underlying list that stores the names or unique identifiers of the events.
     */
    private ArrayList<String> events = new ArrayList<String>();

    /**
     * Gets the complete list of event identifiers.
     * @return An {@code ArrayList<String>} containing the names or IDs of the events.
     */
    public ArrayList<String> getEvents() {
        return events;
    }

    /**
     * Adds an event's identifier to the list.
     * @param eventName The name or unique ID of the event to add.
     */
    public void addEvent(String eventName) {
        events.add(eventName);
    }

    /**
     * Removes an event's identifier from the list.
     * If the specified event name is not in the list, this method does nothing.
     * @param eventName The name or unique ID of the event to remove.
     */
    public void removeEvent(String eventName) {
        events.remove(eventName);
    }


}
