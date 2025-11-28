package com.example.lottos.organizer;


import android.net.Uri;
import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.EventRepository;
import com.example.lottos.ImageLoader;
import com.example.lottos.databinding.FragmentEditEventScreenBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;


import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;


public class EditEventScreen extends Fragment {
    private FragmentEditEventScreenBinding binding;
    private EventRepository repo;
    private OrganizerEventManager manager;
    private String userName;
    private String eventId;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private Uri selectedPosterUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPosterUri = uri;
                    binding.imgEventPoster.setImageURI(uri);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentEditEventScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditEventScreenArgs args = EditEventScreenArgs.fromBundle(getArguments());
        userName = args.getUserName();
        eventId = args.getEventId();

        repo = new EventRepository();
        manager = new OrganizerEventManager();

        loadEventInfo();

        DateTimePickerHelper helper = new DateTimePickerHelper(requireContext());
        binding.etStartTime.setOnClickListener(v -> helper.showDateTimePicker(binding.etStartTime));
        binding.etEndTime.setOnClickListener(v -> helper.showDateTimePicker(binding.etEndTime));
        binding.etRegisterEndTime.setOnClickListener(v -> helper.showDateTimePicker(binding.etRegisterEndTime));

        binding.imgEventPoster.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        binding.btnSave.setOnClickListener(v -> updateEventInfo());
        binding.btnCancel.setOnClickListener(v -> goBack());
        binding.btnDelete.setOnClickListener(v -> deleteEvent());
        binding.btnBack.setOnClickListener(v -> goBack());

        setupNavButtons();
    }

    private void loadEventInfo() {
        DocumentReference doc = repo.getEvent(eventId);
        doc.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {

                binding.etEventName.setText(snapshot.getString("eventName"));
                binding.etEventLocation.setText(snapshot.getString("location"));
                binding.etDescription.setText(snapshot.getString("description"));

                Long cap = snapshot.getLong("selectionCap");
                Long wlCap = snapshot.getLong("waitListCapacity");

                if (cap != null) binding.etCapacity.setText(String.valueOf(cap));
                if (wlCap != null) binding.etWaitListCapacity.setText(String.valueOf(wlCap));

                Timestamp start = snapshot.getTimestamp("startTime");
                Timestamp end = snapshot.getTimestamp("endTime");
                Timestamp reg = snapshot.getTimestamp("registerEndTime");

                if (start != null)
                    binding.etStartTime.setText(timestampToLocal(start).format(formatter));

                if (end != null)
                    binding.etEndTime.setText(timestampToLocal(end).format(formatter));

                if (reg != null)
                    binding.etRegisterEndTime.setText(timestampToLocal(reg).format(formatter));

                String url = snapshot.getString("posterUrl");
                ImageLoader.load(
                        url,
                        binding.imgEventPoster,
                        com.example.lottos.R.drawable.sample_event
                );
            } else Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateEventInfo() {

        String name = binding.etEventName.getText().toString().trim();
        String location = binding.etEventLocation.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();

        String startStr = binding.etStartTime.getText().toString().trim();
        String endStr = binding.etEndTime.getText().toString().trim();
        String regStr = binding.etRegisterEndTime.getText().toString().trim();

        String capStr = binding.etCapacity.getText().toString().trim();
        String wlStr = binding.etWaitListCapacity.getText().toString().trim();

        if (name.isEmpty() || location.isEmpty()) {
            Toast.makeText(getContext(), "Name & location required.", Toast.LENGTH_SHORT).show();
            return;
        }

        Integer cap = null, wlCap = null;

        try {
            if (!capStr.isEmpty()) cap = Integer.parseInt(capStr);
            if (!wlStr.isEmpty()) wlCap = Integer.parseInt(wlStr);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Invalid capacity.", Toast.LENGTH_SHORT).show();
            return;
        }

        LocalDateTime start = null, end = null, reg = null;

        try {
            if (!startStr.isEmpty()) start = LocalDateTime.parse(startStr, formatter);
            if (!endStr.isEmpty()) end = LocalDateTime.parse(endStr, formatter);
            if (!regStr.isEmpty()) reg = LocalDateTime.parse(regStr, formatter);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Use yyyy-MM-dd HH:mm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (start != null && end != null && !end.isAfter(start)) {
            Toast.makeText(getContext(), "End must be after start.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (reg != null && start != null && !start.isAfter(reg)) {
            Toast.makeText(getContext(), "Register-end must be before start.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("eventName", name);
        updates.put("location", location);
        updates.put("description", desc);
        updates.put("selectionCap", cap);
        updates.put("waitListCapacity", wlCap);

        if (start != null) updates.put("startTime", toTimestamp(start));
        if (end != null) updates.put("endTime", toTimestamp(end));
        if (reg != null) updates.put("registerEndTime", toTimestamp(reg));

        if (selectedPosterUri != null) {
            uploadPosterAndUpdate(updates);
        } else {
            applyUpdate(updates);
        }
    }

    private void uploadPosterAndUpdate(Map<String, Object> updates) {
        String path = "event_posters/" + eventId + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference(path);

        ref.putFile(selectedPosterUri)
                .addOnSuccessListener(t ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            updates.put("posterUrl", uri.toString());
                            applyUpdate(updates);
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(),
                                "Upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void applyUpdate(Map<String, Object> updates) {
        manager.updateEvent(eventId, updates,
                () -> {
                    Toast.makeText(getContext(), "Event updated!", Toast.LENGTH_SHORT).show();
                    goBack();
                },
                e -> Toast.makeText(getContext(),
                        "Failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    private void deleteEvent() {
        manager.deleteEvent(eventId,
                () -> {
                    Toast.makeText(getContext(), "Event deleted.", Toast.LENGTH_SHORT).show();
                    goBack();
                },
                e -> Toast.makeText(getContext(),
                        "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setupNavButtons() {
        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections.actionEditEventScreenToProfileScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections.actionEditEventScreenToNotificationScreen(userName)));

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections.actionEditEventScreenToOrganizerEventsScreen(userName)));

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections.actionEditEventScreenToEventHistoryScreen(userName)));
    }

    private void goBack() {
        NavHostFragment.findNavController(this)
                .navigate(EditEventScreenDirections
                        .actionEditEventScreenToOrganizerEventsScreen(userName));
    }

    private LocalDateTime timestampToLocal(Timestamp ts) {
        return ts.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private Timestamp toTimestamp(LocalDateTime ldt) {
        return new Timestamp(java.util.Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

    }
}
