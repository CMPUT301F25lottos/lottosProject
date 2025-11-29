package com.example.lottos.account;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.R;
import com.example.lottos.databinding.FragmentProfileScreenBinding;

public class ProfileScreen extends Fragment {
    private FragmentProfileScreenBinding binding;

    private static final String ADMIN_PASSWORD = "lottos_password";
    private UserProfileManager profileManager;
    private String userName;

    private SharedPreferences sharedPreferences;
    private boolean isAdmin = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileScreenBinding.inflate(inflater, container, false);
        // Initialize SharedPreferences here
        sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- ROBUST USERNAME LOADING (Original Strategy) ---
        // First, try to get userName from navigation arguments as intended.
        if (getArguments() != null) {
            userName = ProfileScreenArgs.fromBundle(getArguments()).getUserName();
        }
        // If it's still null (which happens on credential loss), get it from SharedPreferences as a fallback.
        if (userName == null) {
            userName = sharedPreferences.getString("userName", null);
        }
        // If it's *still* null, then there is a real problem.
        if (userName == null) {
            Toast.makeText(getContext(), "FATAL: Credentials lost. Please log in again.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).navigate(ProfileScreenDirections.actionProfileScreenToWelcomeScreen());
            return;
        }
        // --- END OF FIX ---

        profileManager = new UserProfileManager();
        loadUserRole();
        loadProfile();
        setupNavButtons();
        updateAdminButtonUI();

        binding.btnSwitchToAdmin.setOnClickListener(v -> {
            if (isAdmin) {
                switchToUserMode();
            } else {
                showAdminPasswordDialog();
            }
        });
    }

    private void showAdminPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Enter Admin Password");

        final EditText input = new EditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String password = input.getText().toString();
            checkAdminPassword(password);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void checkAdminPassword(String password) {
        if (password.equals(ADMIN_PASSWORD)) {
            switchToAdminMode();
        } else {
            Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadUserRole() {
        // We also check the SharedPreferences here to be certain
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);
    }

    private void updateAdminButtonUI() {
        if (isAdmin) {
            binding.btnSwitchToAdmin.setText("Switch to User");
        } else {
            binding.btnSwitchToAdmin.setText("Switch to Admin");
        }
    }

    private void switchToAdminMode() {
        isAdmin = true;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isAdmin", true);
        editor.apply();

        Toast.makeText(getContext(), "Admin mode activated!", Toast.LENGTH_SHORT).show();
        updateAdminButtonUI();
        setupNavButtons(); // Refresh nav buttons for admin mode
    }

    private void switchToUserMode() {
        isAdmin = false;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isAdmin", false);
        editor.apply();

        Toast.makeText(getContext(), "Switched to user mode.", Toast.LENGTH_SHORT).show();
        updateAdminButtonUI();
        setupNavButtons(); // Refresh nav buttons for user mode
    }

    private void loadProfile() {
        if (userName == null) return;
        profileManager.loadUserProfile(userName, new UserProfileManager.ProfileLoadListener() {
            @Override
            public void onProfileLoaded(String name, String email, String phone) {
                if (binding == null) return; // Check if view is still valid
                binding.tvUsername.setText("Username: " + userName);
                binding.tvName.setText("Name: " + name);
                binding.tvEmail.setText("Email: " + email);
                binding.tvPhoneNumber.setText("Phone Number: " + phone);
            }
            @Override
            public void onProfileNotFound() { Toast.makeText(getContext(), "User not found.", Toast.LENGTH_SHORT).show(); }
            @Override
            public void onError(String errorMessage) { Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show(); }
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
        if (userName == null) return;
        profileManager.deleteUser(userName, new UserProfileManager.DeleteListener() {
            @Override
            public void onDeleteSuccess() {
                Toast.makeText(getContext(), "Profile deleted successfully.", Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToWelcomeScreen());
            }
            @Override
            public void onDeleteFailure(String errorMessage) { Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show(); }
        });
    }

    private void setupNavButtons() {
        // --- Static Buttons that don't change based on role ---
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToHomeScreen(userName))
        );

        binding.btnEdit.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToEditProfileScreen(userName))
        );

        binding.btnLogout.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToWelcomeScreen()));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToNotificationScreen(userName))
        );

        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());


        // --- DYNAMIC BUTTONS based on the 'isAdmin' flag ---
        if (isAdmin) {
            // ADMIN MODE
            // 1. "Event History" icon becomes "View Users"
            binding.btnEventHistory.setImageResource(R.drawable.outline_article_person_24);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(ProfileScreenDirections.actionProfileScreenToViewUsersScreen(userName)));

            // 2. "Open Events" icon becomes "View Images"
            binding.btnOpenEvents.setImageResource(R.drawable.outline_add_photo_alternate_24); // This line changes the icon
            binding.btnOpenEvents.setOnClickListener(v ->
                    // NAVIGATE to the All Images screen
                    NavHostFragment.findNavController(this)
                            .navigate(ProfileScreenDirections.actionToAllImagesFragment(userName))

            );

            binding.btnOpenEvents.setImageResource(R.drawable.outline_add_photo_alternate_24); // This line changes the icon
            binding.btnOpenEvents.setOnClickListener(v ->
                    // THIS LINE IS PERFECT. IT PERFORMS THE NAVIGATION.
                    NavHostFragment.findNavController(this)
                            .navigate(ProfileScreenDirections.actionToAllImagesFragment(userName))
            );
        } else {
            // REGULAR USER MODE
            // 1. "Event History" icon is the standard history icon
            binding.btnEventHistory.setImageResource(R.drawable.ic_history);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(ProfileScreenDirections.actionProfileScreenToEventHistoryScreen(userName))
            );

            // 2. "Open Events" icon is the standard event icon
            binding.btnOpenEvents.setImageResource(R.drawable.ic_event); // This line sets the correct user icon
            binding.btnOpenEvents.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(ProfileScreenDirections.actionProfileScreenToOrganizerEventsScreen(userName))
            );
        }
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
