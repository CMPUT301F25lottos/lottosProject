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

            // Waitlist
            List<String> waitlistUsers = new ArrayList<>();
            Map<String, Object> waitListMap = (Map<String, Object>) eventSnapshot.get("waitList");
            if (waitListMap != null) {
                Map<String, Object> entrantsMap = (Map<String, Object>) waitListMap.get("entrants");
                if (entrantsMap != null && entrantsMap.get("users") instanceof List) {
                    waitlistUsers = (List<String>) entrantsMap.get("users");
                }
            }

            Boolean isOpen = eventSnapshot.getBoolean("IsOpen");
            String organizer = eventSnapshot.getString("organizer");
            String location = eventSnapshot.getString("location");
            String description = eventSnapshot.getString("description");
            Long capLong = eventSnapshot.getLong("selectionCap");

            if (organizer != null) binding.tvOrganizer.setText("Organizer: " + organizer);
            if (location != null) binding.tvLocation.setText("Location: " + location);
            if (description != null) binding.tvDescription.setText("Description: " + description);
            if (capLong != null) binding.tvCapacity.setText("Capacity: " + capLong);

            // Organizer view: show "Run Lottery"
            if (organizer != null && organizer.equals(userName)) {
                binding.btnLottery.setVisibility(View.VISIBLE);
                List<String> finalWaitlist = new ArrayList<>(waitlistUsers);
                binding.btnLottery.setOnClickListener(v -> {
                    binding.btnLottery.setEnabled(false);
                    Toast.makeText(getContext(), "🎲 Lottery started...", Toast.LENGTH_SHORT).show();
                    runLottery(eventDoc, finalWaitlist);
                    binding.btnLottery.postDelayed(() -> binding.btnLottery.setEnabled(true), 1500);
                });
            }

            // Entrant view: join/leave or accept/decline
            usersDoc.get().addOnSuccessListener(userSnapshot -> {
                Map<String, Object> invitedMap =
                        (Map<String, Object>) userSnapshot.get("invitedEvents");
                List<String> invitedEvents = invitedMap != null
                        ? (List<String>) invitedMap.get("events")
                        : new ArrayList<>();

                Map<String, Object> waitlistedMap =
                        (Map<String, Object>) userSnapshot.get("waitListedEvents");
                List<String> waitListedEvents = waitlistedMap != null
                        ? (List<String>) waitlistedMap.get("events")
                        : new ArrayList<>();

                boolean isAlreadyWaitlisted = waitListedEvents.contains(eventName);
                boolean isInvited = invitedEvents.contains(eventName);

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
                    if (isInvited) {
                        binding.btnAccept.setVisibility(View.VISIBLE);
                        binding.btnDecline.setVisibility(View.VISIBLE);

                        binding.btnAccept.setOnClickListener(v -> acceptInvite(userName));
                        binding.btnDecline.setOnClickListener(v -> declineInvite(userName));
                    } else if (waitListedEvents.contains(eventName)) {
                        Toast.makeText(getContext(),
                                "Event closed — you were not selected.",
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
        if (entrantsList == null || entrantsList.isEmpty()) {
            Toast.makeText(getContext(),
                    "No entrants in waitlist to run the lottery.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        eventDoc.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) {
                Toast.makeText(getContext(), "Event not found.", Toast.LENGTH_SHORT).show();
                return;
            }

            Long selectionCapLong = snapshot.getLong("selectionCap");
            int selectionCap = (selectionCapLong != null)
                    ? selectionCapLong.intValue()
                    : entrantsList.size();

            LotterySystem lottery = new LotterySystem(eventName);
            ArrayList<String> selected =
                    lottery.Selected(new ArrayList<>(entrantsList));

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

            eventDoc.update("selectedList.users", finalSelected)
                    .addOnSuccessListener(aVoid -> {
                        for (String user : finalSelected) {
                            eventDoc.update("waitList.entrants.users",
                                    FieldValue.arrayRemove(user));
                        }
                        eventDoc.update("IsOpen", false);

                        for (String user : finalSelected) {
                            dbRef.collection("users").document(user)
                                    .update("invitedEvents.events",
                                            FieldValue.arrayUnion(eventName))
                                    .addOnFailureListener(e ->
                                            Log.e("Firestore",
                                                    "Error inviting user: " + user, e));
                        }

                        Toast.makeText(getContext(),
                                "✅ Lottery completed! Users selected.",
                                Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error updating selectedList", e);
                        Toast.makeText(getContext(),
                                "Failed to update selected users.",
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
                    "invitedEvents.events",
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
                    "invitedEvents.events",
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

            transaction.update(eventDoc,
                    "waitList.entrants.users",
                    FieldValue.arrayUnion(userName));
            transaction.update(usersDoc,
                    "waitListedEvents.events",
                    FieldValue.arrayUnion(eventName));
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
                    "waitList.entrants.users",
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
