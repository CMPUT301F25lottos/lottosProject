package com.example.lottos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentCreateEventScreenBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CreateEventScreen extends Fragment {

    private FragmentCreateEventScreenBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentCreateEventScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userName = CreateEventScreenArgs.fromBundle(getArguments()).getUserName();

        // Cancel → back to OrganizerEventsScreen
        binding.btnCancel.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToOrganizerEventsScreen(userName))
        );

        binding.btnCreateEvent.setOnClickListener(v -> {
            String eventName = binding.etEventName.getText().toString().trim();
            String location = binding.etEventLocation.getText().toString().trim();
            String desc = binding.etDescription.getText().toString().trim();

            String registerEndTime = binding.etRegisterEndTime.getText().toString().trim();
            String startTime = binding.etStartTime.getText().toString().trim();
            String endTime = binding.etEndTime.getText().toString().trim();

            String capStr = binding.etCapacity.getText().toString().trim();
            String wlCapStr = binding.etWaitListCapacity.getText().toString().trim();

            // Basic required fields
            if (eventName.isEmpty() || location.isEmpty() ||
                    startTime.isEmpty() || endTime.isEmpty() ||
                    capStr.isEmpty() || registerEndTime.isEmpty()) {
                android.widget.Toast.makeText(requireContext(),
                        "Please fill in all information", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // Capacity & waitlist capacity
            int capacity;
            Integer waitListCapacity = null;
            try {
                capacity = Integer.parseInt(capStr);
                if (!wlCapStr.isEmpty()) {
                    waitListCapacity = Integer.parseInt(wlCapStr);
                }
            } catch (NumberFormatException e) {
                android.widget.Toast.makeText(requireContext(),
                        "Please enter valid numbers for capacities", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            if (capacity <= 0) {
                android.widget.Toast.makeText(requireContext(),
                        "Capacity must be greater than 0", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // Parse times
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            LocalDateTime startLdt, endLdt, registerEndLdt;
            try {
                startLdt = LocalDateTime.parse(startTime, formatter);
                endLdt = LocalDateTime.parse(endTime, formatter);
                registerEndLdt = LocalDateTime.parse(registerEndTime, formatter);
            } catch (DateTimeParseException e) {
                android.widget.Toast.makeText(requireContext(),
                        "Invalid date/time. Use yyyy-MM-dd HH:mm", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            if (!endLdt.isAfter(startLdt)) {
                android.widget.Toast.makeText(requireContext(),
                        "End time must be after start time", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            if (!startLdt.isAfter(registerEndLdt)) {
                android.widget.Toast.makeText(requireContext(),
                        "Register end time must be before event start time", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            // Organizer info
            String organizer = userName;
            FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
            String organizerUid = (fbUser != null) ? fbUser.getUid() : null;

            // Convert to Date for Firestore
            Date startDate = Date.from(startLdt.atZone(ZoneId.systemDefault()).toInstant());
            Date endDate = Date.from(endLdt.atZone(ZoneId.systemDefault()).toInstant());
            Date registerEndDate = Date.from(registerEndLdt.atZone(ZoneId.systemDefault()).toInstant());

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            // Use your Event class as before (keeps existing behavior)
            Event event = new Event(
                    eventName,
                    organizer,
                    startLdt,
                    endLdt,
                    desc,
                    location,
                    capacity,
                    registerEndLdt
            );

            String eventId = event.getEventId(); // SAME as your original working code

            // Base document (same fields you already had)
            Map<String, Object> doc = new HashMap<>();
            doc.put("eventId", eventId);
            doc.put("eventName", eventName);
            doc.put("organizer", organizer);
            doc.put("organizerUid", organizerUid);
            doc.put("description", desc);
            doc.put("location", location);
            doc.put("selectionCap", capacity);
            if (waitListCapacity != null) {
                doc.put("waitListCapacity", waitListCapacity);
            }
            doc.put("startTime", new Timestamp(startDate));
            doc.put("endTime", new Timestamp(endDate));
            doc.put("RegisterEnd", new Timestamp(registerEndDate));
            doc.put("createdAt", Timestamp.now());
            doc.put("IsOpen", event.getIsOpen());

            // 🔹 NEW: Placeholder structures for lists (no user input required)

            // waitList: closeDate/CloseTime + entrants.users[]
            Map<String, Object> waitList = new HashMap<>();
            waitList.put("closeDate", "");
            waitList.put("CloseTime", "");
            Map<String, Object> waitEntrants = new HashMap<>();
            waitEntrants.put("users", new ArrayList<String>());
            waitList.put("entrants", waitEntrants);

            // selectedList: users[]
            Map<String, Object> selectedList = new HashMap<>();
            selectedList.put("users", new ArrayList<String>());

            // enrolledList: users[]
            Map<String, Object> enrolledList = new HashMap<>();
            enrolledList.put("users", new ArrayList<String>());

            // cancelledList: users[]
            Map<String, Object> cancelledList = new HashMap<>();
            cancelledList.put("users", new ArrayList<String>());

            doc.put("waitList", waitList);
            doc.put("selectedList", selectedList);
            doc.put("enrolledList", enrolledList);
            doc.put("cancelledList", cancelledList);

            // Write to Firestore
            db.collection("open events")
                    .document(eventId)
                    .set(doc)
                    .addOnSuccessListener(unused -> {
                        android.widget.Toast.makeText(requireContext(),
                                "Event created", android.widget.Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(CreateEventScreen.this)
                                .navigate(CreateEventScreenDirections
                                        .actionCreateEventScreenToOrganizerEventsScreen(userName));
                    })
                    .addOnFailureListener(e -> android.widget.Toast.makeText(requireContext(),
                            "Failed to create event: " + e.getMessage(),
                            android.widget.Toast.LENGTH_SHORT).show());
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
