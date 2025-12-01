// FINAL, CORRECTED and SPECIFIC file for LotteryInfoScreen.java

package com.example.lottos.lottery;

import android.content.Context;import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.R;
import com.example.lottos.databinding.FragmentLotteryInfoScreenBinding;
import com.example.lottos.lottery.LotteryInfoScreenArgs;
import com.example.lottos.lottery.LotteryInfoScreenDirections;

/**
 * A Fragment that displays static informational content about how the lottery system works.
 *
 * Role: This class provides a simple, read-only screen to educate users on the
 * application's processes. It also includes the standard navigation bar to allow
 * the user to move to other parts of the app, such as their profile, notifications,
 * or back to the previous screen. The navigation options are adjusted based on
 * whether the logged-in user is an administrator.
 */
public class LotteryInfoScreen extends Fragment {

    private FragmentLotteryInfoScreenBinding binding;
    private String loggedInUserName;
    private boolean isAdmin = false;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout is inflated from its XML definition.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentLotteryInfoScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored into the view.
     * This method is where the fragment's logic is initialized. It retrieves the user's session information
     * and sets up the navigation button listeners.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);

        if (getArguments() != null) {
            loggedInUserName = LotteryInfoScreenArgs.fromBundle(getArguments()).getUserName();
        }
        if (loggedInUserName == null) {
            loggedInUserName = sharedPreferences.getString("userName", null);
        }

        if (loggedInUserName == null) {
            Toast.makeText(getContext(), "Critical: Session lost. Please log in again.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).popBackStack(R.id.WelcomeScreen, false);
            return;
        }

        setupNavButtons();
    }

    /**
     * Sets up the OnClickListeners for all navigation buttons in the fragment's layout.
     * This includes buttons for navigating back, to the user profile, to notifications,
     * and to other sections, with behavior adjusted for admin users.
     */
    private void setupNavButtons() {


        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp());

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(LotteryInfoScreenDirections.actionLotteryInfoScreenToProfileScreen(loggedInUserName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(LotteryInfoScreenDirections.actionLotteryInfoScreenToNotificationScreen(loggedInUserName)));



        if (isAdmin) {
            binding.btnEventHistory.setImageResource(R.drawable.outline_article_person_24);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(LotteryInfoScreenDirections.actionLotteryInfoScreenToViewUsersScreen(loggedInUserName))
            );

            binding.btnOpenEvents.setOnClickListener(v ->
                    Toast.makeText(getContext(), "Admin action placeholder.", Toast.LENGTH_SHORT).show());
        } else {

            binding.btnEventHistory.setImageResource(R.drawable.ic_history);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(LotteryInfoScreenDirections.actionLotteryInfoScreenToEventHistoryScreen(loggedInUserName))
            );

            binding.btnOpenEvents.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(LotteryInfoScreenDirections.actionLotteryInfoScreenToOrganizerEventsScreen(loggedInUserName))
            );
        }
    }

    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * This is where the view binding object is cleared to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
