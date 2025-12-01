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

/**
 * A Fragment for administrators to view and manage all users in the system.
 * It fetches a list of all registered users from Firestore and displays them in a RecyclerView.
 * For each user, it shows their username and statistics about their event activity.
 * It also provides functionality to delete users from the system.
 */
public class ViewUsersScreen extends Fragment {

    /**
     * The binding object for the fragment's layout (fragment_view_users.xml).
     */
    private FragmentViewUsersBinding binding;
    /**
     * The adapter for the RecyclerView that displays the list of users.
     */
    private UserAdapter userAdapter;
    /**
     * The list of UserItem objects that backs the RecyclerView adapter.
     */
    private final List<UserAdapter.UserItem> userItemList = new ArrayList<>();
    /**
     * The instance of the Firebase Firestore database.
     */
    private FirebaseFirestore db;
    /**
     * The username of the currently logged-in administrator.
     */
    private String loggedInUserName;

    /**
     * Called to have the fragment instantiate its user interface view.
     * Initializes view binding and the Firestore instance.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root View for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentViewUsersBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView() has returned, but before any saved state has been restored into the view.
     * This method handles retrieving user credentials, setting up the RecyclerView and navigation, and fetching the user list.
     * @param view The View returned by onCreateView().
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
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

    /**
     * Initializes the RecyclerView, its LayoutManager, and the UserAdapter.
     * The adapter is provided with a listener to handle delete actions.
     */
    private void setupRecyclerView() {
        // Create the adapter and pass the listener for the delete action.
        userAdapter = new UserAdapter(userItemList, this::showDeleteConfirmationDialog);
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvUsers.setAdapter(userAdapter);
    }

    /**
     * Displays a confirmation dialog to the administrator before deleting a user.
     * @param userItem The user item that is being considered for deletion.
     */
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

    /**
     * Deletes a specified user's document from the "users" collection in Firestore.
     * On success, it refreshes the user list. On failure, it shows an error toast.
     * @param userItem The user item to be deleted.
     */
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

    /**
     * Fetches all documents from the "users" collection in Firestore. For each user, it calculates
     * the number of events they have joined and created, then populates the RecyclerView.
     */
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

    /**
     * Sets up click listeners for all navigation buttons in the bottom navigation bar.
     * This configures the admin-specific navigation actions.
     */
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
