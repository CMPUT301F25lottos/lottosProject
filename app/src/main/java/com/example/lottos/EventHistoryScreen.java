package com.example.lottos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentEventHistoryScreenBinding;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;


/**
 * this fragment displays the entrant user's event history.
 * Role: Reads historical event data from Firestore
 * (based on the user's stored lists) and shows it in a simple list with navigation
 * back to the home screen.
 */

public class EventHistoryScreen extends Fragment {

    private FragmentEventHistoryScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEventHistoryScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userName = EventHistoryScreenArgs.fromBundle(getArguments()).getUserName();
        db = FirebaseFirestore.getInstance();

        loadHistory();

        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToHomeScreen(userName)));

        binding.btnNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToNotificationScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(EventHistoryScreenDirections.actionEventHistoryScreenToProfileScreen(userName)));
    }

    private void loadHistory() {
        db.collection("users").document(userName).get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()) {
                        List<String> history = (List<String>) snapshot.get("waitListedEvents.events");
                        if (history == null || history.isEmpty()) {
                            Toast.makeText(getContext(), "No past events found.", Toast.LENGTH_SHORT).show();
                        } else {
                            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                    getContext(),
                                    android.R.layout.simple_list_item_1,
                                    history
                            );
                            binding.lvEventHistory.setAdapter(adapter);
                        }
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error loading history", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
