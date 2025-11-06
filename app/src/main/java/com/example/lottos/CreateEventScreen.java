package com.example.lottos;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lottos.databinding.FragmentCreateEventScreenBinding;

/**
 *
 */
public class CreateEventScreen extends Fragment {

    private FragmentCreateEventScreenBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentCreateEventScreenBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userName = CreateEventScreenArgs.fromBundle(getArguments()).getUserName(); // pass the user info

        binding.btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(CreateEventScreen.this).navigate(CreateEventScreenDirections.actionCreateEventScreenToOrganizerEventsScreen(userName));
            }
        });
        binding.btnCreateEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String eventName = binding.etEventName.getText().toString().trim();
                String location  = binding.etEventLocation.getText().toString().trim();
                String desc      = binding.etDescription.getText().toString().trim();
                String startTime = binding.etStartTime.getText().toString().trim();
                String endTime   = binding.etEndTime.getText().toString().trim();
                String capStr    = binding.etCapacity.getText().toString().trim();
                String wlCapStr  = binding.etWaitListCapacity.getText().toString().trim();
                if (eventName.isEmpty() || location.isEmpty() ||
                        startTime.isEmpty() || endTime.isEmpty() || capStr.isEmpty()) {
                    android.widget.Toast.makeText(requireContext(),
                            "Please fill in all information", android.widget.Toast.LENGTH_SHORT).show();

                    return;
                }
                int capacity;
                Integer waitListCapacity = null;
                try {
                    capacity = Integer.parseInt(capStr.trim());
                    if (!wlCapStr.isEmpty()) {
                        waitListCapacity = Integer.parseInt(wlCapStr.trim());
                    }
                } catch (NumberFormatException e) {
                    android.widget.Toast.makeText(requireContext(),
                            "Please enter a number", android.widget.Toast.LENGTH_SHORT).show();                    return;
                }
                if (capacity <= 0) {
                    android.widget.Toast.makeText(requireContext(),
                            "Number should bigger than 0", android.widget.Toast.LENGTH_SHORT).show();                    return;
                }
                java.time.format.DateTimeFormatter formatter =
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                java.time.LocalDateTime startLdt;
                java.time.LocalDateTime endLdt;
                try {
                    startLdt = java.time.LocalDateTime.parse(startTime, formatter);
                    endLdt   = java.time.LocalDateTime.parse(endTime, formatter);
                } catch (java.time.format.DateTimeParseException e) {
                    android.widget.Toast.makeText(requireContext(),
                            "Invalid date/time. Use yyyy-MM-dd HH:mm", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!endLdt.isAfter(startLdt)) {
                    android.widget.Toast.makeText(requireContext(),
                            "End time must be after start time", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                String organizer = CreateEventScreenArgs.fromBundle(getArguments()).getUserName();

                Event event = new Event(
                        eventName,
                        organizer,
                        startLdt,
                        endLdt,
                        desc,
                        location,
                        capacity
                );

                String userName = CreateEventScreenArgs.fromBundle(getArguments()).getUserName();

                java.util.Date startDate = java.util.Date.from(
                        startLdt.atZone(java.time.ZoneId.systemDefault()).toInstant());
                java.util.Date endDate = java.util.Date.from(
                        endLdt.atZone(java.time.ZoneId.systemDefault()).toInstant());

                com.google.firebase.auth.FirebaseUser fbUser =
                        com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                String organizerUid = (fbUser != null) ? fbUser.getUid() : null;
                com.google.firebase.firestore.FirebaseFirestore db =
                        com.google.firebase.firestore.FirebaseFirestore.getInstance();

                String eventId = event.getEventId();
                java.util.Map<String, Object> doc = new java.util.HashMap<>();
                doc.put("eventId", eventId);
                doc.put("eventName", eventName);
                doc.put("organizer", organizer);
                doc.put("organizerUid", organizerUid);
                doc.put("description", desc);
                doc.put("location", location);
                doc.put("selectionCap", capacity);
                if (waitListCapacity != null) doc.put("waitListCapacity", waitListCapacity);
                doc.put("startTime", new com.google.firebase.Timestamp(startDate));
                doc.put("endTime", new com.google.firebase.Timestamp(endDate));
                doc.put("createdAt", com.google.firebase.Timestamp.now());

                db.collection("open events")
                        .document(eventId)
                        .set(doc)
                        .addOnSuccessListener(unused -> {
                            android.widget.Toast.makeText(requireContext(),
                                    "Event created", android.widget.Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(CreateEventScreen.this)
                                    .navigate(CreateEventScreenDirections
                                            .actionCreateEventScreenToOrganizerEventsScreen(userName));
                        });



                android.widget.Toast.makeText(requireContext(),
                        "Event created", android.widget.Toast.LENGTH_SHORT).show();


            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}