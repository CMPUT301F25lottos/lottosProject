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

        // Default hide everything until we know what to show
        hideAllButtons();

        setupEntrantUI(userName);
    }

    private void setupEntrantUI(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference usersDoc = db.collection("users").document(userName);

        eventDoc.get().addOnSuccessListener(eventSnapshot -> {
            Boolean isOpen = eventSnapshot.getBoolean("IsOpen");

            usersDoc.get().addOnSuccessListener(usersSnapshot -> {
                Map<String, Object> invitedMap = (Map<String, Object>) usersSnapshot.get("invitedEvents");
                List<String> invitedEvents = invitedMap != null ? (List<String>) invitedMap.get("events") : new ArrayList<>();

                Map<String, Object> waitlistedMap = (Map<String, Object>) usersSnapshot.get("waitListedEvents");
                List<String> waitListedEvents = waitlistedMap != null ? (List<String>) waitlistedMap.get("events") : new ArrayList<>();

                // Determine if user is already in waitlist
                boolean isAlreadyWaitlisted = waitListedEvents.contains(eventName);

                // Always show Back button
                binding.btnBack.setVisibility(View.VISIBLE);

                if (isOpen != null && isOpen) {
                    // Event open
                    if (isAlreadyWaitlisted) {
                        binding.btnJoin.setText("Leave Waitlist");
                        binding.btnJoin.setVisibility(View.VISIBLE);
                    } else {
                        binding.btnJoin.setText("Join Waitlist");
                        binding.btnJoin.setVisibility(View.VISIBLE);
                    }

                    binding.btnJoin.setOnClickListener(v -> {
                        if (binding.btnJoin.getText().toString().equals("Join Waitlist")) {
                            joinWaitlist(userName);
                        } else {
                            leaveWaitlist(userName);
                        }
                    });
                } else {
                    // Event is closed
                    if (invitedEvents != null && invitedEvents.contains(eventName)) {
                        // users was invited
                        binding.btnAccept.setVisibility(View.VISIBLE);
                        binding.btnDecline.setVisibility(View.VISIBLE);
                        binding.btnBack.setVisibility(View.VISIBLE);

                    } else if (waitListedEvents != null && waitListedEvents.contains(eventName)) {
                        // users was waitlisted but not invited
                        Toast.makeText(getContext(),
                                "This event is closed. You were not selected.",
                                Toast.LENGTH_SHORT).show();
                        binding.btnBack.setVisibility(View.VISIBLE);

                    } else {
                        // users was never in the waitlist
                        Toast.makeText(getContext(),
                                "The waitlist is closed and you were not a part of it.",
                                Toast.LENGTH_SHORT).show();
                        binding.btnBack.setVisibility(View.VISIBLE);
                    }
                }

            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to load users info.", Toast.LENGTH_SHORT).show());
        });
    }

    private void joinWaitlist(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference usersDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || !isOpen) {
                throw new FirebaseFirestoreException("This event is closed.", FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(eventDoc, "waitList.users.users", com.google.firebase.firestore.FieldValue.arrayUnion(userName));
            transaction.update(usersDoc, "waitListedEvents.events", com.google.firebase.firestore.FieldValue.arrayUnion(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Joined waitlist!", Toast.LENGTH_SHORT).show();
            binding.btnJoin.setText("Leave Waitlist");
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("Firestore", "Join waitlist failed", e);
        });
    }

    private void leaveWaitlist(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference usersDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            transaction.update(eventDoc, "waitList.users.users", com.google.firebase.firestore.FieldValue.arrayRemove(userName));
            transaction.update(usersDoc, "waitListedEvents.events", com.google.firebase.firestore.FieldValue.arrayRemove(eventName));
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
        DocumentReference usersDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || isOpen) {
                throw new FirebaseFirestoreException("This event is still open.",
                        FirebaseFirestoreException.Code.ABORTED);
            }

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

    /**
     * Declines an event invite (only if closed).
     */
    private void declineEventInvite(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventId);
        DocumentReference usersDoc = db.collection("users").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || isOpen) {
                throw new FirebaseFirestoreException("This event is still open.",
                        FirebaseFirestoreException.Code.ABORTED);
            }

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}