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

public class LoginScreen extends Fragment {

    private FragmentLoginScreenBinding binding;
    private FirebaseFirestore db;

    private CollectionReference entrantsRef;
    private CollectionReference organizersRef;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Back button navigation
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(LoginScreen.this)
                        .navigate(LoginScreenDirections.actionLoginScreenToWelcomeScreen()));

        db = FirebaseFirestore.getInstance();
        entrantsRef = db.collection("entrants");
        organizersRef = db.collection("organizers");

        // Login button logic
        binding.btnLogin.setOnClickListener(v -> {
            String userName = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (userName.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter both username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            checkEntrantLogin(userName, password);
        });
    }

    private void checkEntrantLogin(String userName, String password) {
        DocumentReference entrantDoc = entrantsRef.document(userName);
        entrantDoc.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e("Firestore", "Error getting entrant document", task.getException());
                Toast.makeText(getContext(), "Login failed. Try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentSnapshot doc = task.getResult();
            if (doc == null || !doc.exists()) {
                checkOrganizerLogin(userName, password);
                return;
            }

            // Try to read nested "userInfo" safely
            Object userInfoObj = doc.get("userInfo");
            if (userInfoObj == null) {
                Toast.makeText(getContext(), "User data missing in Firestore.", Toast.LENGTH_SHORT).show();
                Log.e("Firestore", "Entrant has no userInfo field: " + userName);
                return;
            }

            // Manual extraction to avoid nullpointer when casting
            try {
                String storedPassword = doc.getString("userInfo.password");
                if (storedPassword == null) {
                    // fallback if nested map instead of dot path
                    Map<String, Object> userInfoMap = (Map<String, Object>) doc.get("userInfo");
                    if (userInfoMap != null && userInfoMap.get("password") != null) {
                        storedPassword = userInfoMap.get("password").toString();
                    }
                }

                if (storedPassword != null && storedPassword.equals(password)) {
                    navigateToHome(userName);
                } else {
                    Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("Firestore", "Error parsing entrant userInfo for " + userName, e);
                Toast.makeText(getContext(), "Data format error in Firestore", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkOrganizerLogin(String userName, String password) {
        DocumentReference organizerDoc = organizersRef.document(userName);
        organizerDoc.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e("Firestore", "Error getting organizer document", task.getException());
                Toast.makeText(getContext(), "Login failed. Try again.", Toast.LENGTH_SHORT).show();
                return;
            }

            DocumentSnapshot doc = task.getResult();
            if (doc == null || !doc.exists()) {
                Toast.makeText(getContext(), "Username or password invalid", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String storedPassword = doc.getString("userInfo.password");
                if (storedPassword == null) {
                    Map<String, Object> userInfoMap = (Map<String, Object>) doc.get("userInfo");
                    if (userInfoMap != null && userInfoMap.get("password") != null) {
                        storedPassword = userInfoMap.get("password").toString();
                    }
                }

                if (storedPassword != null && storedPassword.equals(password)) {
                    navigateToHome(userName);
                } else {
                    Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e("Firestore", "Error parsing organizer userInfo for " + userName, e);
                Toast.makeText(getContext(), "Data format error in Firestore", Toast.LENGTH_SHORT).show();
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

