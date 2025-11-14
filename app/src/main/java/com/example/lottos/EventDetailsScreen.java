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

import com.example.lottos.databinding.FragmentEventDetailsScreenBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * Fragment that displays detailed information for a specific event.
 * Role: Retrieves event details from Firestore, determines
 * the user’s role (organizer or entrant), and adjusts the UI accordingly.
 * - For organizers: allows viewing event information and running the lottery when registration closes.<br>
 * - For entrants: allows joining/leaving the waitlist, and accepting/declining an invitation after selection.<br>
 * Uses Firestore transactions to ensure atomic updates to both the event document and the user's event lists.
 */

public class EventDetailsScreen extends Fragment {

    private FragmentEventDetailsScreenBinding binding;
    private String eventName;
    private String userName;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventDetailsScreenBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            EventDetailsScreenArgs args = EventDetailsScreenArgs.fromBundle(getArguments());
            eventName = args.getEventName();
            userName = args.getUserName();
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventDetailsScreenDirections.actionEventDetailsScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventDetailsScreenDirections.actionEventDetailsScreenToProfileScreen(userName)));

        binding.btnHome.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventDetailsScreenDirections.actionEventDetailsScreenToHomeScreen(userName)));

        hideAllButtons();
        setupEntrantUI(userName);
    }

    private void setupEntrantUI(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference usersDoc = db.collection("users").document(userName);

        eventDoc.get().addOnSuccessListener(eventSnapshot -> {
            if (!eventSnapshot.exists()) {
                Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Extract waitlist safely
            List<String> waitlistUsers = new ArrayList<>();
            Map<String, Object> waitListMap = (Map<String, Object>) eventSnapshot.get("waitList");
            if (waitListMap != null && waitListMap.get("users") instanceof List) {
                waitlistUsers = (List<String>) waitListMap.get("users");
            }

            Boolean isOpen = eventSnapshot.getBoolean("IsOpen");
            Boolean isLottery = eventSnapshot.getBoolean("IsLottery");
            String organizer = eventSnapshot.getString("organizer");
            String location = eventSnapshot.getString("location");
            String description = eventSnapshot.getString("description");
            Long capLong = eventSnapshot.getLong("selectionCap");

            if (organizer != null) binding.tvOrganizer.setText("Organizer: " + organizer);
            if (location != null) binding.tvLocation.setText("Location: " + location);
            if (description != null) binding.tvDescription.setText("Description: " + description);
            if (capLong != null) binding.tvCapacity.setText("Capacity: " + capLong);

            // Organizer view: show "Run Lottery" only if not already run
            if (organizer != null && organizer.equals(userName)) {
                    // Show the button if lottery not yet done
                    binding.btnLottery.setVisibility(View.VISIBLE);
                    List<String> finalWaitlist = new ArrayList<>(waitlistUsers);
                    binding.btnLottery.setOnClickListener(v -> {
                        binding.btnLottery.setEnabled(false);
                        //Toast.makeText(getContext(), "Lottery started...", Toast.LENGTH_SHORT).show();
                        runLottery(eventDoc, finalWaitlist);
                        binding.btnLottery.postDelayed(() -> binding.btnLottery.setEnabled(true), 1500);
                    });
                }


            // Entrant-side logic
            usersDoc.get().addOnSuccessListener(userSnapshot -> {
                Map<String, Object> selectedMap =
                        (Map<String, Object>) userSnapshot.get("selectedEvents");
                List<String> selectedEvents = selectedMap != null
                        ? (List<String>) selectedMap.get("events")
                        : new ArrayList<>();

                Map<String, Object> waitlistedMap =
                        (Map<String, Object>) userSnapshot.get("waitListedEvents");
                List<String> waitListedEvents = waitlistedMap != null
                        ? (List<String>) waitlistedMap.get("events")
                        : new ArrayList<>();

                boolean isAlreadyWaitlisted = waitListedEvents.contains(eventName);
                boolean isselected = selectedEvents.contains(eventName);

                binding.btnBack.setVisibility(View.VISIBLE);

                if (isOpen != null && isOpen) {
                    binding.btnJoin.setVisibility(View.VISIBLE);
                    binding.btnJoin.setText(isAlreadyWaitlisted ? "Leave Waitlist" : "Join Waitlist");
                    binding.btnJoin.setOnClickListener(v -> {
                        if ("Join Waitlist".contentEquals(binding.btnJoin.getText())) {
                            joinWaitlist(userName);
                        } else {
                            leaveWaitlist(userName);
                        }
                    });
                } else {
                    if (isselected) {
                        binding.btnAccept.setVisibility(View.VISIBLE);
                        binding.btnDecline.setVisibility(View.VISIBLE);
                        binding.btnAccept.setOnClickListener(v -> acceptInvite(userName));
                        binding.btnDecline.setOnClickListener(v -> declineInvite(userName));
                    } else if (waitListedEvents.contains(eventName)) {
                        Toast.makeText(getContext(),
                                "Event closed",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(),
                            "Failed to load user info.",
                            Toast.LENGTH_SHORT).show());

        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Failed to load event details", e);
            Toast.makeText(getContext(), "Error loading event.", Toast.LENGTH_SHORT).show();
        });
    }

    private void runLottery(DocumentReference eventDoc, List<String> entrantsList) {

        eventDoc.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            Boolean isLottery = snapshot.getBoolean("IsLottery");
            if (Boolean.TRUE.equals(isLottery)) {
                Toast.makeText(getContext(),
                        "Lottery already completed for this event. Cannot run again.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            Boolean isOpen = snapshot.getBoolean("IsOpen");
            if (Boolean.TRUE.equals(isOpen)) {
                Toast.makeText(getContext(),
                        "This event is still open. Cannot run the lottery yet.",
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (entrantsList == null || entrantsList.isEmpty()) {
                Toast.makeText(getContext(),
                        "No entrants in waitlist to run the lottery.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            Long selectionCapLong = snapshot.getLong("selectionCap");
            int selectionCap = (selectionCapLong != null)
                    ? selectionCapLong.intValue()
                    : entrantsList.size();

            LotterySystem lottery = new LotterySystem(eventName);
            ArrayList<String> selected = lottery.Selected(new ArrayList<>(entrantsList));

            if (selected.size() > selectionCap) {
                selected = new ArrayList<>(selected.subList(0, selectionCap));
            }

            if (selected.isEmpty()) {
                Toast.makeText(getContext(),
                        "No users selected by lottery.",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            final List<String> finalSelected = new ArrayList<>(selected);
            FirebaseFirestore dbRef = FirebaseFirestore.getInstance();

            // Compute notSelected users
            List<String> notSelectedUsers = new ArrayList<>(entrantsList);
            notSelectedUsers.removeAll(finalSelected);

            // ✅ Transaction for atomic updates
            dbRef.runTransaction(transaction -> {
                // Update selected & notSelected lists
                transaction.update(eventDoc, "selectedList.users", finalSelected);
                transaction.update(eventDoc, "notSelectedList.users", notSelectedUsers);

                // ✅ Remove both groups from waitList (so it's empty after draw)
                for (String user : entrantsList) {
                    transaction.update(eventDoc, "waitList.users", FieldValue.arrayRemove(user));
                }

                // Mark event closed and lottery complete
                transaction.update(eventDoc, "IsOpen", false);
                transaction.update(eventDoc, "IsLottery", true);

                // Update each user's personal data
                for (String user : finalSelected) {
                    DocumentReference userDoc = dbRef.collection("users").document(user);
                    transaction.update(userDoc, "selectedEvents.events", FieldValue.arrayUnion(eventName));
                }
                for (String user : notSelectedUsers) {
                    DocumentReference userDoc = dbRef.collection("users").document(user);
                    transaction.update(userDoc, "notSelectedEvents.events", FieldValue.arrayUnion(eventName));
                }

                return null;
            }).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(),
                        "✅ Lottery completed!\n" +
                                finalSelected.size() + " selected, " +
                                notSelectedUsers.size() + " not selected.",
                        Toast.LENGTH_LONG).show();
            }).addOnFailureListener(e -> {
                Log.e("Firestore", "Error running lottery transaction", e);
                Toast.makeText(getContext(),
                        "Failed to complete lottery: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            });

        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Error fetching event for lottery", e);
            Toast.makeText(getContext(),
                    "Failed to run lottery.",
                    Toast.LENGTH_SHORT).show();
        });
    }



    /** ACCEPT INVITE */
    private void acceptInvite(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference userDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            transaction.update(eventDoc,
                    "enrolledList.users",
                    FieldValue.arrayUnion(userName));

            transaction.update(userDoc,
                    "selectedEvents.events",
                    FieldValue.arrayRemove(eventName));

            transaction.update(userDoc,
                    "enrolledEvents.events",
                    FieldValue.arrayUnion(eventName));

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "You are now enrolled!", Toast.LENGTH_SHORT).show();
            binding.btnAccept.setVisibility(View.GONE);
            binding.btnDecline.setVisibility(View.GONE);
            binding.btnBack.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to accept invite.", Toast.LENGTH_SHORT).show());
    }

    /** DECLINE INVITE */
    private void declineInvite(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference userDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            transaction.update(eventDoc,
                    "cancelledList.users",
                    FieldValue.arrayUnion(userName));

            transaction.update(userDoc,
                    "selectedEvents.events",
                    FieldValue.arrayRemove(eventName));

            transaction.update(userDoc,
                    "declinedEvents.events",
                    FieldValue.arrayUnion(eventName));

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "You declined the invite.", Toast.LENGTH_SHORT).show();
            binding.btnAccept.setVisibility(View.GONE);
            binding.btnDecline.setVisibility(View.GONE);
            binding.btnBack.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to decline invite.", Toast.LENGTH_SHORT).show());
    }

    /** JOIN WAITLIST */
    private void joinWaitlist(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference usersDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || !isOpen) {
                throw new FirebaseFirestoreException(
                        "This event is closed.",
                        FirebaseFirestoreException.Code.ABORTED
                );
            }

            transaction.update(eventDoc, "waitList.users", FieldValue.arrayUnion(userName));
            transaction.update(usersDoc, "waitListedEvents.events", FieldValue.arrayUnion(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(),
                    "Joined waitlist!",
                    Toast.LENGTH_SHORT).show();
            binding.btnJoin.setText("Leave Waitlist");
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(),
                    "Join failed: " + e.getMessage(),
                    Toast.LENGTH_SHORT).show();
            Log.e("Firestore", "Join waitlist failed", e);
        });
    }

    /** LEAVE WAITLIST */
    private void leaveWaitlist(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference usersDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            transaction.update(eventDoc,
                    "waitList.users",
                    FieldValue.arrayRemove(userName));
            transaction.update(usersDoc,
                    "waitListedEvents.events",
                    FieldValue.arrayRemove(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(),
                    "Left waitlist successfully!",
                    Toast.LENGTH_SHORT).show();
            binding.btnJoin.setText("Join Waitlist");
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(),
                    "Failed to leave waitlist.",
                    Toast.LENGTH_SHORT).show();
            Log.e("Firestore", "Leave waitlist failed", e);
        });
    }

    private void hideAllButtons() {
        binding.btnJoin.setVisibility(View.GONE);
        binding.btnLeave.setVisibility(View.GONE);
        binding.btnAccept.setVisibility(View.GONE);
        binding.btnDecline.setVisibility(View.GONE);
        binding.btnViewWaitList.setVisibility(View.GONE);
        binding.btnViewSelected.setVisibility(View.GONE);
        binding.btnViewCancelled.setVisibility(View.GONE);
        binding.btnViewEnrolled.setVisibility(View.GONE);
        binding.btnBack.setVisibility(View.GONE);
        binding.btnLottery.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
