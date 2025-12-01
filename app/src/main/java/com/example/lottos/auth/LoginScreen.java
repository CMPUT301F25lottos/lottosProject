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
 * A Fragment that provides a user interface for user login.
 * This screen allows users to enter their username and password. It uses the
 * {@code UserAuthenticator} class to verify credentials against the backend.
 * On successful authentication, it creates a user session and navigates to the home screen.
 * It provides feedback to the user for both successful and failed login attempts.
 */
public class LoginScreen extends Fragment {
    /**
     * The binding object for the fragment's layout (fragment_login_screen.xml).
     */
    private FragmentLoginScreenBinding binding;
    /**
     * The authenticator object responsible for handling the login logic.
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
        binding = FragmentLoginScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView() has returned, but before any saved state has been restored into the view.
     * This method initializes the authenticator and sets up click listeners for the login and back buttons.
     *
     * @param view               The View returned by onCreateView().
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
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
                /**
                 * Called upon successful authentication.
                 * Saves the user session and navigates to the home screen.
                 * @param userName The username of the authenticated user.
                 */
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


                /**
                 * Called upon failed authentication.
                 * Displays an error message to the user.
                 * @param errorMessage The reason for the authentication failure.
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
