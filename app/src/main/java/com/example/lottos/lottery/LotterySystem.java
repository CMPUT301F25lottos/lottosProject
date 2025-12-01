package com.example.lottos.lottery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A class that performs a deterministic lottery selection based on a string seed.
 *
 * Role: This is a pure logic class responsible for creating a fair, repeatable, and
 * verifiable ordering of participants for an event. It does not use true randomness.
 * Instead, it generates a unique hash for each participant by combining their name
 * with an event-specific seed (typically the event name). By sorting these hash values,
 * it produces a consistent, shuffled list of participants.
 */
public class LotterySystem {
    /**
     * The event-specific string used to generate unique hashes for participants.
     */
    private String seed;

    /**
     * Constructs a LotterySystem instance for a specific event.
     * @param eventName The seed for the lottery, which should be a unique identifier
     *                  for the event (like its name) to ensure a unique ordering.
     */
    public LotterySystem(String eventName) {
        this.seed = eventName;
    }

    /**
     * Takes a list of participant names and returns them in a new, deterministically
     * shuffled order.
     *
     * The process is as follows:
     * 1. For each participant, create a unique key by hashing their name combined with the event seed.
     * 2. Store a mapping of this key back to the original participant name.
     * 3. Sort the list of generated keys alphabetically/numerically.
     * 4. Build the final output list by retrieving the participant names in the new sorted key order.
     *
     * @param src The source ArrayList of participant usernames to be ordered.
     * @return A new ArrayList containing the same participant usernames in a shuffled,
     *         deterministic order.
     */
    public ArrayList<String> Selected(ArrayList<String> src) {
        // Create a mutable copy of the source list.
        ArrayList<String> order = new ArrayList<String>(src);
        // This will store the generated hash keys.
        ArrayList<String> Keyorder = new ArrayList<String>();
        // This will be the final, ordered list of names.
        ArrayList<String> Output = new ArrayList<String>();
        // Maps the generated hash key back to the original username.
        Map<String, String> keyMap = new HashMap<>();

        // Generate a unique hash key for each participant.
        for (int i = 0; i < order.size(); i++) {
            String s = order.get(i);
            // Combine username and seed, get hash code, and convert to a String key.
            String key = (s + seed).hashCode() + "";
            Keyorder.add(key);
            keyMap.put(key, s);
        }

        // Sort the hash keys. This is the core of the deterministic shuffle.
        Collections.sort(Keyorder);

        // Reconstruct the list of names based on the sorted key order.
        for (int i = 0; i < Keyorder.size(); i++) {
            Output.add(keyMap.get(Keyorder.get(i)));
        }

        // Return the final, ordered list.
        return Output;
    }
}

