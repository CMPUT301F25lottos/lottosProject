package com.example.lottos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentHomeScreenBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * Home Screen Fragment
 * - Provides navigation to other screens
 * - Automatically updates event IsOpen status based on close date/time
 */
public class HomeScreen extends Fragment {

    private FragmentHomeScreenBinding binding;
    private FirebaseFirestore db;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentHomeScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userName = HomeScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        // Automatically update event status
        updateEventOpenStatus();

        // Navigation (shared)
        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToProfileScreen(userName)));

        binding.btnInfo.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToLotteryInfoScreen(userName)));

        binding.btnLogout.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToWelcomeScreen()));

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToOrganizerEventsScreen(userName)));

        binding.btnWaitLists.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToEntrantWaitListsScreen(userName)));

        binding.btnOrgEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(HomeScreen.this)
                        .navigate(HomeScreenDirections.actionHomeScreenToEntrantEventsScreen(userName)));

        binding.btnEventHistory.setOnClickListener(v -> {
            NavHostFragment.findNavController(HomeScreen.this)
                    .navigate(HomeScreenDirections.actionHomeScreenToEventHistoryScreen(userName));
        });

        binding.btnNotification.setOnClickListener(v -> {
            NavHostFragment.findNavController(HomeScreen.this)
                    .navigate(HomeScreenDirections.actionHomeScreenToNotificationScreen(userName));
        });

    }

    /**
     * Checks all events in "open events" and updates IsOpen field
     * based on whether their waitlist close date/time has passed.
     */
    private void updateEventOpenStatus() {
        CollectionReference eventsRef = db.collection("open events");

        eventsRef.get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) return;
            //just use firebase clock instead of using local time
            com.google.firebase.Timestamp nowTs = com.google.firebase.Timestamp.now();



            for (DocumentSnapshot doc : querySnapshot) {
                com.google.firebase.Timestamp RegisterEnd = doc.getTimestamp("RegisterEnd");
                    Map<String, Object> waitList = (Map<String, Object>) doc.get("waitList");
                    if (waitList == null) continue;

                    if (RegisterEnd == null) continue;


                    // Check whether event should be open
                    boolean shouldBeOpen = nowTs.compareTo(RegisterEnd) < 0;
                    Boolean currentIsOpen = doc.getBoolean("IsOpen");


                    // Update Firestore only if value changed
                    if (currentIsOpen == null || currentIsOpen != shouldBeOpen) {
                        doc.getReference().update("IsOpen", shouldBeOpen);

                    }
                }

            Toast.makeText(getContext(), "Event statuses updated.", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to update event status.", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
