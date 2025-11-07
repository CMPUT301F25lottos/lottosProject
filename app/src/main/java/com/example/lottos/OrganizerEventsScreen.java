package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottos.databinding.FragmentOrganizerEventsScreenBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * This fragment that displays all events created by a specific organizer.
 *
 * Roles:
 * It connects Firestore data to the RecyclerView display, allowing organizers
 * to view, create, and edit their own events. This fragment also manages user
 * navigation between event creation, editing, and the home screen, serving as
 * the central hub for organizer-side interactions in the app.
 */

public class OrganizerEventsScreen extends Fragment {

    private FragmentOrganizerEventsScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;
    private List<String> organizerEvents = new ArrayList<>();
    private EventAdapter eventAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOrganizerEventsScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = OrganizerEventsScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        binding.recyclerOpenEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        eventAdapter = new EventAdapter(organizerEvents, eventName ->
                openEditEventScreen(userName, eventName));
        binding.recyclerOpenEvents.setAdapter(eventAdapter);

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToHomeScreen(userName))
        );

        binding.btnCreateEvent.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(OrganizerEventsScreenDirections
                                .actionOrganizerEventsScreenToCreateEventScreen(userName))
        );

        loadOrganizerEvents();
    }

    /** Load all events created by this organizer */
    private void loadOrganizerEvents() {
        db.collection("open events")
                .whereEqualTo("organizer", userName)
                .get()
                .addOnSuccessListener(query -> {
                    organizerEvents.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String eventName = doc.getString("eventName");
                        if (eventName != null) {
                            organizerEvents.add(eventName);
                        }
                    }

                    eventAdapter.notifyDataSetChanged();

                    if (organizerEvents.isEmpty()) {
                        Toast.makeText(getContext(), "You haven’t created any events yet.", Toast.LENGTH_SHORT).show();
                    }

                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Failed to load events", e);
                    Toast.makeText(getContext(), "Error loading events.", Toast.LENGTH_SHORT).show();
                });
    }

    /** Navigate to EditEventScreen */
    private void openEditEventScreen(String userName, String eventName) {
        OrganizerEventsScreenDirections.ActionOrganizerEventsScreenToEditEventScreen action =
                OrganizerEventsScreenDirections.actionOrganizerEventsScreenToEditEventScreen(userName, eventName);
        NavHostFragment.findNavController(this).navigate(action);
    }

    /** Adapter for event list */
    private static class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {
        interface OnEventClickListener {
            void onEventClick(String eventName);
        }

        private final List<String> events;
        private final OnEventClickListener listener;

        EventAdapter(List<String> events, OnEventClickListener listener) {
            this.events = events;
            this.listener = listener;
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tv;
            VH(@NonNull LinearLayout layout) {
                super(layout);
                tv = (TextView) layout.getChildAt(0);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout layout = new LinearLayout(parent.getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(16, 12, 16, 12);

            TextView tv = new TextView(parent.getContext());
            tv.setTextSize(16f);
            layout.addView(tv);

            return new VH(layout);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            String eventName = events.get(position);
            holder.tv.setText("📅 " + eventName);
            holder.itemView.setOnClickListener(v -> listener.onEventClick(eventName));
        }

        @Override
        public int getItemCount() {
            return events.size();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
