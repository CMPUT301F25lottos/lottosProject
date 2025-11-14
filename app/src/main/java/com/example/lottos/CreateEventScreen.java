package com.example.lottos;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentCreateEventScreenBinding;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * UI for creating a new event.
 * All Firestore logic is delegated to OrganizerEventManager.
 */
public class CreateEventScreen extends Fragment {

    private FragmentCreateEventScreenBinding binding;
    private final OrganizerEventManager manager = new OrganizerEventManager();
    private String userName;

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

        userName = CreateEventScreenArgs.fromBundle(getArguments()).getUserName();
        // Make date/time fields easier to use
        setupDateTimePickers();

        binding.btnCancel.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToOrganizerEventsScreen(userName))
        );

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToNotificationScreen(userName))
        );

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToHomeScreen(userName))
        );

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(CreateEventScreen.this)
                        .navigate(CreateEventScreenDirections
                                .actionCreateEventScreenToProfileScreen(userName))
        );


        binding.btnCreateEvent.setOnClickListener(v -> handleCreateEvent());
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

        if (eventName.isEmpty() || location.isEmpty() ||
                startTime.isEmpty() || endTime.isEmpty() ||
                capStr.isEmpty() || registerEndTime.isEmpty()) {
            Toast.makeText(requireContext(),
                    "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity;
        Integer waitListCapacity = null;
        try {
            capacity = Integer.parseInt(capStr);
            if (!wlCapStr.isEmpty()) {
                waitListCapacity = Integer.parseInt(wlCapStr);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(),
                    "Enter valid numbers for capacities.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (capacity <= 0) {
            Toast.makeText(requireContext(),
                    "Capacity must be greater than 0.", Toast.LENGTH_SHORT).show();
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime startLdt, endLdt, registerEndLdt;
        try {
            startLdt = LocalDateTime.parse(startTime, formatter);
            endLdt = LocalDateTime.parse(endTime, formatter);
            registerEndLdt = LocalDateTime.parse(registerEndTime, formatter);
        } catch (DateTimeParseException e) {
            Toast.makeText(requireContext(),
                    "Invalid date/time format. Use yyyy-MM-dd HH:mm", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!endLdt.isAfter(startLdt)) {
            Toast.makeText(requireContext(),
                    "End time must be after start time.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!startLdt.isAfter(registerEndLdt)) {
            Toast.makeText(requireContext(),
                    "Register end time must be before event start time.", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = new Event(eventName, userName, startLdt, endLdt, desc, location, capacity, registerEndLdt
        );

        manager.createEvent(event, registerEndLdt, waitListCapacity,
                () -> {
                    Toast.makeText(requireContext(),
                            "Event created and linked to your organized events!",
                            Toast.LENGTH_SHORT).show();

                    NavHostFragment.findNavController(CreateEventScreen.this)
                            .navigate(CreateEventScreenDirections
                                    .actionCreateEventScreenToOrganizerEventsScreen(userName));
                },
                e -> Toast.makeText(requireContext(),
                        "Failed to create event: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Wire up the click listeners so tapping the date/time fields
     * opens a DatePicker + TimePicker and auto-fills the text.
     */
    private void setupDateTimePickers() {
        binding.etRegisterEndTime.setOnClickListener(
                v -> showDateTimePicker(binding.etRegisterEndTime));

        binding.etStartTime.setOnClickListener(
                v -> showDateTimePicker(binding.etStartTime));

        binding.etEndTime.setOnClickListener(
                v -> showDateTimePicker(binding.etEndTime));
    }

    /**
     * Shows a DatePicker, then a TimePicker, then formats as "yyyy-MM-dd HH:mm"
     * and places the result into the given EditText.
     */
    private void showDateTimePicker(EditText targetEditText) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    TimePickerDialog timePickerDialog = new TimePickerDialog(
                            requireContext(),
                            (timeView, hourOfDay, minute) -> {
                                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                calendar.set(Calendar.MINUTE, minute);

                                SimpleDateFormat sdf =
                                        new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                String formatted = sdf.format(calendar.getTime());
                                targetEditText.setText(formatted);
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true // 24-hour format
                    );
                    timePickerDialog.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
