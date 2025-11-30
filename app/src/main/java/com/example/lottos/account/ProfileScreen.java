package com.example.lottos.account;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.FirebaseFirestore;

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

        sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Argument and SharedPreferences loading for robust userName retrieval
        if (getArguments() != null) {
            userName = ProfileScreenArgs.fromBundle(getArguments()).getUserName();
        }
        if (userName == null) {
            userName = sharedPreferences.getString("userName", null);
        }
        if (userName == null) {
            Toast.makeText(getContext(), "FATAL: Credentials lost. Please log in again.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).navigate(ProfileScreenDirections.actionProfileScreenToWelcomeScreen());
            return;
        }

        FirebaseFirestore firestoreInstance = FirebaseFirestore.getInstance();
        UserProfileManager manager = new UserProfileManager(firestoreInstance);
        loadUserRole();
        loadProfile(); // Initial profile load
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
        setupNavButtons();
        loadProfile(); // Refresh the profile view to show Admin details
    }

    private void switchToUserMode() {
        isAdmin = false;
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isAdmin", false);
        editor.apply();

        Toast.makeText(getContext(), "Switched to user mode.", Toast.LENGTH_SHORT).show();
        updateAdminButtonUI();
        setupNavButtons();
        loadProfile(); // Refresh the profile view to show User details
    }

    private void loadProfile() {
        // This method now correctly decides which view to show
        if (isAdmin) {
            // If in admin mode, show the special admin view and stop.
            setAdminProfileView();
            return;
        }

        // If not admin, proceed to load the regular user's profile.
        if (userName == null) return;
        profileManager.loadUserProfile(userName, new UserProfileManager.ProfileLoadListener() {
            @Override
            public void onProfileLoaded(String name, String email, String phone) {
                if (binding == null) return;
                setUserProfileView(name, email, phone);
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

    /**
     * Sets the UI to display "Admin" credentials and hides user-specific buttons.
     */
    private void setAdminProfileView() {
        if (binding == null) return;
        binding.tvUsername.setText("Mode: Admin");
        binding.tvName.setText("Name: Admin");
        binding.tvEmail.setText("Email: Admin");
        binding.tvPhoneNumber.setText("Phone Number: Admin");

        // Hide buttons that don't apply to the admin view
        binding.btnEdit.setVisibility(View.GONE);
        binding.btnDelete.setVisibility(View.GONE);
    }

    /**
     * Sets the UI to display the regular user's profile information.
     */
    private void setUserProfileView(String name, String email, String phone) {
        if (binding == null) return;
        binding.tvUsername.setText("Username: " + userName);
        binding.tvName.setText("Name: " + name);
        binding.tvEmail.setText("Email: " + email);
        binding.tvPhoneNumber.setText("Phone Number: " + phone);

        // Make sure user-specific buttons are visible
        binding.btnEdit.setVisibility(View.VISIBLE);
        binding.btnDelete.setVisibility(View.VISIBLE);
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
            binding.btnOpenEvents.setImageResource(R.drawable.outline_add_photo_alternate_24);
            binding.btnOpenEvents.setOnClickListener(v ->
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
            binding.btnOpenEvents.setImageResource(R.drawable.ic_event);
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
