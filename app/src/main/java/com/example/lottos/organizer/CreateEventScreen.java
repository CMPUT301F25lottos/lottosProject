package com.example.lottos.organizer;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentCreateEventScreenBinding;
import com.example.lottos.entities.Event;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class CreateEventScreen extends Fragment {

    private FragmentCreateEventScreenBinding binding;
    private final OrganizerEventManager manager = new OrganizerEventManager();
    private String userName;

    private Uri selectedPosterUri = null;

    // Example preset keywords – you can change/extend these
    private static final String[] PRESET_KEYWORDS = new String[] {
            "Sports",
            "Music",
            "Food",
            "Arts and Crafts",
            "Charity",
            "Cultural",
            "Workshop",
            "Party",
            "Study",
            "Networking",
            "Family",
            "Seniors",
            "Teens",
            "Health",
            "Kids",
            "Movie",
            "Other"
    };

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPosterUri = uri;
                    binding.imgEventPoster.setImageURI(uri);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCreateEventScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = CreateEventScreenArgs.fromBundle(getArguments()).getUserName();

        // Date/time pickers
        DateTimePickerHelper helper = new DateTimePickerHelper(requireContext());
        binding.etRegisterEndTime.setOnClickListener(
                v -> helper.showDateTimePicker(binding.etRegisterEndTime));
        binding.etStartTime.setOnClickListener(
                v -> helper.showDateTimePicker(binding.etStartTime));
        binding.etEndTime.setOnClickListener(
                v -> helper.showDateTimePicker(binding.etEndTime));

        // Poster picker
        binding.imgEventPoster.setOnClickListener(
                v -> pickImageLauncher.launch("image/*"));

        // Setup filter dropdown
        setupFilterDropdown();

        setupNavButtons();
    }

    private void setupFilterDropdown() {
        MultiAutoCompleteTextView filterView =
                (MultiAutoCompleteTextView) binding.etFilter;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                PRESET_KEYWORDS
        );
        filterView.setAdapter(adapter);

        // How many chars before it suggests – 1 is typical
        filterView.setThreshold(1);

        // This tells it how to split tokens: "music, party, food"
        filterView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        // Force dropdown to open when user taps into the field
        filterView.setOnClickListener(v -> {
            if (!filterView.isPopupShowing()) {
                filterView.showDropDown();
            }
        });

        filterView.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && !filterView.isPopupShowing()) {
                filterView.showDropDown();
            }
        });
    }



    private void handleCreateEvent() {

        String eventName = binding.etEventName.getText().toString().trim();
        String location  = binding.etEventLocation.getText().toString().trim();
        String desc      = binding.etDescription.getText().toString().trim();

        String registerEndTime = binding.etRegisterEndTime.getText().toString().trim();
        String startTime       = binding.etStartTime.getText().toString().trim();
        String endTime         = binding.etEndTime.getText().toString().trim();

        String capStr   = binding.etCapacity.getText().toString().trim();
        String filter   = binding.etFilter.getText().toString().trim();
        String wlCapStr = binding.etWaitListCapacity.getText().toString().trim();
        boolean geolocationRequired = binding.switchGeolocationRequired.isChecked();

        // Required fields check
        if (eventName.isEmpty() ||
                location.isEmpty() ||
                startTime.isEmpty() ||
                endTime.isEmpty() ||
                capStr.isEmpty() ||
                registerEndTime.isEmpty() ||
                filter.isEmpty()) {

            Toast.makeText(requireContext(),
                    "Fill required fields.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse capacity + optional waitlist capacity
        int capacity;
        Integer waitCap = null;
        try {
            capacity = Integer.parseInt(capStr);
            if (!wlCapStr.isEmpty()) {
                waitCap = Integer.parseInt(wlCapStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(),
                    "Invalid capacity.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (capacity <= 0) {
            Toast.makeText(requireContext(),
                    "Capacity must be > 0.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse date/times
        DateTimeFormatter f = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime startLdt;
        LocalDateTime endLdt;
        LocalDateTime regEndLdt;

        try {
            startLdt   = LocalDateTime.parse(startTime, f);
            endLdt     = LocalDateTime.parse(endTime, f);
            regEndLdt  = LocalDateTime.parse(registerEndTime, f);
        } catch (DateTimeParseException e) {
            Toast.makeText(requireContext(),
                    "Use yyyy-MM-dd HH:mm",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!endLdt.isAfter(startLdt)) {
            Toast.makeText(requireContext(),
                    "End must be after start.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!startLdt.isAfter(regEndLdt)) {
            Toast.makeText(requireContext(),
                    "Register-end must be before start.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Build filterWords list from the text (comma-separated)
        // e.g. "Music, Party, Free Food" -> ["music", "party", "free food"]
        List<String> filterWords = new ArrayList<>();
        for (String token : filter.split(",")) {
            String word = token.trim();
            if (!word.isEmpty()) {
                filterWords.add(word.toLowerCase());   // lowercase for easier searching later
            }
        }

        if (filterWords.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Please enter at least one valid keyword.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Build Event entity
        Event event = new Event(
                eventName,
                userName,
                startLdt,
                endLdt,
                desc,
                location,
                capacity,
                regEndLdt,
                filterWords
        );

        event.setGeolocationRequired(geolocationRequired);


        // Create with or without poster
        if (selectedPosterUri != null) {
            uploadPosterAndCreate(event, regEndLdt, waitCap, filterWords, geolocationRequired);
        } else {
            finishCreate(event, regEndLdt, waitCap, filterWords, geolocationRequired);
        }
    }


    private void uploadPosterAndCreate(Event event,
                                       LocalDateTime regEnd,
                                       Integer waitCap,
                                       List<String> filterWords,
                                       boolean geolocationRequired) {

        String path = "event_posters/" + event.getEventId() + ".jpg";
        StorageReference ref = FirebaseStorage.getInstance().getReference(path);

        ref.putFile(selectedPosterUri)
                .addOnSuccessListener(task ->
                        ref.getDownloadUrl().addOnSuccessListener(url -> {
                            event.setPosterUrl(url.toString());
                            finishCreate(event, regEnd, waitCap, filterWords, geolocationRequired);
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(),
                                "Poster upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    private void finishCreate(Event event,
                              LocalDateTime regEnd,
                              Integer waitCap,
                              List<String> filterWords,
                              boolean geolocationRequired) {

        manager.createEvent(event, regEnd, waitCap, filterWords, geolocationRequired,
                () -> {
                    Toast.makeText(requireContext(),
                            "Event created!",
                            Toast.LENGTH_SHORT).show();

                    NavHostFragment.findNavController(CreateEventScreen.this)
                            .navigate(CreateEventScreenDirections
                                    .actionCreateEventScreenToOrganizerEventsScreen(userName));
                },
                e -> Toast.makeText(requireContext(),
                        "Failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
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
