package com.example.lottos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LotterySystem {
    private String seed;//can just be event name or other number
    public LotterySystem(String seed)
    { this.seed = seed; }
    private ArrayList<String> DeterministicOrder(ArrayList<String> src, String seed) {
        ArrayList<String> order = new ArrayList<String>(src);
        ArrayList<String> Keyorder = new ArrayList<String>;
        ArrayList<String> Output = new ArrayList<String>;
        Map<Long, String> keyMap = new HashMap<>();
        for (int i = 0; i < order.size(); i++){
            String s = order.get(i)+seed;
            long key = s.hashCode();
            Keyorder.add(key);
            keyMap.put(key, s);
        }
        Arrays.sort(Keyorder);
        for (int i = 0; i < Keyorder.size(); i++){
            Output.add(keyMap.get(Keyorder.get(i)));
        }
        return Output;

    }
    public ArrayList<String> Selected(WaitList waitList, UserList selectedList, int targetCount){
        ArrayList<String> LocalselectedList=selectedList.getUsers();
        ArrayList<String> LocalWaitList=waitList.getEntrants().getUsers();
        if (LocalselectedList.size() >= targetCount) return LocalselectedList;
        ArrayList<String> order = DeterministicOrder(LocalWaitList, seed);
        for(int i = 0; i < targetCount; i++){
            if (LocalselectedList.size() >= targetCount) break;
            if (!LocalselectedList.contains(order[i])){
                LocalselectedList.add(order[i]);
            }

        }
        return LocalselectedList;
    }
}
