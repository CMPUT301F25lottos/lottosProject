package com.example.lottos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentEditProfileScreenBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class EditProfileScreen extends Fragment {

    private FragmentEditProfileScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentEditProfileScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        userName = EditProfileScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        loadUserInfo();

        binding.btnSave.setOnClickListener(v -> updateUserInfo());
        binding.btnCancel.setOnClickListener(v ->
                NavHostFragment.findNavController(EditProfileScreen.this)
                        .navigate(EditProfileScreenDirections.actionEditProfileScreenToProfileScreen(userName))
        );
    }

    private void loadUserInfo() {
        DocumentReference entrantDoc = db.collection("entrants").document(userName);
        entrantDoc.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Map<String, Object> userInfo = (Map<String, Object>) snapshot.get("userInfo");
                if (userInfo != null) {
                    binding.etName.setText((String) userInfo.get("name"));
                    binding.etEmail.setText((String) userInfo.get("email"));
                    binding.etPhone.setText((String) userInfo.get("phoneNumber"));
                }
            }
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to load profile info.", Toast.LENGTH_SHORT).show()
        );
    }

    private void updateUserInfo() {
        String name = binding.etName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String phone = binding.etPhone.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(getContext(), "Name and email are required.", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference entrantDoc = db.collection("entrants").document(userName);
        entrantDoc.update(
                "userInfo.name", name,
                "userInfo.email", email,
                "userInfo.phoneNumber", phone
        ).addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            NavHostFragment.findNavController(EditProfileScreen.this)
                    .navigate(EditProfileScreenDirections.actionEditProfileScreenToProfileScreen(userName));
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(), "Failed to update profile.", Toast.LENGTH_SHORT).show()
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
