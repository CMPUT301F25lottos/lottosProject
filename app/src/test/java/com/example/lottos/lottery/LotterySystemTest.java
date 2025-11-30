package com.example.lottos.lottery;

import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Unit tests for the LotterySystem class.
 * Verifies that the lottery selection is deterministic and fair based on its hash-and-sort logic.
 */
public class LotterySystemTest {

    private ArrayList<String> participants;

    @Before
    public void setUp() {
        participants = new ArrayList<>(Arrays.asList("Alice", "Bob", "Charlie", "David"));
    }

    @Test
    public void selected_shouldReturnAllOriginalParticipants() {
        LotterySystem lottery = new LotterySystem("My-Awesome-Event");

        ArrayList<String> selected = lottery.Selected(new ArrayList<>(participants));

        assertEquals("Output list size should match input list size", participants.size(), selected.size());

        assertTrue("Output list should contain all original participants", new HashSet<>(selected).containsAll(participants));
    }

    @Test
    public void selected_shouldBeDeterministicForSameSeed() {
        String seed = "Project-Omega-2025";
        LotterySystem lottery1 = new LotterySystem(seed);
        LotterySystem lottery2 = new LotterySystem(seed);

        ArrayList<String> result1 = lottery1.Selected(new ArrayList<>(participants));
        ArrayList<String> result2 = lottery2.Selected(new ArrayList<>(participants));

        assertEquals("Results should be identical for the same seed", result1, result2);
    }

    @Test
    public void selected_withEmptyParticipantList_shouldReturnEmptyList() {

        LotterySystem lottery = new LotterySystem("Empty-Event");
        ArrayList<String> emptyList = new ArrayList<>();

        ArrayList<String> selected = lottery.Selected(emptyList);

        assertNotNull("Output should not be null", selected);
        assertTrue("Output should be an empty list", selected.isEmpty());
    }

    @Test
    public void selected_withSingleParticipant_shouldReturnSingleParticipant() {

        LotterySystem lottery = new LotterySystem("Solo-Event");
        ArrayList<String> singleParticipant = new ArrayList<>(List.of("Eve"));

        ArrayList<String> selected = lottery.Selected(singleParticipant);

        assertEquals("Output list should have one participant", 1, selected.size());
        assertEquals("The single participant should be returned", "Eve", selected.get(0));
    }
}
