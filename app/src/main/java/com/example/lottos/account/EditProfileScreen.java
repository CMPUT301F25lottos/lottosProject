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
 * A Fragment that allows the user to view and update their profile information.
 * This screen loads existing profile data from Firestore into editable fields,
 * validates user input, saves the changes back to the "users" collection,
 * and handles navigation back to the main profile screen upon completion or cancellation.
 */
public class EditProfileScreen extends Fragment {

    private FragmentEditProfileScreenBinding binding;
    private UserProfileManager profileManager;
    private String userName;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout is inflated and view binding is initialized.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEditProfileScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored in to the view.
     * This method initializes the profile manager, retrieves the username from navigation arguments,
     * and sets up the UI listeners for navigation and action buttons.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
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

    /**
     * Loads the current user's profile data from the data source and populates the input fields.
     * Handles cases where the profile is not found or an error occurs during fetching.
     */
    private void loadUserInfo() {
        profileManager.loadUserProfile(userName, new UserProfileManager.ProfileLoadListener() {
            /**
             * Populates the EditText fields with the loaded user data.
             * @param name The user's name.
             * @param email The user's email.
             * @param phone The user's phone number.
             */
            @Override
            public void onProfileLoaded(String name, String email, String phone) {
                if (binding == null) return;
                binding.etName.setText(name);
                binding.etEmail.setText(email);
                binding.etPhone.setText(phone);
            }
            /**
             * Displays a toast message if the user's profile could not be found.
             */
            @Override
            public void onProfileNotFound() {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Profile not found.", Toast.LENGTH_SHORT).show();
                }
            }

            /**
             * Displays a toast message if an error occurs while loading the profile.
             * @param errorMessage The error message to display.
             */
            @Override
            public void onError(String errorMessage) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Gathers the updated information from the input fields, validates it,
     * and requests the profile manager to save it to Firestore.
     * Provides feedback to the user on success or failure and navigates back on success.
     */
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
                    /**
                     * Called on successful profile update. Displays a confirmation toast
                     * and navigates back to the profile screen.
                     */
                    @Override
                    public void onUpdateSuccess() {
                        Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                        // REVERTED: Pass the userName back to the profile screen
                        NavHostFragment.findNavController(EditProfileScreen.this)
                                .navigate(EditProfileScreenDirections.actionEditProfileScreenToProfileScreen(userName));
                    }

                    /**
                     * Called on profile update failure. Displays an error message.
                     * @param errorMessage The error message to display.
                     */
                    @Override
                    public void onUpdateFailure(String errorMessage) {
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    /**
     * Sets up the OnClickListener for all navigation and action buttons on the screen.
     * This includes the save, cancel, and bottom navigation bar buttons.
     */
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
