package com.example.lottos.admin;

import android.app.AlertDialog;
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
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lottos.R;
import com.example.lottos.databinding.FragmentViewUsersBinding;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewUsersScreen extends Fragment {

    private FragmentViewUsersBinding binding;
    private UserAdapter userAdapter;
    private final List<UserAdapter.UserItem> userItemList = new ArrayList<>();
    private FirebaseFirestore db;
    private String loggedInUserName;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentViewUsersBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Robustly get user credentials
        if (getArguments() != null) {
            loggedInUserName = ViewUsersScreenArgs.fromBundle(getArguments()).getUserName();
        }
        if (loggedInUserName == null) {
            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
            loggedInUserName = sharedPreferences.getString("userName", null);
        }
        if (loggedInUserName == null) {
            Toast.makeText(getContext(), "Critical: Session lost. Please log in again.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).popBackStack(R.id.WelcomeScreen, false);
            return;
        }

        setupRecyclerView();
        setupNavButtons();
        fetchUsernames();
    }

    private void setupRecyclerView() {
        // Create the adapter and pass the listener for the delete action.
        userAdapter = new UserAdapter(userItemList, this::showDeleteConfirmationDialog);
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvUsers.setAdapter(userAdapter);
    }

    private void showDeleteConfirmationDialog(UserAdapter.UserItem userItem) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete '" + userItem.userId + "'? This action cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    // User confirmed, call the delete method
                    deleteUserFromFirestore(userItem);
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void deleteUserFromFirestore(UserAdapter.UserItem userItem) {
        db.collection("users").document(userItem.userId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), "User '" + userItem.userId + "' deleted successfully.", Toast.LENGTH_SHORT).show();
                    fetchUsernames(); // Refresh the list after deletion
                })
                .addOnFailureListener(e -> {
                    if (getContext() == null) return;
                    Toast.makeText(getContext(), "Error deleting user: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void fetchUsernames() {
        db.collection("users").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                userItemList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    UserAdapter.UserItem userItem = new UserAdapter.UserItem(document.getId());

                    // --- LOGIC FOR COUNTING EVENTS ---
                    Object enrolledMapObj = document.get("enrolledEvents");
                    Object notSelectedMapObj = document.get("notSelectedEvents");
                    Object organizedMapObj = document.get("organizedEvents");

                    int joinedCount = 0;
                    if (enrolledMapObj instanceof java.util.Map) {
                        List<?> events = (List<?>) ((java.util.Map) enrolledMapObj).get("events");
                        if (events != null) joinedCount += events.size();
                    }
                    if (notSelectedMapObj instanceof java.util.Map) {
                        List<?> events = (List<?>) ((java.util.Map) notSelectedMapObj).get("events");
                        if (events != null) joinedCount += events.size();
                    }
                    userItem.joinedEventCount = joinedCount;

                    int createdCount = 0;
                    if (organizedMapObj instanceof java.util.Map) {
                        List<?> events = (List<?>) ((java.util.Map) organizedMapObj).get("events");
                        if (events != null) createdCount = events.size();
                    }
                    userItem.createdEventCount = createdCount;

                    userItemList.add(userItem);
                }
                userAdapter.notifyDataSetChanged();
            } else {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to fetch users.", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupNavButtons() {

        binding.btnHome.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(ViewUsersScreenDirections.actionViewUsersScreenToHomeScreen(loggedInUserName)));

        binding.btnProfile.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(ViewUsersScreenDirections.actionViewUsersScreenToProfileScreen(loggedInUserName)));

        binding.btnNotification.setOnClickListener(v -> NavHostFragment.findNavController(this)
                .navigate(ViewUsersScreenDirections.actionViewUsersScreenToNotificationScreen(loggedInUserName)));

        binding.btnEventHistory.setImageResource(R.drawable.outline_article_person_24); // Stays as "View Users" icon
        binding.btnEventHistory.setOnClickListener(v -> {
            Toast.makeText(getContext(), "User list reloaded.", Toast.LENGTH_SHORT).show();
            fetchUsernames();
        });

        binding.btnOpenEvents.setImageResource(R.drawable.outline_add_photo_alternate_24);
        binding.btnOpenEvents.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(ViewUsersScreenDirections.actionToAllImagesFragment(loggedInUserName));
        });
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
