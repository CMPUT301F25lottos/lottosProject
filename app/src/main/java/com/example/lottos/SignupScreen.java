package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentSignupScreenBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * This fragment handles new user registration.
 *
 * Role: Provides the interface to create a new user account.
 * Collects signup input, validates required fields, creates a corresponding user document in Firestore
 * with initialized event lists and navigates to the home screen on success.
 */

public class SignupScreen extends Fragment {

    private FragmentSignupScreenBinding binding;
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignupScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(SignupScreen.this)
                        .navigate(SignupScreenDirections.actionSignupScreenToWelcomeScreen()));

        binding.btnSignup.setOnClickListener(v -> {
            String userName = binding.etUsername.getText().toString().trim();
            String displayName = binding.etDisplayName.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String email = binding.etEmail.getText().toString().trim();
            String phoneNumber = binding.etPhoneNumber.getText().toString().trim();

            if (userName.isEmpty() || displayName.isEmpty() || password.isEmpty() || email.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            createUser(userName, displayName, password, email, phoneNumber);
        });
    }

    private void createUser(String userName, String displayName, String password, String email, String phoneNumber) {
        DocumentReference userDoc = usersRef.document(userName);

        userDoc.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Toast.makeText(getContext(), "Username already taken. Please choose another.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Helper inline: create {"events": []} map
            Map<String, Object> createEventsMap = new HashMap<>();
            createEventsMap.put("events", new ArrayList<String>());

            // userInfo
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("displayName", displayName);
            userInfo.put("email", email);
            userInfo.put("name", displayName);
            userInfo.put("password", password);
            userInfo.put("phoneNumber", phoneNumber);

            // main user data map
            Map<String, Object> userData = new HashMap<>();
            userData.put("userName", userName);
            userData.put("userInfo", userInfo);

            // each event map must be a new instance — clone it each time
            userData.put("closedEvents", new HashMap<>(createEventsMap));
            userData.put("declinedEvents", new HashMap<>(createEventsMap));
            userData.put("enrolledEvents", new HashMap<>(createEventsMap));
            userData.put("selectedEvents", new HashMap<>(createEventsMap));
            userData.put("notSelectedEvents", new HashMap<>(createEventsMap));
            userData.put("organizedEvents", new HashMap<>(createEventsMap));
            userData.put("waitListedEvents", new HashMap<>(createEventsMap));

            userDoc.set(userData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();
                        NavHostFragment.findNavController(SignupScreen.this)
                                .navigate(SignupScreenDirections.actionSignupScreenToHomeScreen(userName));
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error creating user: " + e.getMessage(), e);
                        Toast.makeText(getContext(), "Error creating account. Please try again.", Toast.LENGTH_SHORT).show();
                    });

        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Error checking username: " + e.getMessage(), e);
            Toast.makeText(getContext(), "Error checking username. Try again.", Toast.LENGTH_SHORT).show();
        });
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
