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

import com.example.lottos.databinding.FragmentLoginScreenBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/**
 * Fragment that handles user login functionality.
 * Role: Provides the interface and logic for
 * user authentication using Firestore-stored credentials.
 * Responsibilities:
 * - Authenticates users by verifying username and password stored in Firestore.
 * - Navigates to the Home screen upon successful login.
 * - Displays appropriate error messages for invalid or missing credentials.
 */

public class LoginScreen extends Fragment {

    private FragmentLoginScreenBinding binding;
    private FirebaseFirestore db;
    private CollectionReference usersRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Back button
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(LoginScreen.this)
                        .navigate(LoginScreenDirections.actionLoginScreenToWelcomeScreen()));

        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");

        binding.btnLogin.setOnClickListener(v -> {
            String userName = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (userName.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter both username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            checkUserLogin(userName, password);
        });
    }

    private void checkUserLogin(String userName, String password) {
        DocumentReference userDoc = usersRef.document(userName);
        userDoc.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e("Firestore", "Error getting user document", task.getException());
                Toast.makeText(getContext(), "Login failed. Try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentSnapshot doc = task.getResult();
            if (doc == null || !doc.exists()) {
                Toast.makeText(getContext(), "Username not found", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                Map<String, Object> userInfoMap = (Map<String, Object>) doc.get("userInfo");
                if (userInfoMap == null || userInfoMap.get("password") == null) {
                    Toast.makeText(getContext(), "User data missing", Toast.LENGTH_SHORT).show();
                    return;
                }

                String storedPassword = userInfoMap.get("password").toString();
                if (storedPassword.equals(password)) {
                    navigateToHome(userName);
                } else {
                    Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("Firestore", "Error reading user data for " + userName, e);
                Toast.makeText(getContext(), "Data format error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToHome(String userName) {
        try {
            LoginScreenDirections.ActionLoginScreenToHomeScreen action =
                    LoginScreenDirections.actionLoginScreenToHomeScreen(userName);
            NavHostFragment.findNavController(LoginScreen.this).navigate(action);
        } catch (Exception e) {
            Log.e("Navigation", "Navigation failed", e);
            Toast.makeText(getContext(), "Navigation error — check nav_graph argument name.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
