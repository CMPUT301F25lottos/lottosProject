package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentEntrantEventsScreenBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntrantEventsScreen extends Fragment {

    private FragmentEntrantEventsScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;

    private List<EventItem> eventList = new ArrayList<>();
    private List<String> userWaitlistedEvents = new ArrayList<>();

    private EventListAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEntrantEventsScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = EntrantEventsScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        adapter = new EventListAdapter();
        binding.lvEvents.setAdapter(adapter);

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections.actionEntrantEventsScreenToHomeScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections.actionEntrantEventsScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections.actionEntrantEventsScreenToProfileScreen(userName)));

        loadUserWaitlistedEvents();
    }

    /** Load user's waitlisted events */
    private void loadUserWaitlistedEvents() {
        db.collection("users").document(userName).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        Map<String, Object> waitMap = (Map<String, Object>) snapshot.get("waitListedEvents");
                        if (waitMap != null && waitMap.containsKey("events")) {
                            userWaitlistedEvents = (List<String>) waitMap.get("events");
                        }
                    }
                    loadEvents();
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load waitlist", e);
                    loadEvents();
                });
    }

    /** Load all open events */
    private void loadEvents() {
        db.collection("open events").get().addOnSuccessListener(query -> {
            eventList.clear();

            for (QueryDocumentSnapshot doc : query) {
                String eventName = doc.getId();
                boolean isOpen = Boolean.TRUE.equals(doc.getBoolean("IsOpen"));

                EventItem item = new EventItem(eventName, isOpen);
                item.isJoined = userWaitlistedEvents.contains(eventName);

                eventList.add(item);
            }
            eventList.sort((a, b) -> Boolean.compare(!a.isOpen, !b.isOpen));

            adapter.notifyDataSetChanged();
        });
    }

    /** Join waitlist */
    private void joinWaitlist(String eventName, EventItem event, Button button) {
        DocumentReference userRef = db.collection("users").document(userName);
        DocumentReference eventRef = db.collection("open events").document(eventName);

        eventRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            Map<String, Object> waitData = (Map<String, Object>) snapshot.get("waitList");
            List<String> waitUsers = waitData != null && waitData.containsKey("users")
                    ? (List<String>) waitData.get("users")
                    : new ArrayList<>();

            if (waitUsers.contains(userName)) {
                Toast.makeText(getContext(), "Already joined.", Toast.LENGTH_SHORT).show();
                return;
            }

            waitUsers.add(userName);

            eventRef.update("waitList.users", waitUsers).addOnSuccessListener(v -> {
                userWaitlistedEvents.add(eventName);
                userRef.update("waitListedEvents.events", userWaitlistedEvents);

                event.isJoined = true;
                button.setText("Leave Waitlist");

                Toast.makeText(getContext(), "Joined waitlist!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    /** Leave waitlist */
    private void leaveWaitlist(String eventName, EventItem event, Button button) {
        DocumentReference userRef = db.collection("users").document(userName);
        DocumentReference eventRef = db.collection("open events").document(eventName);

        eventRef.get().addOnSuccessListener(snapshot -> {
            Map<String, Object> waitData = (Map<String, Object>) snapshot.get("waitList");
            List<String> waitUsers = waitData != null && waitData.containsKey("users")
                    ? (List<String>) waitData.get("users")
                    : new ArrayList<>();

            if (!waitUsers.contains(userName)) return;

            waitUsers.remove(userName);

            eventRef.update("waitList.users", waitUsers).addOnSuccessListener(v -> {
                userWaitlistedEvents.remove(eventName);
                userRef.update("waitListedEvents.events", userWaitlistedEvents);

                event.isJoined = false;
                button.setText("Join Waitlist");

                Toast.makeText(getContext(), "Left waitlist.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    /** Open event detail screen */
    private void goToDetails(String eventName) {
        EntrantEventsScreenDirections.ActionEntrantEventsScreenToEventDetailsScreen action =
                EntrantEventsScreenDirections.actionEntrantEventsScreenToEventDetailsScreen(userName, eventName);
        NavHostFragment.findNavController(this).navigate(action);
    }

    /** Item Model */
    private static class EventItem {
        String name;
        boolean isOpen;
        boolean isJoined;

        EventItem(String name, boolean open) {
            this.name = name;
            this.isOpen = open;
        }
    }

    /** Custom List Adapter */
    private class EventListAdapter extends ArrayAdapter<EventItem> {

        EventListAdapter() {
            super(requireContext(), 0, eventList);
        }

        @NonNull
        @Override
        public View getView(int pos, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.HORIZONTAL);
                layout.setPadding(16, 16, 16, 16);
                layout.setLayoutParams(new ListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                ));

                TextView tv = new TextView(getContext());
                tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                tv.setTextSize(16f);

                Button btn = new Button(getContext());
                btn.setText("Join Waitlist");

                layout.addView(tv);
                layout.addView(btn);

                convertView = layout;
            }

            EventItem item = getItem(pos);
            if (item == null) return convertView;

            TextView tv = (TextView) ((LinearLayout) convertView).getChildAt(0);
            Button btn = (Button) ((LinearLayout) convertView).getChildAt(1);

            tv.setText((item.isOpen ? "🟢 " : "🔴 ") + item.name);
            btn.setVisibility(item.isOpen ? View.VISIBLE : View.GONE);

            btn.setText(item.isJoined ? "Leave Waitlist" : "Join Waitlist");

            convertView.setOnClickListener(v -> goToDetails(item.name));

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item.isJoined) {
                        leaveWaitlist(item.name, item, btn);
                    } else {
                        joinWaitlist(item.name, item, btn);
                    }
                }
            });


            return convertView;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
