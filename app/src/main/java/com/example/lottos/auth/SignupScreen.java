package com.example.lottos.auth;

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
 * A Fragment that provides a user interface for new user registration.
 * Role: This screen collects signup information such as username, display name, password, and contact details.
 * It performs basic input validation and then delegates the account creation process to the
 * {@code UserAuthenticator}. Upon successful registration, it creates a user session
 * and navigates the user to the application's home screen.
 */
public class SignupScreen extends Fragment {
    /**
     * The binding object for the fragment's layout (fragment_signup_screen.xml).
     */
    private FragmentSignupScreenBinding binding;
    /**
     * The authenticator object responsible for handling the registration logic with the backend.
     */
    private UserAuthenticator authenticator;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout is inflated and the view binding is initialized.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSignupScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView() has returned, but before any saved state has been restored into the view.
     * This method initializes the authenticator and sets up click listeners for the signup and back buttons.
     *
     * @param view               The View returned by onCreateView().
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
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
                /**
                 * Called upon successful registration.
                 * Saves the new user's session and navigates to the home screen.
                 * @param userName The username of the newly created user.
                 */
                @Override
                public void onSuccess(String userName) {
                    UserSession.saveUser(requireContext(), userName);

                    Toast.makeText(getContext(), "Account created successfully!", Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(SignupScreen.this)
                            .navigate(SignupScreenDirections.actionSignupScreenToHomeScreen(userName));
                }


                /**
                 * Called upon a registration failure.
                 * Displays an error message to the user (e.g., username already taken).
                 * @param errorMessage The reason for the registration failure.
                 */
                @Override
                public void onFailure(String errorMessage) {
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    /**
     * Called when the view previously created by onCreateView() has been detached from the fragment.
     * This is where the view binding is cleaned up to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
