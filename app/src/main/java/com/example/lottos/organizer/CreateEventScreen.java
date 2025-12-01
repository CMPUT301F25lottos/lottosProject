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

/**
 * A Fragment that provides a form for organizers to create new events.
 *
 * Role: This screen is the primary interface for adding events to the system.
 * It is responsible for:
 * <ul>
 *     <li>Collecting all event details through input fields (name, location, capacity, etc.).</li>
 *     <li>Using a helper class to facilitate date and time selection.</li>
 *     <li>Allowing the user to select a poster image from their device's gallery.</li>
 *     <li>Validating all user inputs to ensure data integrity.</li>
 *     <li>Uploading the selected poster image to Firebase Storage if one is provided.</li>
 *     <li>Using the OrganizerEventManager to create the event document in Firestore.</li>
 *     <li>Handling navigation to and from the screen.</li>
 * </ul>
 */
public class CreateEventScreen extends Fragment {

    private FragmentCreateEventScreenBinding binding;
    private final OrganizerEventManager manager = new OrganizerEventManager();
    private String userName;

    private Uri selectedPosterUri = null;

    // A predefined list of keywords for the filter suggestions dropdown.
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

    /**
     * An ActivityResultLauncher to handle the result of the image selection intent.
     * When an image is picked, its URI is stored and the preview ImageView is updated.
     */
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPosterUri = uri;
                    binding.imgEventPoster.setImageURI(uri);
                }
            });

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout is inflated and the view binding is initialized.
     *
     * @param inflater The LayoutInflater object that can be used to inflate views.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCreateEventScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored.
     * This is where the fragment's logic is initialized, including setting up listeners and adapters.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = CreateEventScreenArgs.fromBundle(getArguments()).getUserName();

        // Sets up the click listeners for the date and time input fields.
        DateTimePickerHelper helper = new DateTimePickerHelper(requireContext());
        binding.etRegisterEndTime.setOnClickListener(
                v -> helper.showDateTimePicker(binding.etRegisterEndTime));
        binding.etStartTime.setOnClickListener(
                v -> helper.showDateTimePicker(binding.etStartTime));
        binding.etEndTime.setOnClickListener(
                v -> helper.showDateTimePicker(binding.etEndTime));

        // Sets up the click listener for the poster image to launch the image picker.
        binding.imgEventPoster.setOnClickListener(
                v -> pickImageLauncher.launch("image/*"));

        // Configures the auto-complete functionality for the filter keywords input.
        setupFilterDropdown();

        // Sets up all navigation and action button listeners.
        setupNavButtons();
    }

    /**
     * Configures the MultiAutoCompleteTextView for keyword filtering.
     * It sets up an adapter with preset keywords, a tokenizer for comma separation,
     * and listeners to show the dropdown on focus or click.
     */
    private void setupFilterDropdown() {
        MultiAutoCompleteTextView filterView =
                (MultiAutoCompleteTextView) binding.etFilter;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                PRESET_KEYWORDS
        );
        filterView.setAdapter(adapter);

        // Sets the number of characters the user must type before suggestions appear.
        filterView.setThreshold(1);

        // Defines the comma as the separator for multiple keywords.
        filterView.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        // Ensures the dropdown appears when the user clicks or focuses on the field.
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



    /**
     * Gathers, validates, and processes all user input from the form to create a new event.
     * This method acts as the entry point for the event creation logic.
     */
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

        // Validates that all required fields are filled.
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

        // Parses and validates the capacity and optional waitlist capacity.
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

        // Parses and validates the date and time strings.
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

        // Enforces logical time ordering.
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

        // Parses the comma-separated filter string into a list of lowercase keywords.
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

        // Constructs the Event entity with the validated data.
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


        // Decides whether to upload a poster or create the event directly.
        if (selectedPosterUri != null) {
            uploadPosterAndCreate(event, regEndLdt, waitCap, filterWords, geolocationRequired);
        } else {
            finishCreate(event, regEndLdt, waitCap, filterWords, geolocationRequired);
        }
    }


    /**
     * Uploads the selected poster image to Firebase Storage and then proceeds to create the event.
     * The image is stored in a path that includes the event's unique ID.
     *
     * @param event The Event object to be created.
     * @param regEnd The registration end time.
     * @param waitCap The waitlist capacity, which can be null.
     * @param filterWords The list of filter keywords.
     * @param geolocationRequired A flag indicating if geolocation is required.
     */
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

    /**
     * Calls the OrganizerEventManager to create the event in the database.
     * This is the final step in the creation process, handling success and failure callbacks.
     *
     * @param event The final Event object, possibly with a poster URL.
     * @param regEnd The registration end time.
     * @param waitCap The waitlist capacity, which can be null.
     * @param filterWords The list of filter keywords.
     * @param geolocationRequired A flag indicating if geolocation is required.
     */
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

    /**
     * Sets up the OnClickListeners for all action and navigation buttons on the screen.
     */
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

    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * The view binding object is cleared here to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
