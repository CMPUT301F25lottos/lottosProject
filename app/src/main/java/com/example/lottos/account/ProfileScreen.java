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
import com.example.lottos.auth.UserSession;
import com.example.lottos.databinding.FragmentProfileScreenBinding;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * A Fragment to display and manage a user's profile information.
 * It allows users to view their details, edit their profile, or delete their account.
 * This screen also provides a mechanism to switch between a regular user view and an administrator view,
 * which changes the available navigation options and displayed information.
 */
public class ProfileScreen extends Fragment {
    private FragmentProfileScreenBinding binding;

    private static final String ADMIN_PASSWORD = "lottos_password";
    private UserProfileManager profileManager;
    private String userName;

    private SharedPreferences sharedPreferences;
    private boolean isAdmin = false;

    /**
     * Called to have the fragment instantiate its user interface view.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileScreenBinding.inflate(inflater, container, false);

        sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored in to the view.
     * This is where user data is loaded and UI listeners are set up.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


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
        profileManager = new UserProfileManager(firestoreInstance);

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

    /**
     * Displays a dialog prompting the user to enter the administrator password.
     */
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

    /**
     * Verifies the entered password against the stored admin password.
     * @param password The password entered by the user.
     */
    private void checkAdminPassword(String password) {
        if (password.equals(ADMIN_PASSWORD)) {
            switchToAdminMode();
        } else {
            Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Loads the current user role (admin or regular user) from SharedPreferences.
     */
    private void loadUserRole() {
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);
    }

    /**
     * Updates the text of the "Switch to Admin/User" button based on the current mode.
     */
    private void updateAdminButtonUI() {
        if (isAdmin) {
            binding.btnSwitchToAdmin.setText("Switch to User");
        } else {
            binding.btnSwitchToAdmin.setText("Switch to Admin");
        }
    }

    /**
     * Switches the application state to administrator mode.
     * This updates SharedPreferences and refreshes the UI to reflect admin privileges.
     */
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

    /**
     * Switches the application state back to regular user mode.
     * This updates SharedPreferences and refreshes the UI to reflect user privileges.
     */
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

    /**
     * Loads the profile information from the data source.
     * It displays an admin-specific view if in admin mode, otherwise fetches and displays the user's data.
     */
    private void loadProfile() {
        if (isAdmin) {
            setAdminProfileView();
            return;
        }
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

        binding.btnEdit.setVisibility(View.GONE);
        binding.btnDelete.setVisibility(View.GONE);
    }

    /**
     * Sets the UI to display the regular user's profile information.
     * @param name The user's full name.
     * @param email The user's email address.
     * @param phone The user's phone number.
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

    /**
     * Displays a confirmation dialog before deleting the user's profile.
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to permanently delete your account?")
                .setPositiveButton("Yes", (dialog, which) -> deleteUser())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Initiates the user deletion process and handles the result.
     * On success, it logs the user out and navigates to the welcome screen.
     */
    private void deleteUser() {
        if (userName == null) return;

        profileManager.deleteUser(userName, new UserProfileManager.DeleteListener() {
            @Override
            public void onDeleteSuccess() {
                UserSession.logout(requireContext());

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove("isAdmin");
                editor.apply();

                Toast.makeText(getContext(), "Account deleted successfully.", Toast.LENGTH_SHORT).show();

                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToWelcomeScreen());
            }



            @Override
            public void onDeleteFailure(String errorMessage) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sets up click listeners for all navigation and action buttons on the screen.
     * The navigation targets for some buttons change depending on whether the user is in admin mode.
     */
    private void setupNavButtons() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToHomeScreen(userName))
        );

        binding.btnEdit.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToEditProfileScreen(userName))
        );



        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToNotificationScreen(userName))
        );

        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());


        if (isAdmin) {
            binding.btnEventHistory.setImageResource(R.drawable.outline_article_person_24);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(ProfileScreenDirections.actionProfileScreenToViewUsersScreen(userName)));

            binding.btnOpenEvents.setImageResource(R.drawable.outline_add_photo_alternate_24);
            binding.btnOpenEvents.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(ProfileScreenDirections.actionToAllImagesFragment(userName))

            );
        } else {
            binding.btnEventHistory.setImageResource(R.drawable.ic_history);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(ProfileScreenDirections.actionProfileScreenToEventHistoryScreen(userName))
            );

            binding.btnOpenEvents.setImageResource(R.drawable.ic_event);
            binding.btnOpenEvents.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(ProfileScreenDirections.actionProfileScreenToOrganizerEventsScreen(userName))
            );
        }
    }

    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * This is where the view binding is cleaned up to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
