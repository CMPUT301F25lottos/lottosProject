package com.example.lottos.entities;

/**
 * A data class that encapsulates detailed information about an event.
 * Role: This class is used to group together properties of an event such as its
 * description, location, and participant capacity. It acts as a simple data container (POJO)
 * with constructors, getters, and setters for these properties.
 */
public class EventInfo {
    /**
     * A detailed description of the event.
     */
    private String description;

    /**
     * The physical location or address where the event will take place.
     */
    private String location;
    /**
     * The maximum number of participants that can be selected or enrolled in the event.
     */
    private int selectionCap;

    /**
     * Constructs an EventInfo object.
     * @param description The description of the event.
     * @param location The location of the event.
     * @param selectionCap The maximum number of entrants to enroll.
     */
    public EventInfo(String description, String location, int selectionCap) {
        this.description = description;
        this.location = location;
        this.selectionCap = selectionCap;
    }

    /**
     * Getter method for the event description.
     * @return The description of the event.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Getter method for the event location.
     * @return The location of the event.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Getter method for the event's selection capacity.
     * @return The maximum number of entrants to enroll.
     */
    public int getSelectionCap() {
        return selectionCap;
    }

    /**
     * Setter method for the event description.
     * @param description The new description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Setter method for the event location.
     * @param location The new location to set.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Setter method for the event's selection capacity.
     * @param selectionCap The new selection cap to set.
     */
    public void setSelectionCap(int selectionCap) {
        this.selectionCap = selectionCap;
    }
}
