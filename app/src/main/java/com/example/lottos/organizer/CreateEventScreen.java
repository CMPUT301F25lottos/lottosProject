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

import com.example.lottos.entities.Event;
import com.example.lottos.databinding.FragmentCreateEventScreenBinding;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class CreateEventScreen extends Fragment {

    private FragmentCreateEventScreenBinding binding;
    private final OrganizerEventManager manager = new OrganizerEventManager();
    private String userName;

    private Uri selectedPosterUri = null;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPosterUri = uri;
                    binding.imgEventPoster.setImageURI(uri);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        binding = FragmentCreateEventScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = CreateEventScreenArgs.fromBundle(getArguments()).getUserName();

        DateTimePickerHelper helper = new DateTimePickerHelper(requireContext());
        binding.etRegisterEndTime.setOnClickListener(v -> helper.showDateTimePicker(binding.etRegisterEndTime));
        binding.etStartTime.setOnClickListener(v -> helper.showDateTimePicker(binding.etStartTime));
        binding.etEndTime.setOnClickListener(v -> helper.showDateTimePicker(binding.etEndTime));

        binding.imgEventPoster.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        setupNavButtons();
    }

    private void handleCreateEvent() {

        String eventName = binding.etEventName.getText().toString().trim();
        String location = binding.etEventLocation.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();

        String registerEndTime = binding.etRegisterEndTime.getText().toString().trim();
        String startTime = binding.etStartTime.getText().toString().trim();
        String endTime = binding.etEndTime.getText().toString().trim();

        String capStr = binding.etCapacity.getText().toString().trim();
        String wlCapStr = binding.etWaitListCapacity.getText().toString().trim();

        if (eventName.isEmpty() || location.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || capStr.isEmpty() || registerEndTime.isEmpty()) {
            Toast.makeText(requireContext(), "Fill required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity;
        Integer waitCap = null;
        try {
            capacity = Integer.parseInt(capStr);
            if (!wlCapStr.isEmpty()) waitCap = Integer.parseInt(wlCapStr);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Invalid capacity.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capacity <= 0) {
            Toast.makeText(requireContext(), "Capacity must be > 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime startLdt, endLdt, regEndLdt;

        try {
            startLdt = LocalDateTime.parse(startTime, f);
            endLdt = LocalDateTime.parse(endTime, f);
            regEndLdt = LocalDateTime.parse(registerEndTime, f);
        } catch (DateTimeParseException e) {
            Toast.makeText(requireContext(), "Use yyyy-MM-dd HH:mm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!endLdt.isAfter(startLdt)) {
            Toast.makeText(requireContext(), "End must be after start.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!startLdt.isAfter(regEndLdt)) {
            Toast.makeText(requireContext(), "Register-end must be before start.", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = new Event(eventName, userName, startLdt, endLdt, desc, location, capacity, regEndLdt);

        if (selectedPosterUri != null) {
            uploadPosterAndCreate(event, regEndLdt, waitCap);
        } else {
            finishCreate(event, regEndLdt, waitCap);
        }
    }

    private void uploadPosterAndCreate(Event event, LocalDateTime regEnd, Integer waitCap) {

        String path = "event_posters/" + event.getEventId() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference(path);

        ref.putFile(selectedPosterUri).addOnSuccessListener(task ->
                        ref.getDownloadUrl().addOnSuccessListener(url -> {
                            event.setPosterUrl(url.toString());
                            finishCreate(event, regEnd, waitCap);
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Poster upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
    private void finishCreate(Event event, LocalDateTime regEnd, Integer waitCap) {

        manager.createEvent(event, regEnd, waitCap, () -> {
                    Toast.makeText(requireContext(), "Event created!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(CreateEventScreen.this)
                            .navigate(CreateEventScreenDirections
                                    .actionCreateEventScreenToOrganizerEventsScreen(userName));
                },
                e -> Toast.makeText(requireContext(),
                        "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
    private void setupNavButtons() {

        binding.btnCreateEvent.setOnClickListener(v -> handleCreateEvent());

        binding.btnCancel.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToOrganizerEventsScreen(userName)));

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToHomeScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToProfileScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToNotificationScreen(userName)));

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToEventHistoryScreen(userName)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
