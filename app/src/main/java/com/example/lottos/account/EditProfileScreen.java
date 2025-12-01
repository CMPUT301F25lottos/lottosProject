package com.example.lottos.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentEditProfileScreenBinding;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * This fragment is the main fragment that lets a user view and update their profile information.
 * Role: Loads profile data from Firestore into editable fields,
 * validates changes, updates the "users" collection and navigates back to the profile screen.
 */

public class EditProfileScreen extends Fragment {

    private FragmentEditProfileScreenBinding binding;
    private UserProfileManager profileManager;
    private String userName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (getArguments() != null) {
            userName = EditProfileScreenArgs.fromBundle(getArguments()).getUserName();
        }
        if (userName == null) {
            Toast.makeText(getContext(), "User not found, returning to profile.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).popBackStack();
            return;
        }

        FirebaseFirestore firestoreInstance = FirebaseFirestore.getInstance();
        profileManager = new UserProfileManager(firestoreInstance);
        setupNavButtons();
    }

    private void loadUserInfo() {
        profileManager.loadUserProfile(userName, new UserProfileManager.ProfileLoadListener() {
            @Override
            public void onProfileLoaded(String name, String email, String phone) {
                if (binding == null) return;
                binding.etName.setText(name);
                binding.etEmail.setText(email);
                binding.etPhone.setText(phone);
            }
            @Override
            public void onProfileNotFound() {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Profile not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveProfile() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Name and email are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        profileManager.updateUserProfile(
                userName, name, email, phone,
                new UserProfileManager.ProfileUpdateListener() {
                    @Override
                    public void onUpdateSuccess() {
                        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        // REVERTED: Pass the userName back to the profile screen
                        NavHostFragment.findNavController(EditProfileScreen.this)
                                .navigate(EditProfileScreenDirections.actionEditProfileScreenToProfileScreen(userName));
                    }

                    @Override
                    public void onUpdateFailure(String errorMessage) {
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setupNavButtons() {
        binding.btnSave.setOnClickListener(v -> saveProfile());

        // REVERTED: All navigation calls now correctly pass the userName
        binding.btnCancel.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditProfileScreenDirections.actionEditProfileScreenToProfileScreen(userName))
        );

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditProfileScreenDirections.actionEditProfileScreenToProfileScreen(userName))
        );

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditProfileScreenDirections.actionEditProfileScreenToHomeScreen(userName))
        );


        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditProfileScreenDirections.actionEditProfileScreenToNotificationScreen(userName))
        );

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditProfileScreenDirections.actionEditProfileScreenToOrganizerEventsScreen(userName))
        );

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EditProfileScreenDirections.actionEditProfileScreenToEventHistoryScreen(userName))
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
