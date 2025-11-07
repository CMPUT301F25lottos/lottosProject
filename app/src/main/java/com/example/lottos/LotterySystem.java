package com.example.lottos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LotterySystem {
    private String seed;//can just be event name or other number
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
