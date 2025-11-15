package com.example.lottos.entities;

import java.time.LocalDate;
import java.time.LocalTime;

public class EventTime {
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private String daysRunning;

    /**
     * Constructs an EventTime object
     * @param startDate The starting date of the event
     * @param startTime The starting time of the event
     * @param endDate The ending date of the event
     * @param endTime The ending time of the event
     * @param daysRunning The days of the week the event is on eg.,"SuMTWRFS" for all days
     */
    public EventTime(LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime, String daysRunning) {
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.daysRunning = daysRunning;
    }

    /**
     * Getter method for start date
     * @return The date the event starts
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
     */
    public String getDaysRunning() {
        return daysRunning;
    }

    /**
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
     * Setter method for days running
     * @param daysRunning The new days running to be set
     */
    public void setDaysRunning(String daysRunning) {
        this.daysRunning = daysRunning;
    }
}
