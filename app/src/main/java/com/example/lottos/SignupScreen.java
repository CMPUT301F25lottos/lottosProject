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

        db = FirebaseFirestore.getInstance();
        entrantsRef = db.collection("entrants");

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
    }

    private void handleSignup() {
        String name = binding.etName.getText().toString().trim();
        String userName = binding.etUsername.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phoneNumber = binding.etPhoneNumber.getText().toString().trim();

        if (name.isEmpty() || userName.isEmpty() || password.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Please fill in all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        UserInfo userInfo = new UserInfo(name, password, email, phoneNumber);

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
