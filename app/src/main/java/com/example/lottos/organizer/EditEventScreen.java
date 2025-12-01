package com.example.lottos.organizer;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.MultiAutoCompleteTextView;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * A Fragment that provides a form for an organizer to edit an existing event.
 *
 * Role: This screen allows the creator of an event to modify its details.
 * Its responsibilities include:
 * <ul>
 *     <li>Loading the current details of a specific event from Firestore and populating the form fields.</li>
 *     <li>Providing UI for editing all mutable event properties, such as name, capacity, and times.</li>
 *     <li>Allowing the organizer to change or add a new event poster image.</li>
 *     <li>Validating all the entered data before saving.</li>
 *     <li>Handling the update logic, including uploading a new poster to Firebase Storage if necessary.</li>
 *     <li>Providing an option to delete the event entirely.</li>
 *     <li>Managing navigation back to the organizer's event list.</li>
 * </ul>
 */
public class EditEventScreen extends Fragment {

    private FragmentEditEventScreenBinding binding;
    private EventRepository repo;
    private OrganizerEventManager manager;
    private String userName;
    private String eventId;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private Uri selectedPosterUri = null;
    private FirebaseFirestore db;

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
     * Inflates the layout for this fragment.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentEditEventScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView() has returned. This is where
     * fragment initialization, such as setting up UI components and listeners, is performed.
     *
     * @param view The View returned by onCreateView().
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view,
                              Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditEventScreenArgs args = EditEventScreenArgs.fromBundle(getArguments());
        userName = args.getUserName();
        eventId = args.getEventId();

        db = FirebaseFirestore.getInstance();
        repo = new EventRepository(db);
        manager = new OrganizerEventManager(repo, db, FirebaseAuth.getInstance());

        // Setup the MultiAutoCompleteTextView for filter keywords.
        setupFilterKeywordField();

        loadEventInfo();

        DateTimePickerHelper helper = new DateTimePickerHelper(requireContext());
        binding.etStartTime.setOnClickListener(
                v -> helper.showDateTimePicker(binding.etStartTime));
        binding.etEndTime.setOnClickListener(
                v -> helper.showDateTimePicker(binding.etEndTime));
        binding.etRegisterEndTime.setOnClickListener(
                v -> helper.showDateTimePicker(binding.etRegisterEndTime));

        binding.imgEventPoster.setOnClickListener(
                v -> pickImageLauncher.launch("image/*"));

        binding.btnSave.setOnClickListener(v -> updateEventInfo());
        binding.btnCancel.setOnClickListener(v -> goBack());
        binding.btnDelete.setOnClickListener(v -> deleteEvent());
        binding.btnBack.setOnClickListener(v -> goBack());

        setupNavButtons();
    }

    /**
     * Configures the MultiAutoCompleteTextView for filter keywords.
     * It uses a CommaTokenizer to allow for multiple, comma-separated keywords and
     * an adapter with preset suggestions.
     */
    private void setupFilterKeywordField() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                PRESET_KEYWORDS
        );

        MultiAutoCompleteTextView etFilter = binding.etFilter;
        etFilter.setAdapter(adapter);
        etFilter.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());

        // Shows the suggestion dropdown when the user taps the field.
        etFilter.setOnClickListener(v -> etFilter.showDropDown());
    }

    /**
     * Fetches the current data for the event from Firestore and populates
     * the form's input fields with the existing values.
     */
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

                // Load existing filter keywords into the MultiAutoCompleteTextView.
                String existingKeywords = snapshot.getString("filterKeywords");
                if (existingKeywords != null && !existingKeywords.isEmpty()) {
                    binding.etFilter.setText(existingKeywords);
                    // Move cursor to the end of the text.
                    binding.etFilter.setSelection(existingKeywords.length());
                }

            } else {
                Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Gathers all data from the form fields, validates it, and then initiates the update process.
     */
    private void updateEventInfo() {

        String name = binding.etEventName.getText().toString().trim();
        String location = binding.etEventLocation.getText().toString().trim();
        String desc = binding.etDescription.getText().toString().trim();

        String startStr = binding.etStartTime.getText().toString().trim();
        String endStr = binding.etEndTime.getText().toString().trim();
        String regStr = binding.etRegisterEndTime.getText().toString().trim();

        String capStr = binding.etCapacity.getText().toString().trim();
        String wlStr = binding.etWaitListCapacity.getText().toString().trim();

        // Read the comma-separated keywords from the MultiAutoCompleteTextView.
        String filterKeywords = binding.etFilter.getText().toString().trim();

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

        // Save the keywords as one comma-separated string in Firestore.
        updates.put("filterKeywords", filterKeywords);

        if (selectedPosterUri != null) {
            uploadPosterAndUpdate(updates);
        } else {
            applyUpdate(updates);
        }
    }

    /**
     * If a new poster was selected, this method uploads it to Firebase Storage
     * and then applies all other updates to the event document.
     * @param updates The map of field updates to apply to the Firestore document.
     */
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

    /**
     * Commits the map of updates to the event document in Firestore.
     * @param updates The map containing the fields and new values to be updated.
     */
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

    /**
     * Permanently deletes the current event from Firestore.
     */
    private void deleteEvent() {
        manager.deleteEvent(eventId,
                () -> {
                    Toast.makeText(getContext(), "Event deleted.", Toast.LENGTH_SHORT).show();
                    goBack();
                },
                e -> Toast.makeText(getContext(),
                        "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Sets up the OnClickListeners for the standard bottom navigation bar.
     */
    private void setupNavButtons() {
        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections
                                .actionEditEventScreenToProfileScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections
                                .actionEditEventScreenToNotificationScreen(userName)));

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections
                                .actionEditEventScreenToOrganizerEventsScreen(userName)));

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditEventScreenDirections
                                .actionEditEventScreenToEventHistoryScreen(userName)));
    }

    /**
     * Navigates the user back to the organizer's main event list screen.
     */
    private void goBack() {
        NavHostFragment.findNavController(this)
                .navigate(EditEventScreenDirections
                        .actionEditEventScreenToOrganizerEventsScreen(userName));
    }

    /**
     * Converts a Firebase Timestamp object to a local LocalDateTime object.
     * @param ts The Firebase Timestamp to convert.
     * @return The corresponding LocalDateTime.
     */
    private LocalDateTime timestampToLocal(Timestamp ts) {
        return ts.toDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    /**
     * Converts a local LocalDateTime object to a Firebase Timestamp object.
     * @param ldt The LocalDateTime to convert.
     * @return The corresponding Firebase Timestamp.
     */
    private Timestamp toTimestamp(LocalDateTime ldt) {
        return new Timestamp(
                java.util.Date.from(
                        ldt.atZone(ZoneId.systemDefault()).toInstant()
                )
        );
    }

    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * Clears the view binding object to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
