package com.example.lottos.entities;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an event within the Lottos application.
 *
 * Role:
 * Stores and manages all event-related data such as organizer, timing, location,
 * registration limits, and participant lists (waitlist, selected, cancelled, enrolled).
 * This class encapsulates the event’s state and provides getter/setter methods
 * to ensure structured access to event properties.
 */

public class Event {
    private String eventName;
    private String EventId;
    private String organizer;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    private String description;

    private String location;
    private int selectionCap;
    private LocalDateTime EndRegisterTime;
    private boolean isOpen;

    private UserList waitList;
    private UserList selectedList;
    private int currentPointer;
    private UserList enrolledList;
    private UserList cancelledList;


    public Event(String eventName, String organizer,
                 LocalDateTime startTime, LocalDateTime endTime,
                 String description, String location,
                 int selectionCap, LocalDateTime EndRegisterTime) {
        this.EventId = UUID.randomUUID().toString();
        this.eventName = eventName;
        this.organizer = organizer;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        // this place should have a photo URL or something
        this.location = location;
        this.selectionCap = selectionCap;
        this.waitList = new UserList();
        this.selectedList = new UserList();
        this.cancelledList = new UserList();
        this.enrolledList = new UserList();
        this.isOpen=true;
        this.EndRegisterTime=EndRegisterTime;
    }
    public boolean isOrganizer() {
        FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
        return cur != null && Objects.equals(cur.getUid(), organizer);
    }


    public String getEventName() {
        return eventName;
    }
    public boolean getIsOpen() {
        return isOpen;
    }
    public String getEventId() {
        return EventId;
    }

    /**
     * Getter method for organizer
     * @return The organizer of the event
     */
    public String getOrganizer() {
        return organizer;
    }


    /**
     * Getter method for start time
     * @return The time the event starts
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }



    /**
     * Getter method for end time
     * @return The time the event ends
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }


    /**
     * Setter method for start time
     * @param startTime The new start time to be set
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }



    /**
     * Setter method for end time
     * @param endTime The new end time to be set
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }


    /**
     * Getter method for event info
     * @return The EventInfo object for the event
     */

    public LocalDateTime getEndRegisterTime() {
        return EndRegisterTime;
    }



    public void setEndRegisterTime(LocalDateTime EndRegisterTime) {
        this.EndRegisterTime = EndRegisterTime;
    }


    /**
     * Getter method for event waitlist
     * @return The WaitList object for the event
     */
    public UserList getWaitList() {
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
    public void setWaitList(UserList waitList) {
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
