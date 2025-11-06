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
        // Ensure the username is unique
        DocumentReference userDoc = usersRef.document(userName);
        userDoc.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Toast.makeText(getContext(), "Username already taken. Please choose another.", Toast.LENGTH_SHORT).show();
            } else {
                // Create the User object
                UserInfo userInfo = new UserInfo(displayName, password, email, phoneNumber);
                User newUser = new User(userName, userInfo);

                userDoc.set(newUser)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();
                            NavHostFragment.findNavController(SignupScreen.this)
                                    .navigate(SignupScreenDirections.actionSignupScreenToHomeScreen(userName));
                        })
                        .addOnFailureListener(e -> {
                            Log.e("Firestore", "Error creating user", e);
                            Toast.makeText(getContext(), "Error creating account. Please try again.", Toast.LENGTH_SHORT).show();
                        });
            }
        }).addOnFailureListener(e -> {
            Log.e("Firestore", "Error checking username", e);
            Toast.makeText(getContext(), "Error checking username. Try again.", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
