package com.example.lottos;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

public class WaitList {
    private LocalDate closeDate;
    private LocalTime closeTime;
    private int waitListCap;
    private UserList entrants;

    /**
     * Constructor for WaitList object
     * @param closeDate The date the waitlist closes
     * @param closeTime The time the waitlist closes
     * @param waitListCap The optional cap for entrants on the waitlist
     */
    public WaitList(LocalDate closeDate, LocalTime closeTime, int waitListCap) {
        this.closeDate = closeDate;
        this.closeTime = closeTime;
        this.waitListCap = waitListCap;
    }

    /**
     * Getter method for close date
     * @return The date the waitlist closes
     */
    public LocalDate getCloseDate() {
        return closeDate;
    }

    /**
     * Getter method for close time
     * @return The time the waitlist closes
     */
    public LocalTime getCloseTime() {
        return closeTime;
    }

    /**
     * Getter method for waitlist cap
     * @return The optional cap for entrants joining the waitlist, if 0 there is no cap
     */
    public int getWaitListCap() {
        return waitListCap;
    }

    /**
     * Getter method for entrants arraylist
     * @return The arraylist of entrants on the waitlist
     */
    public UserList getEntrants() {
        return entrants;
    }

    /**
     * Setter method for close date
     * @param closeDate The new close date to be set
     */
    public void setCloseDate(LocalDate closeDate) {
        this.closeDate = closeDate;
    }

    /**
     * Setter method for close time
     * @param closeTime The new close time to be set
     */
    public void setCloseTime(LocalTime closeTime) {
        this.closeTime = closeTime;
    }

    /**
     * Setter method for waitlist cap
     * @param waitListCap The new waitlist cap to be set
     */
    public void setWaitListCap(int waitListCap) {
        this.waitListCap = waitListCap;
    }

    /**
     * Setter method for entrants list
     * @param entrants The new UserList object to be set
     */
    public void setEntrants(UserList entrants) {
        this.entrants = entrants;
    }
}
