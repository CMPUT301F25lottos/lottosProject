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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class SignupScreen extends Fragment {

    private FragmentSignupScreenBinding binding;
    private FirebaseFirestore db;
    private CollectionReference entrantsRef;
    private CollectionReference organizersRef;

    private ArrayList<String> entrantUserNameArrayList;
    private ArrayList<String> organizerUserNameArrayList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignupScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(SignupScreen.this).navigate(SignupScreenDirections.actionSignupScreenToWelcomeScreen());
            }
        });


        entrantUserNameArrayList = new ArrayList<>();
        organizerUserNameArrayList = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        entrantsRef = db.collection("entrants");
        organizersRef = db.collection("organizers");

        // Load usernames once
        loadUsernames();

        binding.btnSignup.setOnClickListener(v -> handleSignup());
    }

    private void loadUsernames() {
        entrantsRef.get().addOnSuccessListener(value -> {
            for (QueryDocumentSnapshot snapshot : value) {
                String name = snapshot.getString("userName");
                if (name != null) entrantUserNameArrayList.add(name);
            }
        });

        organizersRef.get().addOnSuccessListener(value -> {
            for (QueryDocumentSnapshot snapshot : value) {
                String name = snapshot.getString("userName");
                if (name != null) organizerUserNameArrayList.add(name);
            }
        });
    }

    private void handleSignup() {
        String name = binding.etName.getText().toString().trim();
        String userName = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phoneNumber = binding.etPhoneNumber.getText().toString().trim();
        String accountType = binding.etAccountType.getText().toString().trim();

        if (name.isEmpty() || userName.isEmpty() || password.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() || accountType.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        UserInfo userInfo = new UserInfo(name, password, email, phoneNumber);

        //todo: implement email verification

        if (accountType.equalsIgnoreCase("Entrant")) {
            if (entrantUserNameArrayList.contains(userName) || organizerUserNameArrayList.contains(userName)) {
                Toast.makeText(getContext(), "Username already exists!", Toast.LENGTH_SHORT).show();
                return;
            }

            Entrant newEntrant = new Entrant(userName, userInfo);
            entrantsRef.document(userName).set(newEntrant)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Entrant account created!", Toast.LENGTH_SHORT).show();
                        navigateToHome(userName);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error creating entrant", e);
                        Toast.makeText(getContext(), "Failed to create account.", Toast.LENGTH_SHORT).show();
                    });

        } else if (accountType.equalsIgnoreCase("Organizer")) {
            if (organizerUserNameArrayList.contains(userName) || entrantUserNameArrayList.contains(userName)) {
                Toast.makeText(getContext(), "Username already exists!", Toast.LENGTH_SHORT).show();
                return;
            }

            Organizer newOrganizer = new Organizer(userName, userInfo);
            organizersRef.document(userName).set(newOrganizer)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Organizer account created!", Toast.LENGTH_SHORT).show();
                        navigateToHome(userName);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("Firestore", "Error creating organizer", e);
                        Toast.makeText(getContext(), "Failed to create account.", Toast.LENGTH_SHORT).show();
                    });

        } else {
            Toast.makeText(getContext(), "Please enter a valid account type: 'Entrant' or 'Organizer'.", Toast.LENGTH_SHORT).show();
        }

    }

    private void navigateToHome(String userName) {
        SignupScreenDirections.ActionSignupScreenToHomeScreen action =
                SignupScreenDirections.actionSignupScreenToHomeScreen(userName);
        NavHostFragment.findNavController(SignupScreen.this).navigate(action);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
