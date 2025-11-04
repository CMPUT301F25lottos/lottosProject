package com.example.lottos;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lottos.databinding.FragmentSignupScreenBinding;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

/**
 *
 */
public class SignupScreen extends Fragment {

    private FragmentSignupScreenBinding binding;

    private FirebaseFirestore db;

    private CollectionReference entrantsRef;
    private CollectionReference organizersRef;

    private ArrayList<String> entrantUserNameArrayList;
    private ArrayList<String> organizerUserNameArrayList;

    private Entrant newEntrant;
    private Organizer newOrganizer;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSignupScreenBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        entrantUserNameArrayList = new ArrayList<>();
        organizerUserNameArrayList = new ArrayList<>();

        db = FirebaseFirestore.getInstance();
        entrantsRef = db.collection("entrants");
        organizersRef = db.collection("organizers");

        entrantsRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
            }
            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("userName");

                    entrantUserNameArrayList.add(name);
                }
            }
        });

        organizersRef.addSnapshotListener((value, error) -> {
            if (error != null) {
                Log.e("Firestore", error.toString());
            }
            if (value != null && !value.isEmpty()) {
                for (QueryDocumentSnapshot snapshot : value) {
                    String name = snapshot.getString("userName");

                    organizerUserNameArrayList.add(name);
                }
            }
        });


        binding.btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = binding.etName.getText().toString();
                String userName = binding.etUsername.getText().toString();
                String password = binding.etPassword.getText().toString();
                String email = binding.etEmail.getText().toString();
                String phoneNumber = binding.etPhoneNumber.getText().toString();
                String accountType = binding.etAccountType.getText().toString();

                UserInfo userInfo = new UserInfo(name,password,email,phoneNumber);

                if (accountType.equals("Entrant")) {
                    newEntrant = new Entrant(userName, userInfo);
                    if (entrantUserNameArrayList.contains(newEntrant.getUserName())
                            || organizerUserNameArrayList.contains(newOrganizer.getUserName())) {
                        NavHostFragment.findNavController(SignupScreen.this)
                                .navigate(SignupScreenDirections.actionSignupScreenToWelcomeScreen());
                    }
                    else {
                        DocumentReference docRef = entrantsRef.document(newEntrant.getUserName());
                        docRef.set(newEntrant);
                        SignupScreenDirections.ActionSignupScreenToHomeScreen action =
                                SignupScreenDirections.actionSignupScreenToHomeScreen(userName);
                        NavHostFragment.findNavController(SignupScreen.this)
                                .navigate(action);
                    }

                }
                else if (accountType.equals("Organizer")) {
                    newOrganizer = new Organizer(userName, userInfo);
                    if (organizerUserNameArrayList.contains(newOrganizer.getUserName())
                            || entrantUserNameArrayList.contains(newEntrant.getUserName())) {
                        NavHostFragment.findNavController(SignupScreen.this)
                                .navigate(SignupScreenDirections.actionSignupScreenToWelcomeScreen());
                    }
                    else {
                        DocumentReference docRef = organizersRef.document(newOrganizer.getUserName());
                        docRef.set(newOrganizer);
                        SignupScreenDirections.ActionSignupScreenToHomeScreen action =
                                SignupScreenDirections.actionSignupScreenToHomeScreen(userName);
                        NavHostFragment.findNavController(SignupScreen.this)
                                .navigate(action);
                    }
                }
                else {
                    NavHostFragment.findNavController(SignupScreen.this)
                            .navigate(SignupScreenDirections.actionSignupScreenToWelcomeScreen());
                }





            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}