package com.example.lottos;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lottos.databinding.FragmentLoginScreenBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
public class LoginScreen extends Fragment {

    private FragmentLoginScreenBinding binding;

    private FirebaseFirestore db;

    private CollectionReference entrantsRef;
    private CollectionReference organizersRef;

    private ArrayList<String> entrantUserNameArrayList;
    private ArrayList<String> organizerUserNameArrayList;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentLoginScreenBinding.inflate(inflater, container, false);
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


        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = binding.etUsername.getText().toString();
                String password = binding.etPassword.getText().toString();

                if (entrantUserNameArrayList.contains(userName)) {
                    DocumentReference docRef = entrantsRef.document(userName);
                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot snapshot = task.getResult();
                                if (snapshot.exists()) {
                                    Map<String,Object> checkUserInfo = (Map<String, Object>) snapshot.get("userInfo");
                                    String checkPassword = (String) checkUserInfo.get("password");
                                    if (checkPassword.equals(password)) {
                                        LoginScreenDirections.ActionLoginScreenToHomeScreen action =
                                                LoginScreenDirections.actionLoginScreenToHomeScreen(userName);
                                        NavHostFragment.findNavController(LoginScreen.this)
                                                .navigate(action);
                                    }
                                    else {
                                        NavHostFragment.findNavController(LoginScreen.this)
                                                .navigate(LoginScreenDirections.actionLoginScreenToWelcomeScreen());
                                    }
                                }
                                else {
                                    NavHostFragment.findNavController(LoginScreen.this)
                                            .navigate(LoginScreenDirections.actionLoginScreenToWelcomeScreen());
                                }
                            }
                            else {
                                NavHostFragment.findNavController(LoginScreen.this)
                                        .navigate(LoginScreenDirections.actionLoginScreenToWelcomeScreen());
                            }
                        }
                    });
                }

                if (organizerUserNameArrayList.contains(userName)) {
                    DocumentReference docRef = organizersRef.document(userName);
                    docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                            if (task.isSuccessful()) {
                                DocumentSnapshot snapshot = task.getResult();
                                if (snapshot.exists()) {
                                    Map<String,Object> checkUserInfo = (Map<String, Object>) snapshot.get("userInfo");
                                    String checkPassword = (String) checkUserInfo.get("password");
                                    if (checkPassword.equals(password)) {
                                        LoginScreenDirections.ActionLoginScreenToHomeScreen action =
                                                LoginScreenDirections.actionLoginScreenToHomeScreen(userName);
                                        NavHostFragment.findNavController(LoginScreen.this)
                                                .navigate(action);
                                    }
                                    else {
                                        NavHostFragment.findNavController(LoginScreen.this)
                                                .navigate(LoginScreenDirections.actionLoginScreenToWelcomeScreen());
                                    }
                                }
                                else {
                                    NavHostFragment.findNavController(LoginScreen.this)
                                            .navigate(LoginScreenDirections.actionLoginScreenToWelcomeScreen());
                                }
                            }
                            else {
                                NavHostFragment.findNavController(LoginScreen.this)
                                        .navigate(LoginScreenDirections.actionLoginScreenToWelcomeScreen());
                            }
                        }
                    });
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