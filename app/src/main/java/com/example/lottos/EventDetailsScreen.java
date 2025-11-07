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

            // read from nested waitList.entrants.users
            Map<String, Object> waitListMap = (Map<String, Object>) eventSnapshot.get("waitList");
            List<String> userWaitlist = new ArrayList<>();
            if (waitListMap != null) {
                Map<String, Object> entrantsMap = (Map<String, Object>) waitListMap.get("entrants");
                if (entrantsMap != null && entrantsMap.get("users") instanceof List) {
                    userWaitlist = (List<String>) entrantsMap.get("users");
                }
            }

            List<String> selectedList = (List<String>) eventSnapshot.get("selectedList.users");
            if (selectedList == null) selectedList = new ArrayList<>();

            Boolean isOpen = eventSnapshot.getBoolean("IsOpen");
            String organizer = eventSnapshot.getString("organizer");
            String location = eventSnapshot.getString("location");
            String description = eventSnapshot.getString("description");
            Long capLong = eventSnapshot.getLong("selectionCap");

            if (organizer != null)
                binding.tvOrganizer.setText("Organizer: " + organizer);
            if (location != null)
                binding.tvLocation.setText("Location: " + location);
            if (description != null)
                binding.tvDescription.setText("Description: " + description);
            if (capLong != null)
                binding.tvCapacity.setText("Capacity: " + capLong);

            // Only organizer sees "Run Lottery"
            if (organizer != null && organizer.equals(userName)) {
                binding.btnLottery.setVisibility(View.VISIBLE);
                binding.btnLottery.setOnClickListener(v -> {
                    binding.btnLottery.setEnabled(false);
                    Toast.makeText(getContext(), "🎲 Lottery started...", Toast.LENGTH_SHORT).show();
                    runLottery(eventDoc);
                    binding.btnLottery.postDelayed(() -> binding.btnLottery.setEnabled(true), 2000);
                });
            }

            // Entrant logic
            usersDoc.get().addOnSuccessListener(usersSnapshot -> {
                Map<String, Object> invitedMap = (Map<String, Object>) usersSnapshot.get("invitedEvents");
                List<String> invitedEvents = invitedMap != null ? (List<String>) invitedMap.get("events") : new ArrayList<>();

                Map<String, Object> waitlistedMap = (Map<String, Object>) usersSnapshot.get("waitListedEvents");
                List<String> waitListedEvents = waitlistedMap != null ? (List<String>) waitlistedMap.get("events") : new ArrayList<>();

                boolean isAlreadyWaitlisted = waitListedEvents.contains(eventName);
                binding.btnBack.setVisibility(View.VISIBLE);

                if (isOpen != null && isOpen) {
                    binding.btnJoin.setText(isAlreadyWaitlisted ? "Leave Waitlist" : "Join Waitlist");
                    binding.btnJoin.setVisibility(View.VISIBLE);
                    binding.btnJoin.setOnClickListener(v -> {
                        if (binding.btnJoin.getText().toString().equals("Join Waitlist")) {
                            joinWaitlist(userName);
                        } else {
                            leaveWaitlist(userName);
                        }
                    });
                } else {
                    if (invitedEvents.contains(eventName)) {
                        binding.btnAccept.setVisibility(View.VISIBLE);
                        binding.btnDecline.setVisibility(View.VISIBLE);
                    } else if (waitListedEvents.contains(eventName)) {
                        Toast.makeText(getContext(), "Event closed — not selected.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Event closed — you were not in the waitlist.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }

    /** Runs lottery on waitList.entrants.users */
    private void runLottery(DocumentReference eventDoc) {
        eventDoc.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Extract nested waitList.entrants.users
            Map<String, Object> waitListMap = (Map<String, Object>) snapshot.get("waitList");
            List<String> entrantsList = new ArrayList<>();

            if (waitListMap != null) {
                Map<String, Object> entrantsMap = (Map<String, Object>) waitListMap.get("entrants");
                if (entrantsMap != null && entrantsMap.get("users") instanceof List) {
                    entrantsList = (List<String>) entrantsMap.get("users");
                }
            }

            if (entrantsList.isEmpty()) {
                Toast.makeText(getContext(),
                        "No entrants in waitlist to run the lottery.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get selection cap
            Long selectionCapLong = snapshot.getLong("selectionCap");
            int selectionCap = selectionCapLong != null
                    ? selectionCapLong.intValue()
                    : entrantsList.size();

            // Run the random selection
            LotterySystem lottery = new LotterySystem(eventName);
            ArrayList<String> selected = lottery.Selected(new ArrayList<>(entrantsList));

            if (selected.size() > selectionCap)
                selected = new ArrayList<>(selected.subList(0, selectionCap));

            // Update selectedList.users in the event doc
            List<String> finalEntrantsList = entrantsList;
            ArrayList<String> finalSelected = selected;
            eventDoc.update("selectedList.users", selected)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(),
                                "Lottery completed!",
                                Toast.LENGTH_LONG).show();

                        // For each entrant, update their user record
                        FirebaseFirestore db = FirebaseFirestore.getInstance();

                        for (String entrant : finalEntrantsList) {
                            DocumentReference userDoc = db.collection("users").document(entrant);

                            if (finalSelected.contains(entrant)) {
                                // Selected: add to invitedEvents
                                userDoc.update("invitedEvents.events", FieldValue.arrayUnion(eventName))
                                        .addOnSuccessListener(v -> Log.d("Lottery", entrant + " invited"))
                                        .addOnFailureListener(e -> Log.e("Lottery", "Failed to update invited for " + entrant, e));
                            } else {
                                // Not selected: add to uninvitedEvents
                                userDoc.update("uninvitedEvents.events", FieldValue.arrayUnion(eventName))
                                        .addOnSuccessListener(v -> Log.d("Lottery", entrant + " not selected"))
                                        .addOnFailureListener(e -> Log.e("Lottery", "Failed to update uninvited for " + entrant, e));
                            }
                        }

                        // Clear the waitlist after lottery
                        eventDoc.update("waitList.entrants.users", new ArrayList<>())
                                .addOnSuccessListener(v -> Log.d("Lottery", "Waitlist cleared"))
                                .addOnFailureListener(e -> Log.e("Lottery", "Failed to clear waitlist", e));

                    })
                    .addOnFailureListener(e -> Log.e("Firestore", "Error updating selectedList", e));

        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to read event data.", Toast.LENGTH_SHORT).show());
    }


    /** Join nested waitList.entrants.users */
    private void joinWaitlist(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference usersDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || !isOpen)
                throw new FirebaseFirestoreException("This event is closed.", FirebaseFirestoreException.Code.ABORTED);

            transaction.update(eventDoc, "waitList.entrants.users", FieldValue.arrayUnion(userName));
            transaction.update(usersDoc, "waitListedEvents.events", FieldValue.arrayUnion(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Joined waitlist!", Toast.LENGTH_SHORT).show();
            binding.btnJoin.setText("Leave Waitlist");
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Join failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Leave nested waitList.entrants.users */
    private void leaveWaitlist(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference usersDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            transaction.update(eventDoc, "waitList.entrants.users", FieldValue.arrayRemove(userName));
            transaction.update(usersDoc, "waitListedEvents.events", FieldValue.arrayRemove(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Left waitlist successfully!", Toast.LENGTH_SHORT).show();
            binding.btnJoin.setText("Join Waitlist");
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to leave waitlist.", Toast.LENGTH_SHORT).show());
    }

    /** Accept invite */
    private void acceptEventInvite(String userName) {
        DocumentReference eventDoc = db.collection("openEvents").document(eventName);
        DocumentReference usersDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || isOpen)
                throw new FirebaseFirestoreException("This event is still open.", FirebaseFirestoreException.Code.ABORTED);

            transaction.update(eventDoc, "enrolledList.users", FieldValue.arrayUnion(userName));
            transaction.update(eventDoc, "cancelledList.users", FieldValue.arrayRemove(userName));
            transaction.update(usersDoc, "invitedEvents.events", FieldValue.arrayRemove(eventName));
            transaction.update(usersDoc, "enrolledEvents.events", FieldValue.arrayUnion(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Invite accepted!", Toast.LENGTH_SHORT).show();
            hideAllButtons();
            binding.btnBack.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Decline invite */
    private void declineEventInvite(String userName) {
        DocumentReference eventDoc = db.collection("openEvents").document(eventName);
        DocumentReference usersDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || isOpen)
                throw new FirebaseFirestoreException("This event is still open.", FirebaseFirestoreException.Code.ABORTED);

            transaction.update(eventDoc, "cancelledList.users", FieldValue.arrayUnion(userName));
            transaction.update(eventDoc, "enrolledList.users", FieldValue.arrayRemove(userName));
            transaction.update(usersDoc, "invitedEvents.events", FieldValue.arrayRemove(eventName));
            transaction.update(usersDoc, "declinedEvents.events", FieldValue.arrayUnion(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Invite declined.", Toast.LENGTH_SHORT).show();
            hideAllButtons();
            binding.btnBack.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
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
