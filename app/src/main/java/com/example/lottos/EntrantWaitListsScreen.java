package com.example.lottos;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.lottos.databinding.FragmentEntrantWaitListsScreenBinding;

/**
 *
 */
public class EntrantWaitListsScreen extends Fragment {

    private FragmentEntrantWaitListsScreenBinding binding;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentEntrantWaitListsScreenBinding.inflate(inflater,container,false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String userName = EntrantWaitListsScreenArgs.fromBundle(getArguments()).getUserName(); // pass the user info

        binding.btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NavHostFragment.findNavController(EntrantWaitListsScreen.this).navigate(EntrantWaitListsScreenDirections.actionEntrantWaitListsScreenToHomeScreen(userName));
            }
        });

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}