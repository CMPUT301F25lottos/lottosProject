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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class ProfileScreen extends Fragment {

    private FragmentProfileScreenBinding binding;
    private FirebaseFirestore db;
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

        // Get username from previous screen
        userName = ProfileScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        // Load user profile info
        loadUserProfile();

        // Back button
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToHomeScreen(userName))
        );

        // Edit Profile button
        binding.btnEdit.setOnClickListener(v ->
                NavHostFragment.findNavController(ProfileScreen.this)
                        .navigate(ProfileScreenDirections.actionProfileScreenToEditProfileScreen(userName))
        );

        // Delete Profile button
        binding.btnDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    /**
     * Loads user info (name, email, phone) from Firestore
     */
    private void loadUserProfile() {
        DocumentReference usersDoc = db.collection("users").document(userName);
        //DocumentReference usersDoc = db.collection("users").document(userName);

        usersDoc.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // Get userInfo map from Firestore
                Map<String, Object> userInfo = (Map<String, Object>) snapshot.get("userInfo");
                if (userInfo != null) {
                    String name = (String) userInfo.get("name");
                    String email = (String) userInfo.get("email");
                    String phone = (String) userInfo.get("phoneNumber");

                    binding.tvAccountType.setText("Account Type: users");
                    binding.tvUsername.setText("Username: " + userName);
                    binding.tvName.setText("Name: " + name);
                    binding.tvEmail.setText("Email: " + (email != null ? email : "N/A"));
                    binding.tvPhoneNumber.setText("Phone Number: " + (phone != null ? phone : "N/A"));
                } else {
                    Toast.makeText(getContext(), "No profile data found.", Toast.LENGTH_SHORT).show();
                }
            } else {
                checkOrganizerProfile();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to load profile info.", Toast.LENGTH_SHORT).show()
        );
    }

    private void checkOrganizerProfile() {
        DocumentReference usersDoc = db.collection("users").document(userName);

        usersDoc.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                // Found in users
                Map<String, Object> userInfo = (Map<String, Object>) snapshot.get("userInfo");
                if (userInfo != null) {
                    String name = (String) userInfo.get("name");
                    String email = (String) userInfo.get("email");
                    String phone = (String) userInfo.get("phoneNumber");

                    binding.tvAccountType.setText("Account Type: users");
                    binding.tvUsername.setText("Username: " + userName);
                    binding.tvName.setText("Name: " + name);
                    binding.tvEmail.setText("Email: " + (email != null ? email : "N/A"));
                    binding.tvPhoneNumber.setText("Phone Number: " + (phone != null ? phone : "N/A"));
                } else {
                    Toast.makeText(getContext(), "No profile data found for users.", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Not found anywhere
                Toast.makeText(getContext(), "User not found in users.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to load users profile.", Toast.LENGTH_SHORT).show()
        );
    }

    /**
     * Confirm before deleting user profile
     */
    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Profile")
                .setMessage("Are you sure you want to permanently delete your account?")
                .setPositiveButton("Yes", (dialog, which) -> deleteProfile())
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the entrant's Firestore document
     */
    private void deleteProfile() {
        db.collection("users").document(userName).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile deleted successfully.", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(ProfileScreen.this)
                            .navigate(ProfileScreenDirections.actionProfileScreenToWelcomeScreen());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Failed to delete profile.", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
