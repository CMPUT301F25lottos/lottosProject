package com.example.lottos.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lottos.R;
import com.example.lottos.databinding.FragmentNotificationScreenBinding;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Fragment for displaying and managing a user's notifications.
 *
 * Role: This screen is responsible for showing a list of notifications to the user.
 * It tailors its behavior based on whether the user is a standard user or an administrator.
 * Key responsibilities include:
 * <ul>
 *     <li>Fetching and displaying notifications relevant to the current user (or all notifications for an admin).</li>
 *     <li>Providing UI elements for navigation to other parts of the app.</li>
 *     <li>Allowing users to delete their notifications.</li>
 *     <li>Offering a switch to toggle the visibility of the notification list.</li>
 *     <li>Adjusting the UI and available actions based on admin status (e.g., showing a "Send Notification" button).</li>
 * </ul>
 * It implements the NotificationAdapter.Listener interface to handle user interactions with the list items.
 */
public class NotificationScreen extends Fragment implements NotificationAdapter.Listener {

    private FragmentNotificationScreenBinding binding;
    private NotificationManager notificationManager;
    private NotificationAdapter adapter;
    private final List<NotificationAdapter.NotificationItem> notificationItems = new ArrayList<>();
    private String userName;
    private boolean isAdmin = false;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout is inflated and the view binding object is initialized.
     *
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The root view for the fragment's UI.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored into the view.
     * This is where the fragment's logic is initialized, including setting up UI components,
     * retrieving user session data, and loading initial data.
     *
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPreferences = requireActivity()
                .getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        // If using view binding:
        SwitchMaterial stShowNotification = binding.stShowNotification;

        isAdmin = sharedPreferences.getBoolean("isAdmin", false);

        if (getArguments() != null) {
            userName = NotificationScreenArgs.fromBundle(getArguments()).getUserName();
        }

        if (userName == null) {
            userName = sharedPreferences.getString("userName", null);
        }

        if (userName == null) {
            Toast.makeText(getContext(), "Credentials not found. Please log in.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this)
                    .popBackStack(R.id.WelcomeScreen, false);
            return;
        }

        // 🔹 Set title + send button based on admin
        if (isAdmin) {
            binding.tvTitle.setText("All Notifications");
            binding.btnSendNotification.setVisibility(View.VISIBLE);
        } else {
            binding.tvTitle.setText("My Notifications");
            binding.btnSendNotification.setVisibility(View.VISIBLE);
        }

        setupNavButtons();

        setupRecyclerView();

        notificationManager = new NotificationManager();

        // Load notifications initially (you can choose default ON/OFF)
        loadNotifications();

        // Switch behaviour: show/hide notifications
        stShowNotification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Show list and (re)load notifications
                binding.rvReceivedNotification.setVisibility(View.VISIBLE);
                loadNotifications();
            } else {
                // Hide list
                binding.rvReceivedNotification.setVisibility(View.GONE);
            }
        });
    }

    /**
     * A helper method that determines whether to load notifications for an admin or a regular user.
     */
    private void loadNotifications() {
        if (isAdmin) {
            loadAllNotificationsForAdmin();
        } else {
            loadNotificationsForUser();
        }
    }

    /**
     * Initializes the RecyclerView, its adapter, and its layout manager.
     * The adapter is configured based on whether the current user is an admin.
     */
    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notificationItems, this);
        adapter.setAdminView(isAdmin);
        adapter.setCurrentUserName(userName); // ADD THIS LINE
        binding.rvReceivedNotification.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvReceivedNotification.setAdapter(adapter);
    }

    /**
     * Initiates the process of loading all notifications from the database for the admin view.
     * It uses the NotificationManager and handles the success or failure of the operation.
     */
    private void loadAllNotificationsForAdmin() {
        notificationManager.loadAllNotifications(new NotificationManager.NotificationCallback() {
            @Override
            public void onSuccess(List<NotificationManager.NotificationModel> models) {
                if (!isAdded()) return;
                updateAdapterWithNotifications(models);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to load notifications.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Initiates the process of loading notifications specifically for the logged-in user.
     * It uses the NotificationManager and handles the success or failure of the operation.
     */
    private void loadNotificationsForUser() {
        if (userName == null || userName.isEmpty()) return;
        notificationManager.loadNotificationForUser(userName, new NotificationManager.NotificationCallback() {
            @Override
            public void onSuccess(List<NotificationManager.NotificationModel> models) {
                if (!isAdded()) return;
                updateAdapterWithNotifications(models);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to load notifications.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Clears the current list of notification items and repopulates it with a new list of data.
     * The list is reversed to show the most recent notifications first, and the adapter is notified.
     *
     * @param models The new list of NotificationModel data to be displayed.
     */
    private void updateAdapterWithNotifications(List<NotificationManager.NotificationModel> models) {
        notificationItems.clear();
        for (NotificationManager.NotificationModel model : models) {
            notificationItems.add(
                    new NotificationAdapter.NotificationItem(
                            model.id, model.content, model.eventName,
                            model.receiver, model.sender, model.timestamp
                    )
            );
        }
        Collections.reverse(notificationItems);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * Callback method from NotificationAdapter.Listener, triggered when a notification item is clicked.
     * @param item The NotificationItem that was clicked.
     */
    @Override
    public void onNotificationClick(NotificationAdapter.NotificationItem item) {
        if (!isAdded()) return;
        //Toast.makeText(getContext(), "Notification from: " + item.sender, Toast.LENGTH_SHORT).show();
    }

    /**
     * Callback method from NotificationAdapter.Listener, triggered when the delete button on an item is clicked.
     * It removes the item from the database and then updates the RecyclerView.
     *
     * @param item The NotificationItem to be deleted.
     * @param position The adapter position of the item being deleted.
     */
    @Override
    public void onDelete(NotificationAdapter.NotificationItem item, int position) {
        // This method now accepts the position directly from the adapter
        if (!isAdded() || adapter == null) return;


        if (position < 0 || position >= notificationItems.size()) {
            return;
        }

        notificationManager.deleteNotificationById(item.id, () -> {
            if (!isAdded()) return;


            notificationItems.remove(position);
            adapter.notifyItemRemoved(position);


            adapter.notifyItemRangeChanged(position, notificationItems.size());
        });
    }

    /**
     * Sets up the OnClickListeners for all navigation buttons in the fragment's layout.
     * The navigation targets and icon resources are adjusted based on whether the user is an admin.
     */
    private void setupNavButtons() {
        // --- Navigation that is the same for all users ---
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(NotificationScreenDirections.actionNotificationScreenToHomeScreen(userName)));

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(NotificationScreenDirections.actionNotificationScreenToProfileScreen(userName)));

        binding.btnSendNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(this)
                        .navigate(NotificationScreenDirections.actionNotificationScreenToSendNotificationScreen(userName)));

        if (isAdmin) {
            // --- ADMIN MODE ---
            // 1. "Event History" icon becomes "View Users".
            binding.btnEventHistory.setImageResource(R.drawable.outline_article_person_24);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(NotificationScreenDirections.actionNotificationScreenToViewUsersScreen(userName))
            );

            binding.btnOpenEvents.setImageResource(R.drawable.outline_add_photo_alternate_24);
            binding.btnOpenEvents.setOnClickListener(v ->

                    NavHostFragment.findNavController(this)
                            .navigate(NotificationScreenDirections.actionToAllImagesFragment(userName))
            );

        } else {

            binding.btnEventHistory.setImageResource(R.drawable.ic_history);
            binding.btnEventHistory.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(NotificationScreenDirections.actionNotificationScreenToEventHistoryScreen(userName))
            );


            binding.btnOpenEvents.setImageResource(R.drawable.ic_event);
            binding.btnOpenEvents.setOnClickListener(v ->
                    NavHostFragment.findNavController(this)
                            .navigate(NotificationScreenDirections.actionNotificationScreenToOrganizerEventsScreen(userName))
            );
        }
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
