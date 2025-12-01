package com.example.lottos.admin;

/**
 * A data model class representing a single user for display in administrative lists.
 * This class encapsulates a user's identity (ID and name) and tracks statistics
 * such as the number of events they have participated in and created.
 */
public class UserItem {
    /**
     * The unique identifier for the user, typically from Firebase Authentication.
     */
    private final String userId;

    /**
     * The display name or username of the user.
     */
    private final String userName;

    /**
     * The count of events the user has participated in or joined.
     */
    private int eventsParticipated;

    /**
     * The count of events the user has created or organized.
     */
    private int eventsCreated;

    /**
     * Constructs a new UserItem object with initial counts set to zero.
     *
     * @param userId   The unique identifier for the user.
     * @param userName The display name for the user.
     */
    public UserItem(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.eventsParticipated = 0;
        this.eventsCreated = 0;
    }

    /**
     * Gets the unique identifier of the user.
     * @return The user's ID.
     */
    public String getUserId() { return userId; }

    /**
     * Gets the display name of the user.
     * @return The user's name.
     */
    public String getUserName() { return userName; }

    /**
     * Gets the number of events the user has participated in.
     * @return The count of participated events.
     */
    public int getEventsParticipated() { return eventsParticipated; }

    /**
     * Gets the number of events the user has created.
     * @return The count of created events.
     */
    public int getEventsCreated() { return eventsCreated; }

    /**
     * Sets the number of events the user has participated in.
     * @param count The new count of participated events.
     */
    public void setEventsParticipated(int count) { this.eventsParticipated = count; }

    /**
     * Sets the number of events the user has created.
     * @param count The new count of created events.
     */
    public void setEventsCreated(int count) { this.eventsCreated = count; }
}
