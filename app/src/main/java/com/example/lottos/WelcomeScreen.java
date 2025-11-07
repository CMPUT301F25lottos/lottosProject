package com.example.lottos;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lottos.databinding.FragmentWelcomeScreenBinding;

/**
 * This fragment that displays the initial welcome screen for the app.
 *
 * Role:  Introduces the application and provides
 * navigation to the login and signup screens, acting as the starting point
 * for both new and returning users.
 */
public class WelcomeScreen extends Fragment {

    private FragmentWelcomeScreenBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentWelcomeScreenBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(WelcomeScreen.this).navigate(WelcomeScreenDirections.actionWelcomeScreenToLoginScreen());
            }
        });

        binding.btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void  onClick(View v) {
                NavHostFragment.findNavController(WelcomeScreen.this).navigate(WelcomeScreenDirections.actionWelcomeScreenToSignupScreen());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

