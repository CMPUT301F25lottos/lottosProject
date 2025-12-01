package com.example.lottos.entities;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDateTime;
import java.util.List;
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
    /**
     * The name of the event.
     */
    private String eventName;
    /**
     * A unique identifier for the event.
     */
    private String EventId;
    /**
     * The username of the user who created the event.
     */
    private String organizer;
    /**
     * The date and time when the event starts.
     */
    private LocalDateTime startTime;
    /**
     * The date and time when the event ends.
     */
    private LocalDateTime endTime;

    /**
     * A detailed description of the event.
     */
    private String description;

    /**
     * The physical location or address of the event.
     */
    private String location;
    /**
     * The maximum number of participants that can be selected from the waitlist.
     */
    private int selectionCap;
    /**
     * The deadline for users to register for the event.
     */
    private LocalDateTime EndRegisterTime;
    /**
     * A flag indicating if the event is currently open for registration.
     */
    private boolean isOpen;

    /**
     * A list of users waiting to be selected for the event.
     */
    private UserList waitList;
    /**
     * A list of users who have been selected to attend the event.
     */
    private UserList selectedList;
    /**
     * A pointer used for selection logic, not currently implemented.
     */
    private int currentPointer;
    /**
     * A list of users who have confirmed their attendance and are enrolled in the event.
     */
    private UserList enrolledList;
    /**
     * A list of users who cancelled their registration or spot.
     */
    private UserList cancelledList;

    /**
     * The URL for the event's promotional poster image.
     */
    private String posterUrl;

    /**
     * A list of keywords used for filtering or categorization, not currently implemented.
     */
    private List<String> filterWords;
    /**
     * A flag indicating whether entrants must provide their location upon registration.
     */
    private boolean geolocationRequired = false;

    /**
     * Getter method for geolocationRequired.
     * @return True if entrants must submit their location when joining.
     */
    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    /**
     * Setter method for geolocationRequired.
     * @param geolocationRequired The new requirement status to set.
     */
    public void setGeolocationRequired(boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
    }


    /**
     * Constructs a new Event object with specified details.
     * It automatically generates a unique ID and initializes participant lists.
     *
     * @param eventName       The name of the event.
     * @param organizer       The username of the organizer.
     * @param startTime       The start date and time of the event.
     * @param endTime         The end date and time of the event.
     * @param description     A description of the event.
     * @param location        The location of the event.
     * @param selectionCap    The maximum number of participants to be selected.
     * @param EndRegisterTime The registration deadline.
     * @param filterWords     A list of keywords for filtering.
     */
    public Event(String eventName,
                 String organizer,
                 LocalDateTime startTime,
                 LocalDateTime endTime,
                 String description,
                 String location,
                 int selectionCap,
                 LocalDateTime EndRegisterTime,
                 List<String> filterWords) {

        this.EventId = UUID.randomUUID().toString();
        this.eventName = eventName;
        this.organizer = organizer;
        this.startTime = startTime;
        this.endTime = endTime;
        this.description = description;
        this.location = location;
        this.selectionCap = selectionCap;
        this.waitList = new UserList();
        this.selectedList = new UserList();
        this.cancelledList = new UserList();
        this.enrolledList = new UserList();
        this.isOpen = true;
        this.EndRegisterTime = EndRegisterTime;
        this.posterUrl = null;
        this.filterWords = filterWords;
    }

    /**
     * Checks if the currently authenticated Firebase user is the organizer of this event.
     *
     * @return true if the current user is the organizer, false otherwise.
     */
    public boolean isOrganizer() {
        FirebaseUser cur = FirebaseAuth.getInstance().getCurrentUser();
        return cur != null && Objects.equals(cur.getUid(), organizer);
    }


    /**
     * Getter for the event name.
     * @return The name of the event.
     */
    public String getEventName() {
        return eventName;
    }
    /**
     * Checks if the event is open for registration.
     * @return true if the event is open, false otherwise.
     */
    public boolean getIsOpen() {
        return isOpen;
    }
    /**
     * Getter for the event's unique ID.
     * @return The unique identifier string for the event.
     */
    public String getEventId() {
        return EventId;
    }

    /**
     * Getter method for organizer.
     * @return The organizer of the event.
     */
    public String getOrganizer() {
        return organizer;
    }


    /**
     * Getter method for start time.
     * @return The time the event starts.
     */
    public LocalDateTime getStartTime() {
        return startTime;
    }



    /**
     * Getter method for end time.
     * @return The time the event ends.
     */
    public LocalDateTime getEndTime() {
        return endTime;
    }


    /**
     * Setter method for start time.
     * @param startTime The new start time to be set.
     */
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }



    /**
     * Setter method for end time.
     * @param endTime The new end time to be set.
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }


    /**
     * Getter method for the registration end time.
     * @return The LocalDateTime when event registration closes.
     */
    public LocalDateTime getEndRegisterTime() {
        return EndRegisterTime;
    }


    /**
     * Setter method for the registration end time.
     * @param EndRegisterTime The new registration end time to set.
     */
    public void setEndRegisterTime(LocalDateTime EndRegisterTime) {
        this.EndRegisterTime = EndRegisterTime;
    }


    /**
     * Getter method for event waitlist.
     * @return The UserList object for the event's waitlist.
     */
    public UserList getWaitList() {
        return waitList;
    }

    /**
     * Getter method for the selected list.
     * @return The list of entrants selected for the event.
     */
    public UserList getSelectedList() {
        return selectedList;
    }

    /**
     * Getter method for the cancelled list.
     * @return The list of entrants who cancelled for the event.
     */
    public UserList getCancelledList() {
        return cancelledList;
    }

    /**
     * Getter method for the enrolled list.
     * @return The list of entrants enrolled in the event.
     */
    public UserList getEnrolledList() {
        return enrolledList;
    }

    /**
     * Setter method for the event name.
     * @param eventName The new event name to set.
     */
    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    /**
     * Setter method for the organizer.
     * @param organizer The new organizer to set.
     */
    public void setOrganizer(String  organizer) {
        this.organizer = organizer;
    }

    /**
     * Getter for the event description.
     * @return The description of the event.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter method for location.
     * @return The location of the event.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Getter method for selection cap.
     * @return The maximum number of entrants to enroll.
     */
    public int getSelectionCap() {
        return selectionCap;
    }

    /**
     * Setter method for description.
     * @param description The new description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Setter method for location.
     * @param location The new location to set.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Setter method for selection cap.
     * @param selectionCap The new selection cap to set.
     */
    public void setSelectionCap(int selectionCap) {
        this.selectionCap = selectionCap;
    }


    /**
     * Setter method for waitlist.
     * @param waitList The new UserList object to set.
     */
    public void setWaitList(UserList waitList) {
        this.waitList = waitList;
    }

    /**
     * Setter method for selected list.
     * @param selectedList The new UserList object to be set.
     */
    public void setSelectedList(UserList selectedList) {
        this.selectedList = selectedList;
    }

    /**
     * Setter method for cancelled list.
     * @param cancelledList The new UserList object to be set.
     */
    public void setCancelledList(UserList cancelledList) {
        this.cancelledList = cancelledList;
    }

    /**
     * Setter method for enrolled list.
     * @param enrolledList The new UserList object to be set.
     */
    public void setEnrolledList(UserList enrolledList) {
        this.enrolledList = enrolledList;
    }

    /**
     * Getter for the event poster URL.
     * @return The URL string of the poster image.
     */
    public String getPosterUrl() {return posterUrl;}

    /**
     * Setter for the event poster URL.
     * @param posterUrl The new URL string for the poster image.
     */
    public void setPosterUrl(String posterUrl) {this.posterUrl = posterUrl;}

    /**
     * Getter for the list of filter words.
     * @return The list of filter word strings.
     */
    public List<String> getFilterWords() {
        return filterWords;
    }

    /**
     * Setter for the list of filter words.
     * @param filterWords The new list of filter words to set.
     */
    public void setFilterWords(List<String> filterWords) {
        this.filterWords = filterWords;
    }


    /**
     * Compares this event to another object for equality.
     * Two events are considered equal if their event names are the same.
     *
     * @param event The object to compare with.
     * @return true if the objects are the same or have the same event name, false otherwise.
     */
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

    /**
     * Generates a hash code for the event.
     * The hash code is based on the event's name.
     *
     * @return The hash code value for this event.
     */
    @Override
    public int hashCode() {
        return eventName.hashCode();
    }


}
