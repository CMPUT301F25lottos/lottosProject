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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FieldValue;

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
        binding.btnAccept.setOnClickListener(v -> acceptEventInvite());
        binding.btnDecline.setOnClickListener(v -> declineEventInvite());
    }

    private void acceptEventInvite() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }


        String entrantName = currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "TestEntrant1";

        if (eventId == null || eventName == null) {
            Toast.makeText(getContext(), "Error: Missing event details.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference eventDocRef = db.collection("open events").document(eventId);
        DocumentReference entrantDocRef = db.collection("entrants").document(entrantName);

        setButtonsEnabled(false);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDocRef).getBoolean("IsOpen");
            if (isOpen == null || isOpen) {
                throw new FirebaseFirestoreException("This event is still open.",
                        FirebaseFirestoreException.Code.ABORTED);
            }


            transaction.update(eventDocRef, "enrolledList.users", FieldValue.arrayUnion(entrantName));
            transaction.update(eventDocRef, "cancelledList.users", FieldValue.arrayRemove(entrantName));


            transaction.update(entrantDocRef, "invitedEvents.events", FieldValue.arrayRemove(eventName));
            transaction.update(entrantDocRef, "enrolledEvents.events", FieldValue.arrayUnion(eventName));

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("Firestore", "Entrant successfully enrolled in event.");
            Toast.makeText(getContext(), "Invite Accepted!", Toast.LENGTH_SHORT).show();
            removeButtons();
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Transaction failed: ", e);
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            setButtonsEnabled(true);
        });
    }

    private void declineEventInvite() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Error: No user logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String entrantName = currentUser.getDisplayName() != null ?
                currentUser.getDisplayName() : "TestEntrant1";

        if (eventId == null || eventName == null) {
            Toast.makeText(getContext(), "Error: Missing event details.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference eventDocRef = db.collection("open events").document(eventId);
        DocumentReference entrantDocRef = db.collection("entrants").document(entrantName);

        setButtonsEnabled(false);

        db.runTransaction(transaction -> {
            Boolean isOpen = transaction.get(eventDocRef).getBoolean("IsOpen");
            if (isOpen == null || isOpen) {
                throw new FirebaseFirestoreException("This event is still open.",
                        FirebaseFirestoreException.Code.ABORTED);
            }


            transaction.update(eventDocRef, "cancelledList.users", FieldValue.arrayUnion(entrantName));
            transaction.update(eventDocRef, "enrolledList.users", FieldValue.arrayRemove(entrantName));


            transaction.update(entrantDocRef, "invitedEvents.events", FieldValue.arrayRemove(eventName));
            transaction.update(entrantDocRef, "declinedEvents.events", FieldValue.arrayUnion(eventName));

            return null;
        }).addOnSuccessListener(aVoid -> {
            Log.d("Firestore", "Entrant successfully declined event.");
            Toast.makeText(getContext(), "Invite Declined.", Toast.LENGTH_SHORT).show();
            removeButtons();
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Transaction failed: ", e);
            Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            setButtonsEnabled(true);
        });
    }

    private void removeButtons() {
        binding.btnAccept.setVisibility(View.GONE);
        binding.btnDecline.setVisibility(View.GONE);
    }

    private void setButtonsEnabled(boolean isEnabled) {
        binding.btnAccept.setEnabled(isEnabled);
        binding.btnDecline.setEnabled(isEnabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
