package com.example.lottos;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lottos.databinding.FragmentHomeScreenBinding;

/**
 *
 */
public class HomeScreen extends Fragment {

    private FragmentHomeScreenBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentHomeScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String userName = HomeScreenArgs.fromBundle(getArguments()).getUserName();


        binding.btnOpenEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(HomeScreen.this).navigate(HomeScreenDirections.actionHomeScreenToOrganizerEventsScreen(userName));
            }
        });

        binding.btnOrgEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void  onClick(View v) {
                NavHostFragment.findNavController(HomeScreen.this).navigate(HomeScreenDirections.actionHomeScreenToEntrantEventsScreen(userName));
            }
        });

        binding.btnWaitLists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void  onClick(View v) {
                NavHostFragment.findNavController(HomeScreen.this).navigate(HomeScreenDirections.actionHomeScreenToEntrantWaitListsScreen(userName));
            }
        });

        binding.btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void  onClick(View v) {
                NavHostFragment.findNavController(HomeScreen.this).navigate(HomeScreenDirections.actionHomeScreenToProfileScreen(userName));
            }
        });

        binding.btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void  onClick(View v) {
                NavHostFragment.findNavController(HomeScreen.this).navigate(HomeScreenDirections.actionHomeScreenToLotteryInfoScreen(userName));
            }
        });

        binding.btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void  onClick(View v) {
                NavHostFragment.findNavController(HomeScreen.this).navigate(HomeScreenDirections.actionHomeScreenToWelcomeScreen());
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}