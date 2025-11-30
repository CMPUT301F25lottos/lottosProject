// FINAL, CORRECTED and SPECIFIC file for LotteryInfoScreen.java

package com.example.lottos.lottery;

import android.content.Context;
import android.content.SharedPreferences;
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

public class LotteryInfoScreen extends Fragment {

    private FragmentLotteryInfoScreenBinding binding;
    private String loggedInUserName;
    private boolean isAdmin = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentLotteryInfoScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
