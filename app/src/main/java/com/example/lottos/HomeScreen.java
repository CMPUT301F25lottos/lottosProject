package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
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
            if (querySnapshot.isEmpty()) {
                Toast.makeText(getContext(), "No events found.", Toast.LENGTH_SHORT).show();
                return;
            }

            com.google.firebase.Timestamp nowTs = com.google.firebase.Timestamp.now();

            int updatedCount = 0;

            for (DocumentSnapshot doc : querySnapshot) {
                com.google.firebase.Timestamp registerEnd = doc.getTimestamp("RegisterEnd");
                if (registerEnd == null) continue;

                boolean shouldBeOpen = nowTs.compareTo(registerEnd) < 0;
                Boolean currentIsOpen = doc.getBoolean("IsOpen");

                // Update only if the state changed
                if (currentIsOpen == null || currentIsOpen != shouldBeOpen) {
                    doc.getReference().update("IsOpen", shouldBeOpen)
                            .addOnSuccessListener(v -> Log.d("Firestore", "Updated " + doc.getId() + " to " + shouldBeOpen))
                            .addOnFailureListener(e -> Log.e("Firestore", "Failed to update event " + doc.getId(), e));
                    updatedCount++;
                }
            }

            Toast.makeText(getContext(),
                    updatedCount > 0 ? "✅ Updated " + updatedCount + " event statuses." : "No status changes.",
                    Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Failed to update event statuses", e);
            Toast.makeText(getContext(), "❌ Failed to update event statuses.", Toast.LENGTH_SHORT).show();
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
