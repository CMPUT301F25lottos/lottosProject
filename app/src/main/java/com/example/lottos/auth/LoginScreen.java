package com.example.lottos.auth;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.R;
import com.example.lottos.databinding.FragmentLoginScreenBinding;

/**
 * Handles user login UI.
 *
 * Role: Provides interface for users to enter login credentials.
 * Delegates authentication logic to UserAuthenticator.
 */
public class LoginScreen extends Fragment {
    private FragmentLoginScreenBinding binding;
    private UserAuthenticator authenticator;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoginScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authenticator = new UserAuthenticator();

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(LoginScreen.this)
                        .navigate(LoginScreenDirections.actionLoginScreenToWelcomeScreen()));

        binding.btnLogin.setOnClickListener(v -> {
            String userName = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (userName.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please enter both username and password", Toast.LENGTH_SHORT).show();
                return;
            }

            authenticator.checkUserLogin(userName, password, new UserAuthenticator.AuthListener() {
                @Override
                public void onSuccess(String userName) {

                    if (!isAdded() || getView() == null) return;

                    if (NavHostFragment.findNavController(LoginScreen.this)
                            .getCurrentDestination().getId() != R.id.LoginScreen) {
                        return;
                    }

                    UserSession.saveUser(requireContext(), userName);

                    Toast.makeText(getContext(), "Welcome back, " + userName + "!", Toast.LENGTH_SHORT).show();

                    NavHostFragment.findNavController(LoginScreen.this)
                            .navigate(LoginScreenDirections.actionLoginScreenToHomeScreen(userName));
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
