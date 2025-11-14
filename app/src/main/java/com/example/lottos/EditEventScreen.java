package com.example.lottos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentEditEventScreenBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

/**
 * UI for viewing and editing a single event.
 * Reads data via EventRepository, updates/deletes via OrganizerEventManager.
 */
public class EditEventScreen extends Fragment {

    private FragmentEditEventScreenBinding binding;
    private EventRepository repo;
    private OrganizerEventManager manager;
    private String userName;
    private String eventId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditEventScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditEventScreenArgs args = EditEventScreenArgs.fromBundle(getArguments());
        userName = args.getUserName();
        eventId = args.getEventId();  // requires nav_graph argument "eventId"

        repo = new EventRepository();
        manager = new OrganizerEventManager();

        loadEventInfo();

        binding.btnSave.setOnClickListener(v -> updateEventInfo());
        binding.btnDelete.setOnClickListener(v -> deleteEvent());
        binding.btnCancel.setOnClickListener(v -> goBack());
        binding.btnBack.setOnClickListener(v -> goBack());
    }

    private void loadEventInfo() {
        DocumentReference eventDoc = repo.getEvent(eventId);
        eventDoc.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                binding.etEventName.setText(snapshot.getString("eventName"));
                binding.etLocation.setText(snapshot.getString("location"));
                binding.etDescription.setText(snapshot.getString("description"));

                Long selectionCap = snapshot.getLong("selectionCap");
                Long waitListCap = snapshot.getLong("waitListCapacity");

                if (selectionCap != null)
                    binding.etSelectionCap.setText(String.valueOf(selectionCap));
                if (waitListCap != null)
                    binding.etWaitListCap.setText(String.valueOf(waitListCap));

                Timestamp startTime = snapshot.getTimestamp("startTime");
                Timestamp endTime = snapshot.getTimestamp("endTime");

                if (startTime != null)
                    binding.tvStartTime.setText("Start: " + startTime.toDate());
                if (endTime != null)
                    binding.tvEndTime.setText("End: " + endTime.toDate());
            } else {
                Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to load event info.", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateEventInfo() {
        String newEventName = binding.etEventName.getText().toString().trim();
        String location = binding.etLocation.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();
        String selectionCapStr = binding.etSelectionCap.getText().toString().trim();
        String waitListCapStr = binding.etWaitListCap.getText().toString().trim();

        if (newEventName.isEmpty() || location.isEmpty()) {
            Toast.makeText(getContext(), "Event name and location are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer selectionCap = selectionCapStr.isEmpty() ? null : Integer.parseInt(selectionCapStr);
        Integer waitListCap = waitListCapStr.isEmpty() ? null : Integer.parseInt(waitListCapStr);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("eventName", newEventName);
        updates.put("location", location);
        updates.put("description", description);
        updates.put("selectionCap", selectionCap);
        updates.put("waitListCapacity", waitListCap);

        manager.updateEvent(eventId, updates,
                () -> {
                    Toast.makeText(getContext(), "Event updated successfully!", Toast.LENGTH_SHORT).show();
                    goBack();
                },
                e -> Toast.makeText(getContext(), "Failed to update event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void deleteEvent() {
        manager.deleteEvent(eventId,
                () -> {
                    Toast.makeText(getContext(), "Event deleted.", Toast.LENGTH_SHORT).show();
                    goBack();
                },
                e -> Toast.makeText(getContext(), "Failed to delete event: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    private void goBack() {
        NavHostFragment.findNavController(EditEventScreen.this)
                .navigate(EditEventScreenDirections
                        .actionEditEventScreenToOrganizerEventsScreen(userName));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
