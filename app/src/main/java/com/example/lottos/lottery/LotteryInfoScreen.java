package com.example.lottos.lottery;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lottos.EntrantWaitListsScreenArgs;
import com.example.lottos.lottery.LotteryInfoScreenDirections;
import com.example.lottos.databinding.FragmentLotteryInfoScreenBinding;

/**
 * Fragment that displays information about how the lottery system works.
 * Role: Shows explanatory text to help users
 * understand event lotteries and provides navigation back to the home screen
 * while preserving the current user.
 */

public class LotteryInfoScreen extends Fragment {

    private FragmentLotteryInfoScreenBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentLotteryInfoScreenBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String userName = EntrantWaitListsScreenArgs.fromBundle(getArguments()).getUserName(); // pass the user info

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(LotteryInfoScreen.this)
                        .navigate(LotteryInfoScreenDirections
                                .actionLotteryInfoScreenToHomeScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(LotteryInfoScreen.this)
                        .navigate(LotteryInfoScreenDirections
                                .actionLotteryInfoScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(LotteryInfoScreen.this)
                        .navigate(LotteryInfoScreenDirections
                                .actionLotteryInfoScreenToProfileScreen(userName)));

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(LotteryInfoScreen.this)
                        .navigate(LotteryInfoScreenDirections
                                .actionLotteryInfoScreenToEventHistoryScreen(userName)));

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(LotteryInfoScreen.this)
                        .navigate(LotteryInfoScreenDirections
                                .actionLotteryInfoScreenToOrganizerEventsScreen(userName)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}