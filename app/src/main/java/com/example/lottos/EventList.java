package com.example.lottos;

import java.util.ArrayList;

public class EventList {
    private ArrayList<Event> eventList = new ArrayList<Event>();

    /**
     * Getter method for event list
     * @return The list of events
     */
    public ArrayList<Event> getEventList() {
        return eventList;
    }

    /**
     * Method to add event to event list
     * @param event The event to add to the event list
     */
    public void addEvent(Event event) {
        eventList.add(event);
    }

    /**
     * Method to remove event from event list
     * @param event The event to remove from the event list
     */
    public void removeEvent(Event event) {
        eventList.remove(event);
    }
}
