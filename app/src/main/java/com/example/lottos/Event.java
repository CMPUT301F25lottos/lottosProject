package com.example.lottos;

import java.sql.Array;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Objects;

public class Event {
    private String eventName;
    private String organizer;
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;

    private String description;

    private String location;
    private int selectionCap;
    private WaitList waitList;
    private UserList selectedList;
    private UserList cancelledList;
    private UserList enrolledList;

    /**
     * Constructs an Event Object
     * @param eventName The name of the event
     * @param organizer The organizer of the event

     */
    public Event(String eventName, String organizer,
                 LocalDate startDate, LocalTime startTime,
                 LocalDate endDate, LocalTime endTime,
                 String description, String location,
                 int selectionCap) {

        this.eventName = eventName;
        this.organizer = organizer;
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.description = description;
        this.location = location;
        this.selectionCap = selectionCap;
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
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Getter method for start time
     * @return The time the event starts
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Getter method for end date
     * @return The date the event ends
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Getter method for end time
     * @return The time the event ends
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Getter method for the days the event runs
     * @return String representing the days of the week the event will run eg.,"SuMTWRFS" for all days

    public String getDaysRunning() {
        return daysRunning;
    }

     * Setter method for start date
     * @param startDate The new start date to be set
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Setter method for start time
     * @param startTime The new start time to be set
     */
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Setter method for end date
     * @param endDate The new end date to be set
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Setter method for end time
     * @param endTime The new end time to be set
     */
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }


    /**
     * Getter method for event info
     * @return The EventInfo object for the event
     */


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

    public String getDescription() {
        return description;
    }

    /**
     * Getter method for location
     * @return The location of the event
     */
    public String getLocation() {
        return location;
    }

    /**
     * Getter method for selection cap
     * @return The maximum number of entrants to enroll
     */
    public int getSelectionCap() {
        return selectionCap;
    }

    /**
     * Setter method for description
     * @param description The new description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Setter method for location
     * @param location The new location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Setter method for selection cap
     * @param selectionCap The new selection cap to set
     */
    public void setSelectionCap(int selectionCap) {
        this.selectionCap = selectionCap;
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
