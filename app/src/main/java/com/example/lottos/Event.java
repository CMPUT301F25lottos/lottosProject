package com.example.lottos;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Objects;

public class Event {
    private String eventName;
    private String organizer;
    private EventTime eventTime;
    private EventInfo eventInfo;
    private WaitList waitList;
    private UserList selectedList;
    private UserList cancelledList;
    private UserList enrolledList;

    /**
     * Constructs an Event Object
     * @param eventName The name of the event
     * @param organizer The organizer of the event
     * @param eventTime The EventTime object for the event
     * @param eventInfo The EventInfo object for the event
     */
    public Event(String eventName, String organizer, EventTime eventTime, EventInfo eventInfo, WaitList waitList) {
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
    public String getOrganizer() {
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
    public UserList getSelectedList() {
        return selectedList;
    }

    /**
     * Getter method for the cancelled list
     * @return The list of entrants cancelled for the event
     */
    public UserList getCancelledList() {
        return cancelledList;
    }

    /**
     * Getter method for the enrolled list
     * @return The list of entrants enrolled in the event
     */
    public UserList getEnrolledList() {
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
    public void setOrganizer(String  organizer) {
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
     * Setter method for selected list
     * @param selectedList The new UserList object to be set
     */
    public void setSelectedList(UserList selectedList) {
        this.selectedList = selectedList;
    }

    /**
     * Setter method for cancelled list
     * @param cancelledList The new UserList object to be set
     */
    public void setCancelledList(UserList cancelledList) {
        this.cancelledList = cancelledList;
    }

    /**
     * Setter method for enrolled list
     * @param enrolledList The new UserList object to be set
     */
    public void setEnrolledList(UserList enrolledList) {
        this.enrolledList = enrolledList;
    }

    @Override
    public boolean equals(Object event) {
        if (this == event) {
            return true;
        }
        if (event == null || (getClass() != event.getClass())) {
            return false;
        }
        else {
            Event check = (Event) event;
            return (eventName.equals(check.getEventName()));
        }
    }

    @Override
    public int hashCode() {
        return eventName.hashCode();
    }
}
