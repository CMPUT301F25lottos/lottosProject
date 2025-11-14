package com.example.lottos;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentProfileScreenBinding;

/**
 * UI-only fragment for displaying and managing user profile.
 * Logic is fully delegated to UserProfileManager.
 */
public class ProfileScreen extends Fragment {

    private FragmentProfileScreenBinding binding;
    private UserProfileManager profileManager;
    private String userName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = ProfileScreenArgs.fromBundle(getArguments()).getUserName();
        profileManager = new UserProfileManager();

        loadProfile();

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToHomeScreen(userName))
        );

        binding.btnEdit.setOnClickListener(v ->
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToEditProfileScreen(userName))
        );

        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void loadProfile() {
        profileManager.loadUserProfile(userName, new UserProfileManager.ProfileLoadListener() {
            @Override
            public void onProfileLoaded(String name, String email, String phone) {
                binding.tvUsername.setText("Username: " + userName);
                binding.tvName.setText("Name: " + name);
                binding.tvEmail.setText("Email: " + email);
                binding.tvPhoneNumber.setText("Phone Number: " + phone);
            }

            @Override
            public void onProfileNotFound() {
                Toast.makeText(getContext(), "User not found.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String errorMessage) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to permanently delete your account?")
                .setPositiveButton("Yes", (dialog, which) -> deleteUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteUser() {
        profileManager.deleteUser(userName, new UserProfileManager.DeleteListener() {
            @Override
            public void onDeleteSuccess() {
                Toast.makeText(getContext(), "Profile deleted successfully.", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToWelcomeScreen());
            }

            @Override
            public void onDeleteFailure(String errorMessage) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
