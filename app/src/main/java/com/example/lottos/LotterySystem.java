package com.example.lottos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LotterySystem {
    private String seed;//can just be event name or other number
    public LotterySystem(String eventName)
    { this.seed = eventName + "_" + System.currentTimeMillis(); }
    private ArrayList<String> DeterministicOrder(ArrayList<String> src, String seed) {
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
        for (int i = 0; i < Keyorder.size(); i++){
            Output.add(keyMap.get(Keyorder.get(i)));
        }
        return Output;

    }
    public ArrayList<String> Selected(ArrayList<String> waitList, ArrayList<String> selectedList, int targetCount){
        ArrayList<String> LocalselectedList=selectedList;
        ArrayList<String> LocalWaitList=waitList;
        if (LocalselectedList.size() >= targetCount) return LocalselectedList;
        ArrayList<String> order = DeterministicOrder(LocalWaitList, seed);
        for(int i = 0; i < targetCount; i++){
            if (LocalselectedList.size() >= targetCount) break;
            if (!LocalselectedList.contains(order.get(i))){
                LocalselectedList.add(order.get(i));
            }

        }
        return LocalselectedList;
    }
}
