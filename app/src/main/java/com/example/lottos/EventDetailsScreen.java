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
        DocumentReference entrantDoc = db.collection("entrants").document(userName);

        eventDoc.get().addOnSuccessListener(eventSnapshot -> {
            Boolean isOpen = eventSnapshot.getBoolean("IsOpen");

            entrantDoc.get().addOnSuccessListener(entrantSnapshot -> {
                Map<String, Object> waitlistedMap = (Map<String, Object>) entrantSnapshot.get("waitListedEvents");
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
                    // Event closed (keep your lottery logic)
                    if (waitListedEvents.contains(eventName)) {
                        Toast.makeText(getContext(), "Event closed. You were not selected.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Waitlist is closed.", Toast.LENGTH_SHORT).show();
                    }
                }

            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to load entrant info.", Toast.LENGTH_SHORT).show());
        });
    }

    private void joinWaitlist(String userName) {
        DocumentReference eventDoc = db.collection("open events").document(eventName);
        DocumentReference entrantDoc = db.collection("entrants").document(userName);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDoc).getBoolean("IsOpen");
            if (isOpen == null || !isOpen) {
                throw new FirebaseFirestoreException("This event is closed.", FirebaseFirestoreException.Code.ABORTED);
            }

            transaction.update(eventDoc, "waitList.entrants.users", com.google.firebase.firestore.FieldValue.arrayUnion(userName));
            transaction.update(entrantDoc, "waitListedEvents.events", com.google.firebase.firestore.FieldValue.arrayUnion(eventName));
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
        DocumentReference entrantDoc = db.collection("entrants").document(userName);

        db.runTransaction(transaction -> {
            transaction.update(eventDoc, "waitList.entrants.users", com.google.firebase.firestore.FieldValue.arrayRemove(userName));
            transaction.update(entrantDoc, "waitListedEvents.events", com.google.firebase.firestore.FieldValue.arrayRemove(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Left waitlist.", Toast.LENGTH_SHORT).show();
            binding.btnJoin.setText("Join Waitlist");
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to leave waitlist.", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
