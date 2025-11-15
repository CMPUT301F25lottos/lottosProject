package com.example.lottos.account;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.account.EditProfileScreenArgs;
import com.example.lottos.account.EditProfileScreenDirections;
import com.example.lottos.databinding.FragmentEditProfileScreenBinding;

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditProfileScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = EditProfileScreenArgs.fromBundle(getArguments()).getUserName();
        profileManager = new UserProfileManager();

        loadUserInfo();

        binding.btnSave.setOnClickListener(v -> saveProfile());
        binding.btnCancel.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(EditProfileScreenDirections.actionEditProfileScreenToProfileScreen(userName));
        });

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(EditProfileScreen.this)
                        .navigate(EditProfileScreenDirections.actionEditProfileScreenToProfileScreen(userName))
        );

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(EditProfileScreen.this)
                        .navigate(EditProfileScreenDirections.actionEditProfileScreenToHomeScreen(userName))
        );


        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(EditProfileScreen.this)
                        .navigate(EditProfileScreenDirections.actionEditProfileScreenToNotificationScreen(userName))
        );
    }

    private void loadUserInfo() {
        profileManager.loadUserProfile(userName, new UserProfileManager.ProfileLoadListener() {
            @Override
            public void onProfileLoaded(String name, String email, String phone) {
                binding.etName.setText(name);
                binding.etEmail.setText(email);
                binding.etPhone.setText(phone);
            }

            @Override
            public void onProfileNotFound() {
                Toast.makeText(getContext(), "Profile not found.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
