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
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * This fragment allows organizers to view, edit, and delete existing events.
 * Role: Retrieves event details from Firestore,
 * displays them for editing and updates or deletes the event document based on user actions.
 * Provides navigation back to the organizer’s event list upon completion.
 */

public class EditEventScreen extends Fragment {

    private FragmentEditEventScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;
    private String eventName;

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
        eventName = args.getEventName();

        db = FirebaseFirestore.getInstance();

        loadEventInfo();

        binding.btnSave.setOnClickListener(v -> updateEventInfo());
        binding.btnDelete.setOnClickListener(v -> deleteEvent());
        binding.btnCancel.setOnClickListener(v ->
                NavHostFragment.findNavController(EditEventScreen.this)
                        .navigate(EditEventScreenDirections
                                .actionEditEventScreenToOrganizerEventsScreen(userName, eventName))
        );
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(EditEventScreen.this)
                        .navigate(EditEventScreenDirections
                                .actionEditEventScreenToOrganizerEventsScreen(userName, eventName))
        );
    }

    private void loadEventInfo() {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
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

        DocumentReference eventDoc = db.collection("open events").document(eventName);

        eventDoc.update(
                "eventName", newEventName,
                "location", location,
                "description", description,
                "selectionCap", selectionCapStr.isEmpty() ? null : Integer.parseInt(selectionCapStr),
                "waitListCapacity", waitListCapStr.isEmpty() ? null : Integer.parseInt(waitListCapStr)
        ).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Event updated successfully!", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(EditEventScreen.this)
                    .navigate(EditEventScreenDirections
                            .actionEditEventScreenToOrganizerEventsScreen(userName, eventName));
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to update event.", Toast.LENGTH_SHORT).show()
        );
    }

    private void deleteEvent() {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        eventDoc.delete().addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Event deleted.", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(EditEventScreen.this)
                    .navigate(EditEventScreenDirections
                            .actionEditEventScreenToOrganizerEventsScreen(userName, eventName));
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to delete event.", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
