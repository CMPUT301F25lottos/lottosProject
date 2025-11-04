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
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;
import java.util.Objects;

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

        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(LoginScreen.this).navigate(LoginScreenDirections.actionLoginScreenToWelcomeScreen());
            }
        });

        db = FirebaseFirestore.getInstance();
        entrantsRef = db.collection("entrants");
        organizersRef = db.collection("organizers");

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
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    UserInfo userInfo = doc.get("userInfo", UserInfo.class);
                    if (userInfo != null && password.equals(userInfo.getPassword())) {
                        navigateToHome(userName);
                    } else {
                        Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    checkOrganizerLogin(userName, password);
                }
            } else {
                Log.e("Firestore", "Error getting entrant document", task.getException());
                Toast.makeText(getContext(), "Login failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkOrganizerLogin(String userName, String password) {
        DocumentReference organizerDoc = organizersRef.document(userName);
        organizerDoc.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                if (doc.exists()) {
                    UserInfo userInfo = doc.get("userInfo", UserInfo.class);
                    if (userInfo != null && password.equals(userInfo.getPassword())) {
                        navigateToHome(userName);
                    } else {
                        Toast.makeText(getContext(), "Username or password invalid", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Username or password invalid", Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.e("Firestore", "Error getting organizer document", task.getException());
                Toast.makeText(getContext(), "Login failed. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void navigateToHome(String userName) {
        LoginScreenDirections.ActionLoginScreenToHomeScreen action =
                LoginScreenDirections.actionLoginScreenToHomeScreen(userName);
        NavHostFragment.findNavController(LoginScreen.this).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
