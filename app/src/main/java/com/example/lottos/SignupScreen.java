package com.example.lottos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentSignupScreenBinding;

/**
 * Handles user registration UI.
 *
 * Role: Collects signup input fields, validates them,
 * and delegates account creation to UserAuthenticator.
 */
public class SignupScreen extends Fragment {

    private FragmentSignupScreenBinding binding;
    private UserAuthenticator authenticator;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignupScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authenticator = new UserAuthenticator();

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

            authenticator.registerUser(userName, displayName, password, email, phoneNumber, new UserAuthenticator.AuthListener() {
                @Override
                public void onSuccess(String userName) {
                    Toast.makeText(getContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(SignupScreen.this)
                            .navigate(SignupScreenDirections.actionSignupScreenToHomeScreen(userName));
                }

                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
