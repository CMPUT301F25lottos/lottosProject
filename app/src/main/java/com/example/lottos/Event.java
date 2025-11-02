package com.example.lottos;

import java.sql.Array;
import java.util.ArrayList;

public class Event {
    private String eventName;
    private Organizer organizer;
    private EventTime eventTime;
    private EventInfo eventInfo;
    private WaitList waitList;
    private ArrayList<Entrant> selectedList = new ArrayList<Entrant>();
    private ArrayList<Entrant> cancelledList = new ArrayList<Entrant>();
    private ArrayList<Entrant> enrolledList = new ArrayList<Entrant>();

    /**
     * Constructs an Event Object
     * @param eventName The name of the event
     * @param organizer The organizer of the event
     * @param eventTime The EventTime object for the event
     * @param eventInfo The EventInfo object for the event
     */
    public Event(String eventName, Organizer organizer, EventTime eventTime, EventInfo eventInfo, WaitList waitList) {
        this.eventName = eventName;
        this.organizer = organizer;
        this.eventTime = eventTime;
        this.eventInfo = eventInfo;
        this.waitList = waitList;
    }

    /**
     * Getter method for event name
     * @return The name of the event
     */
    public String getEventName() {
        return eventName;
    }

    /**
     * Getter method for organizer
     * @return The organizer of the event
     */
    public Organizer getOrganizer() {
        return organizer;
    }

    /**
     * Getter method for event time
     * @return The EventTime object for the event
     */
    public EventTime getEventTime() {
        return eventTime;
    }

    /**
     * Getter method for event info
     * @return The EventInfo object for the event
     */
    public EventInfo getEventInfo() {
        return eventInfo;
    }

    /**
     * Getter method for event waitlist
     * @return The WaitList object for the event
     */
    public WaitList getWaitList() {
        return waitList;
    }

    /**
     * Getter method for the selected list
     * @return The list of entrants selected for the event
     */
    public ArrayList<Entrant> getSelectedList() {
        return selectedList;
    }

    /**
     * Getter method for the cancelled list
     * @return The list of entrants cancelled for the event
     */
    public ArrayList<Entrant> getCancelledList() {
        return cancelledList;
    }

    /**
     * Getter method for the enrolled list
     * @return The list of entrants enrolled in the event
     */
    public ArrayList<Entrant> getEnrolledList() {
        return enrolledList;
    }

    /**
     * Setter method for the event name
     * @param eventName The new event name to set
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Setter method for the organizer
     * @param organizer The new organizer to set
     */
    public void setOrganizer(Organizer organizer) {
        this.organizer = organizer;
    }

    /**
     * Setter method for event time
     * @param eventTime The new EventTime object to set
     */
    public void setEventTime(EventTime eventTime) {
        this.eventTime = eventTime;
    }

    /**
     * Setter method for event info
     * @param eventInfo The EventInfo object to set
     */
    public void setEventInfo(EventInfo eventInfo) {
        this.eventInfo = eventInfo;
    }

    /**
     * Setter method for waitlist
     * @param waitList The new WaitList object to set
     */
    public void setWaitList(WaitList waitList) {
        this.waitList = waitList;
    }

    /**
     * Method to add entrant to selected list
     * @param entrant The entrant to add to selected list
     */
    public void addSelectedEntrant(Entrant entrant) {
        selectedList.add(entrant);
    }

    /**
     * Method to remove entrant from selected list
     * @param entrant The entrant to remove from selected list
     */
    public void removeSelectedEntrant(Entrant entrant) {
        selectedList.remove(entrant);
    }

    /**
     * Method to add entrant to cancelled list
     * @param entrant The entrant to add to cancelled list
     */
    public void addCancelledEntrant(Entrant entrant) {
        cancelledList.add(entrant);
    }

    /**
     * Method to remove entrant from cancelled list
     * @param entrant The entrant to remove from cancelled list
     */
    public void removeCancelledEntrant(Entrant entrant) {
        cancelledList.remove(entrant);
    }

    /**
     * Method to add entrant to enrolled list
     * @param entrant The entrant to add to enrolled list
     */
    public void addEnrolledEntrant(Entrant entrant) {
        enrolledList.add(entrant);
    }

    /**
     * Method to remove entrant from enrolled list
     * @param entrant The entrant to remove from enrolled list
     */
    public void removeEnrolledEntrant(Entrant entrant) {
        enrolledList.remove(entrant);
    }

}
