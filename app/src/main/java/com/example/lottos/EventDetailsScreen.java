package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.lottos.databinding.FragmentEventDetailsScreenBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventDetailsScreen extends Fragment {

    private FragmentEventDetailsScreenBinding binding;
    private String eventId;
    private String eventName;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventDetailsScreenBinding.inflate(inflater, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
            eventName = getArguments().getString("eventName");
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userName = currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "TestEntrant1";

        // Setup listeners for all buttons
        binding.btnJoin.setOnClickListener(v -> joinWaitlist(userName));
        binding.btnLeave.setOnClickListener(v -> leaveWaitlist(userName));
        binding.btnAccept.setOnClickListener(v -> acceptEventInvite(userName));
        binding.btnDecline.setOnClickListener(v -> declineEventInvite(userName));
        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        // Load role (entrant vs organizer) and configure UI
        checkUserRoleAndSetupUI(userName);
    }

    /**
     * Determines if the current user is an organizer or entrant and sets up UI accordingly.
     */
    private void checkUserRoleAndSetupUI(String userName) {
        DocumentReference organizerDoc = db.collection("organizers").document(userName);
        organizerDoc.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                setupOrganizerUI();
            } else {
                setupEntrantUI(userName);
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to check user role.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Organizer UI logic.
     */
    private void setupOrganizerUI() {
        hideAllButtons();
        DocumentReference eventDoc = db.collection("open events").document(eventId);

        eventDoc.get().addOnSuccessListener(snapshot -> {
            Boolean isOpen = snapshot.getBoolean("IsOpen");
            if (isOpen != null && isOpen) {
                binding.btnViewWaitList.setVisibility(View.VISIBLE);
                binding.btnBack.setVisibility(View.VISIBLE);
            } else {
                binding.btnViewSelected.setVisibility(View.VISIBLE);
                binding.btnViewCancelled.setVisibility(View.VISIBLE);
                binding.btnViewEnrolled.setVisibility(View.VISIBLE);
                binding.btnBack.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Entrant UI logic.
     */
    private void setupEntrantUI(String userName) {
        hideAllButtons();

        DocumentReference eventDoc = db.collection("open events").document(eventId);
        DocumentReference entrantDoc = db.collection("entrants").document(userName);

        eventDoc.get().addOnSuccessListener(eventSnapshot -> {
            Boolean isOpen = eventSnapshot.getBoolean("IsOpen");

            entrantDoc.get().addOnSuccessListener(entrantSnapshot -> {
                Map<String, Object> invitedMap = (Map<String, Object>) entrantSnapshot.get("invitedEvents");
                List<String> invitedEvents = invitedMap != null ? (List<String>) invitedMap.get("events") : new ArrayList<>();

                Map<String, Object> waitlistedMap = (Map<String, Object>) entrantSnapshot.get("waitListedEvents");
                List<String> waitListedEvents = waitlistedMap != null ? (List<String>) waitlistedMap.get("events") : new ArrayList<>();

                if (isOpen != null && isOpen) {
                    // Event is open → show Join Waitlist + Back
                    binding.btnJoin.setVisibility(View.VISIBLE);
                    binding.btnBack.setVisibility(View.VISIBLE);

                } else {
                    // Event is closed
                    if (invitedEvents != null && invitedEvents.contains(eventName)) {
                        // Entrant was invited
                        binding.btnAccept.setVisibility(View.VISIBLE);
                        binding.btnDecline.setVisibility(View.VISIBLE);
                        binding.btnBack.setVisibility(View.VISIBLE);

                    } else if (waitListedEvents != null && waitListedEvents.contains(eventName)) {
                        // Entrant was waitlisted but not invited
                        Toast.makeText(getContext(),
                                "This event is closed. You were not selected.",
                                Toast.LENGTH_SHORT).show();
                        binding.btnBack.setVisibility(View.VISIBLE);

                    } else {
                        // Entrant was never in the waitlist
                        Toast.makeText(getContext(),
                                "The waitlist is closed and you were not a part of it.",
                                Toast.LENGTH_SHORT).show();
                        binding.btnBack.setVisibility(View.VISIBLE);
                    }
                }

            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to fetch entrant info.", Toast.LENGTH_SHORT).show());
        });
    }

    /**
     * Adds the user to the waitlist if the event is open.
     */
    private void joinWaitlist(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventId);
        DocumentReference entrantDoc = db.collection("entrants").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || !isOpen) {
                throw new FirebaseFirestoreException("This event is closed.", FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(eventDoc, "waitList.entrants.users", FieldValue.arrayUnion(userName));
            transaction.update(entrantDoc, "waitListedEvents.events", FieldValue.arrayUnion(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Joined waitlist successfully!", Toast.LENGTH_SHORT).show();
            hideAllButtons();
            binding.btnLeave.setVisibility(View.VISIBLE);
            binding.btnBack.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Removes the user from the waitlist.
     */
    private void leaveWaitlist(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventId);
        DocumentReference entrantDoc = db.collection("entrants").document(userName);

        db.runTransaction(transaction -> {
            transaction.update(eventDoc, "waitList.entrants.users", FieldValue.arrayRemove(userName));
            transaction.update(entrantDoc, "waitListedEvents.events", FieldValue.arrayRemove(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Left waitlist successfully!", Toast.LENGTH_SHORT).show();
            hideAllButtons();
            binding.btnJoin.setVisibility(View.VISIBLE);
            binding.btnBack.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to leave waitlist.", Toast.LENGTH_SHORT).show());
    }

    /**
     * Accepts an event invite (only if closed).
     */
    private void acceptEventInvite(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventId);
        DocumentReference entrantDoc = db.collection("entrants").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || isOpen) {
                throw new FirebaseFirestoreException("This event is still open.",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(eventDoc, "enrolledList.users", FieldValue.arrayUnion(userName));
            transaction.update(eventDoc, "cancelledList.users", FieldValue.arrayRemove(userName));
            transaction.update(entrantDoc, "invitedEvents.events", FieldValue.arrayRemove(eventName));
            transaction.update(entrantDoc, "enrolledEvents.events", FieldValue.arrayUnion(eventName));

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Invite accepted!", Toast.LENGTH_SHORT).show();
            hideAllButtons();
            binding.btnBack.setVisibility(View.VISIBLE);
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /**
     * Declines an event invite (only if closed).
     */
    private void declineEventInvite(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventId);
        DocumentReference entrantDoc = db.collection("entrants").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || isOpen) {
                throw new FirebaseFirestoreException("This event is still open.",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(eventDoc, "cancelledList.users", FieldValue.arrayUnion(userName));
            transaction.update(eventDoc, "enrolledList.users", FieldValue.arrayRemove(userName));
            transaction.update(entrantDoc, "invitedEvents.events", FieldValue.arrayRemove(eventName));
            transaction.update(entrantDoc, "declinedEvents.events", FieldValue.arrayUnion(eventName));

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
