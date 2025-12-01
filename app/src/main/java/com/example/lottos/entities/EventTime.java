package com.example.lottos.entities;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * A data class that encapsulates the scheduling details of an event.
 * Role: This class holds the start and end dates and times for an event,
 * as well as a string representing the specific days of the week on which
 * the event occurs. It serves as a structured container for time-related
 * properties of an event.
 */
public class EventTime {
    /**
     * The starting date of the event.
     */
    private LocalDate startDate;
    /**
     * The starting time of the event on its start date.
     */
    private LocalTime startTime;
    /**
     * The ending date of the event.
     */
    private LocalDate endDate;
    /**
     * The ending time of the event on its end date.
     */
    private LocalTime endTime;
    /**
     * A string representing the days of the week the event runs.
     * Example: "SuMTWRFS" for all days of the week.
     */
    private String daysRunning;

    /**
     * Constructs an EventTime object with specified scheduling details.
     * @param startDate The starting date of the event.
     * @param startTime The starting time of the event.
     * @param endDate The ending date of the event.
     * @param endTime The ending time of the event.
     * @param daysRunning The days of the week the event is on, e.g., "SuMTWRFS" for all days.
     */
    public EventTime(LocalDate startDate, LocalTime startTime, LocalDate endDate, LocalTime endTime, String daysRunning) {
        this.startDate = startDate;
        this.startTime = startTime;
        this.endDate = endDate;
        this.endTime = endTime;
        this.daysRunning = daysRunning;
    }

    /**
     * Getter method for the start date.
     * @return The LocalDate object representing the date the event starts.
     */
    public LocalDate getStartDate() {
        return startDate;
    }

    /**
     * Getter method for the start time.
     * @return The LocalTime object representing the time the event starts.
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Getter method for the end date.
     * @return The LocalDate object representing the date the event ends.
     */
    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * Getter method for the end time.
     * @return The LocalTime object representing the time the event ends.
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Getter method for the days the event runs.
     * @return A string representing the days of the week the event will run, e.g., "SuMTWRFS" for all days.
     */
    public String getDaysRunning() {
        return daysRunning;
    }

    /**
     * Setter method for the start date.
     * @param startDate The new start date to be set.
     */
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    /**
     * Setter method for the start time.
     * @param startTime The new start time to be set.
     */
    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Setter method for the end date.
     * @param endDate The new end date to be set.
     */
    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    /**
     * Setter method for the end time.
     * @param endTime The new end time to be set.
     */
    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    /**
     * Setter method for the days running.
     * @param daysRunning The new string representing the days running to be set.
     */
    public void setDaysRunning(String daysRunning) {
        this.daysRunning = daysRunning;
    }
}
