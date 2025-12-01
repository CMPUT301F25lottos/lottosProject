package com.example.lottos;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lottos.auth.UserSession;
import com.example.lottos.databinding.FragmentWelcomeScreenBinding;

/**
 * A Fragment that displays the initial welcome screen for the app.
 *
 * Role: This screen serves as the primary entry point for the application. It has two main functions:
 * <ul>
 *     <li><b>Automatic Login:</b> If a user is already logged in (checked via {@link UserSession}),
 *         it verifies that their current device is still registered and authorized in Firestore.
 *         If the device is valid, it automatically navigates them to the {@code HomeScreen}.
 *         If the device is not found or the check fails, it logs the user out for security.</li>
 *     <li><b>New/Logged-Out Users:</b> If no user is logged in, it presents a simple
 *         UI with options to navigate to either the login screen or the signup screen.</li>
 * </ul>
 * It acts as a gatekeeper, directing users based on their authentication state.
 */
public class WelcomeScreen extends Fragment {

    private FragmentWelcomeScreenBinding binding;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout is inflated using view binding.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState
    ) {
        binding = FragmentWelcomeScreenBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView() has returned, but before any saved state has been restored.
     * This is where the core logic of the screen is executed. It checks the user's login status
     * and sets up the appropriate UI and navigation listeners.
     *
     * @param view The View returned by onCreateView().
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if there is a locally saved user session.
        if (UserSession.isLoggedIn(requireContext())) {
            String user = UserSession.getUser(requireContext());

            // Verify the current device installation ID against the user's document in Firestore.
            // This is a security measure to ensure the session is still valid on this specific device.
            com.google.firebase.installations.FirebaseInstallations.getInstance().getId()
                    .addOnSuccessListener(deviceId -> {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(user)
                                .collection("devices")
                                .document(deviceId)
                                .get()
                                .addOnSuccessListener(doc -> {
                                    // If the device ID exists in Firestore for this user, they are authenticated.
                                    if (doc.exists()) {
                                        NavHostFragment.findNavController(WelcomeScreen.this)
                                                .navigate(WelcomeScreenDirections.actionWelcomeScreenToHomeScreen(user));
                                    } else {
                                        UserSession.logout(requireContext());
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    // If the check fails, log the user out for safety.
                                    UserSession.logout(requireContext());
                                });
                    })
                    .addOnFailureListener(e -> {
                        // If getting the device ID fails, log the user out.
                        UserSession.logout(requireContext());
                    });

            return;
        }


        // If the user is not logged in, set up the navigation buttons for login and sign up.
        binding.btnLogin.setOnClickListener(v ->
                NavHostFragment.findNavController(WelcomeScreen.this)
                        .navigate(WelcomeScreenDirections.actionWelcomeScreenToLoginScreen()));

        binding.btnSignUp.setOnClickListener(v ->
                NavHostFragment.findNavController(WelcomeScreen.this)
                        .navigate(WelcomeScreenDirections.actionWelcomeScreenToSignupScreen()));
    }


    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * The view binding object is cleared here to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
