package com.example.lottos.admin;

// This is a simple data holder class for our adapter.
public class UserItem {
    private final String userId;
    private final String userName;
    private int eventsParticipated;
    private int eventsCreated;

    public UserItem(String userId, String userName) {
        this.userId = userId;
        this.userName = userName;
        this.eventsParticipated = 0;
        this.eventsCreated = 0;
    }

    // --- Getters ---
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public int getEventsParticipated() { return eventsParticipated; }
    public int getEventsCreated() { return eventsCreated; }


    public void setEventsParticipated(int count) { this.eventsParticipated = count; }
    public void setEventsCreated(int count) { this.eventsCreated = count; }
}
