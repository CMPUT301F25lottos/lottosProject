package com.example.lottos.lottery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class that performs lottery selection based on a string seed.
 *
 * Role: logic class for randomization and participant ordering.
 * Generates a list of selected participants
 * by hashing each participant’s name with an event-specific seed (usually
 * the event name), sorting the hash keys and returning the ordered list.
 * This ensures fairness and repeatability without relying on true randomness.
 */

public class LotterySystem {
    private String seed;
    public LotterySystem(String eventName)
    { this.seed = eventName; }
    public ArrayList<String> Selected(ArrayList<String> src) {
        ArrayList<String> order = new ArrayList<String>(src);
        ArrayList<String> Keyorder = new ArrayList<String>();
        ArrayList<String> Output = new ArrayList<String>();
        Map<String, String> keyMap = new HashMap<>();
        for (int i = 0; i < order.size(); i++){
            String s = order.get(i);
            String key = (s+seed).hashCode()+ "";
            Keyorder.add(key);
            keyMap.put(key, s);
        }
        Collections.sort(Keyorder);
        for (int i = 0; i < Keyorder.size(); i++)
        { Output.add(keyMap.get(Keyorder.get(i))); }
        return Output;


    }
}
