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

            List<String> userWaitlist = (List<String>) eventSnapshot.get("waitList.users");
            if (userWaitlist == null) userWaitlist = new ArrayList<>();

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

            // Show "Run Lottery" button only if current user is the organizer
            if (organizer != null && organizer.equals(userName)) {
                binding.btnLottery.setVisibility(View.VISIBLE);

                List<String> finalWaitlist = new ArrayList<>(userWaitlist);
                binding.btnLottery.setOnClickListener(v -> {
                    binding.btnLottery.setEnabled(false);
                    Toast.makeText(getContext(), "🎲 Lottery started... please wait", Toast.LENGTH_SHORT).show();
                    runLottery(eventDoc, finalWaitlist);
                    binding.btnLottery.postDelayed(() -> binding.btnLottery.setEnabled(true), 2000);
                });
            }

            // Check user status for waitlist/join/etc.
            usersDoc.get().addOnSuccessListener(usersSnapshot -> {
                Map<String, Object> invitedMap = (Map<String, Object>) usersSnapshot.get("invitedEvents");
                List<String> invitedEvents = invitedMap != null ? (List<String>) invitedMap.get("events") : new ArrayList<>();

                Map<String, Object> waitlistedMap = (Map<String, Object>) usersSnapshot.get("waitListedEvents");
                List<String> waitListedEvents = waitlistedMap != null ? (List<String>) waitlistedMap.get("events") : new ArrayList<>();

                boolean isAlreadyWaitlisted = waitListedEvents.contains(eventName);
                binding.btnBack.setVisibility(View.VISIBLE);

                if (isOpen != null && isOpen) {
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
                    if (invitedEvents.contains(eventName)) {
                        binding.btnAccept.setVisibility(View.VISIBLE);
                        binding.btnDecline.setVisibility(View.VISIBLE);
                    } else if (waitListedEvents.contains(eventName)) {
                        Toast.makeText(getContext(), "Event closed — not selected.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Event closed — you were not in the waitlist.", Toast.LENGTH_SHORT).show();
                    }
                }

            }).addOnFailureListener(e ->
                    Toast.makeText(getContext(), "Failed to load user info.", Toast.LENGTH_SHORT).show());
        });
    }

    /** Manual trigger for lottery by organizer */
    private void runLottery(DocumentReference eventDoc, List<String> waitList) {
        if (waitList.isEmpty()) {
            Toast.makeText(getContext(), "No entrants in waitlist to run the lottery.", Toast.LENGTH_SHORT).show();
            return;
        }

        eventDoc.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            Long selectionCapLong = snapshot.getLong("selectionCap");
            int selectionCap = selectionCapLong != null ? selectionCapLong.intValue() : waitList.size();

            LotterySystem lottery = new LotterySystem(eventName);
            ArrayList<String> selected = lottery.Selected(new ArrayList<>(waitList));

            if (selected.size() > selectionCap)
                selected = new ArrayList<>(selected.subList(0, selectionCap));

            eventDoc.update("selectedList.users", selected)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(),
                                "✅ Lottery completed! Selected.",
                                Toast.LENGTH_LONG).show();
                    })
                    .addOnFailureListener(e ->
                            Log.e("Firestore", "Error updating selectedList", e));
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

            transaction.update(eventDoc, "waitList.users", FieldValue.arrayUnion(userName));
            transaction.update(usersDoc, "waitListedEvents.events", FieldValue.arrayUnion(eventName));
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
            transaction.update(eventDoc, "waitList.users", FieldValue.arrayRemove(userName));
            transaction.update(usersDoc, "waitListedEvents.events", FieldValue.arrayRemove(eventName));
            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Left waitlist successfully!", Toast.LENGTH_SHORT).show();
            binding.btnJoin.setText("Join Waitlist");
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to leave waitlist.", Toast.LENGTH_SHORT).show());
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
