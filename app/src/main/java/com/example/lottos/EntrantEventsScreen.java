package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottos.databinding.FragmentEntrantEventsScreenBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
    private List<EventItem> eventList;
    private String userName;
    private List<String> userWaitlistedEvents = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEntrantEventsScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EntrantEventsScreenArgs args = EntrantEventsScreenArgs.fromBundle(getArguments());
        userName = args.getUserName();

        db = FirebaseFirestore.getInstance();
        eventList = new ArrayList<>();

        binding.recyclerEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerEvents.setAdapter(new EventItemAdapter());

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EntrantEventsScreenDirections.actionEntrantEventsScreenToHomeScreen(userName)));

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
                    Log.e("Firestore", "Failed to load user waitlist", e);
                    loadEvents();
                });
    }

    /** Load events and mark joined ones */
    private void loadEvents() {
        db.collection("open events").get().addOnSuccessListener(value -> {
            eventList.clear();
            for (QueryDocumentSnapshot doc : value) {
                String eventName = doc.getId();
                Boolean openFlag = doc.getBoolean("IsOpen");
                boolean isOpen = openFlag != null && openFlag;

                EventItem item = new EventItem(eventName, isOpen);
                item.isJoined = userWaitlistedEvents.contains(eventName);
                eventList.add(item);
            }

            eventList.sort((a, b) -> Boolean.compare(!a.IsOpen, !b.IsOpen));
            binding.recyclerEvents.getAdapter().notifyDataSetChanged();
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Failed to load events", e);
            Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
        });
    }

    /** Join waitlist */
    private void joinWaitlist(String eventName, EventItem event, Button button) {
        DocumentReference userRef = db.collection("users").document(userName);
        DocumentReference eventRef = db.collection("open events").document(eventName);
        //FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //String uid = user.getUid();


        eventRef.get().addOnSuccessListener(eventSnap -> {
            if (!eventSnap.exists()) return;

            // get waitlist array from event
            Map<String, Object> waitMap = (Map<String, Object>) eventSnap.get("waitList");
            List<String> waitUsers = waitMap != null && waitMap.containsKey("user")
                    ? (List<String>) waitMap.get("user") : new ArrayList<>();

            if (waitUsers.contains(userName)) {
                Toast.makeText(getContext(), "Already joined " + eventName, Toast.LENGTH_SHORT).show();
                return;
            }

            Long capLong = eventSnap.getLong("waitListCapacity");
            int capacity = capLong != null ? capLong.intValue() : 0;
            if (capacity > 0 && waitUsers.size() >= capacity) {
                Toast.makeText(getContext(), "Waitlist full for " + eventName, Toast.LENGTH_SHORT).show();
                return;
            }

            // add user to event waitlist
            waitUsers.add(userName);
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("waitList.user", waitUsers);

            eventRef.update(updateMap)
                    .addOnSuccessListener(v -> {
                        // add event to user's waitlist
                        userWaitlistedEvents.add(eventName);
                        Map<String, Object> userUpdate = new HashMap<>();
                        userUpdate.put("waitListedEvents.events", userWaitlistedEvents);

                        userRef.update(userUpdate)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Joined waitlist for " + eventName, Toast.LENGTH_SHORT).show();
                                    event.isJoined = true;
                                    button.setText("Leave Waitlist");
                                })
                                .addOnFailureListener(e -> Log.e("Firestore", "Error updating user waitlist", e));
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error updating event waitlist", e));
        });
    }

    /** Leave waitlist */
    private void leaveWaitlist(String eventName, EventItem event, Button button) {
        DocumentReference userRef = db.collection("users").document(userName);
        DocumentReference eventRef = db.collection("open events").document(eventName);

        eventRef.get().addOnSuccessListener(eventSnap -> {
            if (!eventSnap.exists()) return;

            Map<String, Object> waitMap = (Map<String, Object>) eventSnap.get("waitList");
            List<String> waitUsers = waitMap != null && waitMap.containsKey("user")
                    ? (List<String>) waitMap.get("user") : new ArrayList<>();

            if (!waitUsers.contains(userName)) {
                Toast.makeText(getContext(), "You are not on the waitlist.", Toast.LENGTH_SHORT).show();
                return;
            }

            waitUsers.remove(userName);
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put("waitList.user", waitUsers);

            eventRef.update(updateMap)
                    .addOnSuccessListener(v -> {
                        userWaitlistedEvents.remove(eventName);
                        Map<String, Object> userUpdate = new HashMap<>();
                        userUpdate.put("waitListedEvents.events", userWaitlistedEvents);

                        userRef.update(userUpdate)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Left waitlist for " + eventName, Toast.LENGTH_SHORT).show();
                                    event.isJoined = false;
                                    button.setText("Join Waitlist");
                                })
                                .addOnFailureListener(e -> Log.e("Firestore", "Error updating user waitlist", e));
                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error updating event waitlist", e));
        });
    }

    private void goToDetails(String eventName) {
        EntrantEventsScreenDirections.ActionEntrantEventsScreenToEventDetailsScreen action =
                EntrantEventsScreenDirections.actionEntrantEventsScreenToEventDetailsScreen(userName, eventName);
        NavHostFragment.findNavController(this).navigate(action);
    }

    /** Event model */
    private static class EventItem {
        String name;
        boolean IsOpen;
        boolean isJoined;
        EventItem(String name, boolean isOpen) {
            this.name = name;
            this.IsOpen = isOpen;
        }
    }

    private class EventItemAdapter extends RecyclerView.Adapter<EventItemAdapter.VH> {

        class VH extends RecyclerView.ViewHolder {
            TextView tvEventName;
            Button btnJoin;
            VH(@NonNull LinearLayout layout) {
                super(layout);
                tvEventName = (TextView) layout.getChildAt(0);
                btnJoin = (Button) layout.getChildAt(1);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(16, 16, 16, 16);
            layout.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));

            TextView tv = new TextView(parent.getContext());
            tv.setLayoutParams(new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            tv.setTextSize(16f);

            Button btn = new Button(parent.getContext());
            btn.setText("Join Waitlist");

            layout.addView(tv);
            layout.addView(btn);

            return new VH(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            EventItem ev = eventList.get(position);
            holder.tvEventName.setText((ev.IsOpen ? "🟢 Open: " : "🔴 Closed: ") + ev.name);

            holder.btnJoin.setVisibility(ev.IsOpen ? View.VISIBLE : View.GONE);
            holder.btnJoin.setEnabled(ev.IsOpen);

            holder.btnJoin.setText(ev.isJoined ? "Leave Waitlist" : "Join Waitlist");

            holder.itemView.setOnClickListener(v -> goToDetails(ev.name));

            holder.btnJoin.setOnClickListener(v -> {
                if (!ev.isJoined) joinWaitlist(ev.name, ev, holder.btnJoin);
                else leaveWaitlist(ev.name, ev, holder.btnJoin);
            });
        }

        @Override
        public int getItemCount() {
            return eventList.size();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
