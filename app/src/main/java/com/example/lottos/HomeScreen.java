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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeScreen extends Fragment {

    private FragmentHomeScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        if (getArguments() != null) {
            userName = HomeScreenArgs.fromBundle(getArguments()).getUserName();
        }

        // Hide all buttons initially
        hideAllButtons();

        // Check Firestore to determine user type
        checkUserType(userName);

        // Button navigation setup
        setupButtonListeners();
    }

    private void checkUserType(String userName) {
        DocumentReference entrantDoc = db.collection("entrants").document(userName);
        DocumentReference organizerDoc = db.collection("organizers").document(userName);

        entrantDoc.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                showEntrantButtons();
            } else {
                organizerDoc.get().addOnSuccessListener(orgSnapshot -> {
                    if (orgSnapshot.exists()) {
                        showOrganizerButtons();
                    } else {
                        Toast.makeText(getContext(), "User not found.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Error fetching user type.", Toast.LENGTH_SHORT).show()
        );
    }

    private void setupButtonListeners() {
        // Entrant Events
        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(HomeScreenDirections.actionHomeScreenToEntrantEventsScreen(userName))
        );

        // Waitlists
        binding.btnWaitLists.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(HomeScreenDirections.actionHomeScreenToEntrantWaitListsScreen(userName))
        );

        // Organizer Events
        binding.btnOrgEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(HomeScreenDirections.actionHomeScreenToOrganizerEventsScreen(userName))
        );

        // Profile
        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(HomeScreenDirections.actionHomeScreenToProfileScreen(userName))
        );

        // Lottery Info
        binding.btnInfo.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(HomeScreenDirections.actionHomeScreenToLotteryInfoScreen(userName))
        );

        // Logout → Welcome
        binding.btnLogout.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(HomeScreenDirections.actionHomeScreenToWelcomeScreen())
        );
    }

    private void showEntrantButtons() {
        binding.btnOpenEvents.setVisibility(View.VISIBLE);
        binding.btnWaitLists.setVisibility(View.VISIBLE);
        binding.btnProfile.setVisibility(View.VISIBLE);
        binding.btnInfo.setVisibility(View.VISIBLE);
        binding.btnLogout.setVisibility(View.VISIBLE);
    }

    private void showOrganizerButtons() {
        binding.btnOrgEvents.setVisibility(View.VISIBLE);
        binding.btnProfile.setVisibility(View.VISIBLE);
        binding.btnInfo.setVisibility(View.VISIBLE);
        binding.btnLogout.setVisibility(View.VISIBLE);
    }

    private void hideAllButtons() {
        binding.btnOpenEvents.setVisibility(View.GONE);
        binding.btnWaitLists.setVisibility(View.GONE);
        binding.btnOrgEvents.setVisibility(View.GONE);
        binding.btnProfile.setVisibility(View.GONE);
        binding.btnInfo.setVisibility(View.GONE);
        binding.btnLogout.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
