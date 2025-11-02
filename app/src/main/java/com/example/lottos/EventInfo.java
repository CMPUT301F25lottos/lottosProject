package com.example.lottos;

public class EventInfo {
    private String description;
    private int selectionCap;

    /**
     * Constructs an EventInfo object
     * @param description The description of the event
     * @param selectionCap The maximum number of entrants to enroll
     */
    public EventInfo(String description, int selectionCap) {
        this.description = description;
        this.selectionCap = selectionCap;
    }

    /**
     * Getter method for description
     * @return The description of the event
     */
    public String getDescription() {
        return description;
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
     * Setter method for selection cap
     * @param selectionCap The new selection cap to set
     */
    public void setSelectionCap(int selectionCap) {
        this.selectionCap = selectionCap;
    }
}
