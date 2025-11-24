package com.example.lottos.account;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.account.ProfileScreenArgs;
import com.example.lottos.account.ProfileScreenDirections;
import com.example.lottos.databinding.FragmentProfileScreenBinding;
import com.example.lottos.home.HomeScreen;
import com.example.lottos.home.HomeScreenDirections;

/**
 * UI-only fragment for displaying and managing user profile.
 * Logic is fully delegated to UserProfileManager.
 */

public class ProfileScreen extends Fragment {

    private FragmentProfileScreenBinding binding;

    private static final String ADMIN_PASSWORD = "lottos_password";
    private UserProfileManager profileManager;
    private String userName;

    // To store the user's role
    private SharedPreferences sharedPreferences;
    private boolean isAdmin = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentProfileScreenBinding.inflate(inflater, container, false);

        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = ProfileScreenArgs.fromBundle(getArguments()).getUserName();
        profileManager = new UserProfileManager();

        loadProfile();
        setupNavButtons();

        // Load the current role and update the UI accordingly
        loadUserRole();
        updateAdminButtonUI();

        binding.btnSwitchToAdmin.setOnClickListener(v -> {
            if (isAdmin) {
                // If the user is already an admin, switch them back to a normal user
                switchToUserMode();
            } else {
                // If they are a normal user, show the password dialog
                showAdminPasswordDialog();
            }
        });
    }

    private void showAdminPasswordDialog() {
        // Create an alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Admin Password");

        // Set up the input
        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = input.getText().toString();
            checkAdminPassword(password);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void checkAdminPassword(String password) {
        if (password.equals(ADMIN_PASSWORD)) {
            // Correct password, switch to admin mode
            switchToAdminMode();
        } else {
            Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserRole() {
        // Retrieve the current role from SharedPreferences, defaulting to false (not an admin)
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);
    }

    private void updateAdminButtonUI() {
        if (isAdmin) {
            binding.btnSwitchToAdmin.setText("Switch to User");
            // You can also change the button color if you want
            // binding.btnSwitchToAdmin.setBackgroundColor(getResources().getColor(R.color.your_user_color));
        } else {
            binding.btnSwitchToAdmin.setText("Switch to Admin");
            // binding.btnSwitchToAdmin.setBackgroundColor(getResources().getColor(R.color.your_admin_red_color));
        }
        // TODO: Add logic here to change the visibility of other menu/nav buttons based on 'isAdmin'
    }

    private void switchToAdminMode() {
        isAdmin = true;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isAdmin", true);
        editor.apply();

        Toast.makeText(getContext(), "Admin mode activated!", Toast.LENGTH_SHORT).show();
        updateAdminButtonUI();
    }

    private void switchToUserMode() {
        isAdmin = false;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isAdmin", false);
        editor.apply();

        Toast.makeText(getContext(), "Switched to user mode.", Toast.LENGTH_SHORT).show();
        updateAdminButtonUI();
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

    private void setupNavButtons() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToHomeScreen(userName))
        );

        binding.btnEdit.setOnClickListener(v ->
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToEditProfileScreen(userName))
        );

        binding.btnLogout.setOnClickListener(v ->
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToWelcomeScreen()));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToNotificationScreen(userName))
        );

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToOrganizerEventsScreen(userName))
        );

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToEventHistoryScreen(userName))
        );

        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
