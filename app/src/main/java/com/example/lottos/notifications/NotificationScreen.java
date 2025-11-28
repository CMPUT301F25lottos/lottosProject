package com.example.lottos.notifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.lottos.databinding.FragmentNotificationScreenBinding;
import com.example.lottos.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationScreen extends Fragment implements NotificationAdapter.Listener {

    private static final String TAG = "NotificationScreen";

    private FragmentNotificationScreenBinding binding;
    private NotificationManager notificationManager;

    private NotificationAdapter adapter;
    private final List<NotificationAdapter.NotificationItem> notifications = new ArrayList<>();

    private String userName;
    private boolean isAdmin = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        if (getArguments() != null) {
            NotificationScreenArgs args = NotificationScreenArgs.fromBundle(getArguments());
            userName = args.getUserName();
        }


        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);
        Log.d(TAG, "Username is '" + userName + "', isAdmin from SharedPreferences is: " + isAdmin);
        // --- END: LOGIC COPIED FROM HomeScreen.java ---


        notificationManager = new NotificationManager();
        setupNavButtons();


        setupRecycler();


        if (isAdmin) {
            binding.tvTitle.setText("All Notifications");
            binding.btnSendNotification.setVisibility(View.GONE); // MAKE BUTTON VISIBLE FOR ADMIN
            loadAllNotificationsForAdmin();
        } else {
            binding.tvTitle.setText("My Notifications");
            binding.btnSendNotification.setVisibility(View.VISIBLE); // HIDE BUTTON FOR USERS
            loadNotificationsForUser();
        }
    }

    private void setupRecycler() {
        adapter = new NotificationAdapter(notifications, this);

        adapter.setAdminView(isAdmin);
        binding.rvReceivedNotification.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvReceivedNotification.setAdapter(adapter);
        binding.rvReceivedNotification.setNestedScrollingEnabled(false);
    }

    private void loadAllNotificationsForAdmin() {
        notificationManager.loadAllNotifications(new NotificationManager.NotificationCallback() {
            @Override
            public void onSuccess(List<NotificationManager.NotificationModel> list) {
                if (!isAdded()) return;
                Collections.reverse(list);
                updateAdapterWithEvents(list);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Log.e(TAG, "Error loading all notifications for admin", e);
                Toast.makeText(requireContext(), "Failed to load all notifications.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadNotificationsForUser() {
        if (userName == null || userName.isEmpty()) {
            Log.e(TAG, "userName is null/empty, cannot load notifications");
            return;
        }

        notificationManager.loadNotificationForUser(userName, new NotificationManager.NotificationCallback() {
            @Override
            public void onSuccess(List<NotificationManager.NotificationModel> list) {
                if (!isAdded()) return;
                updateAdapterWithEvents(list);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                Log.e(TAG, "Error loading notifications", e);
                Toast.makeText(
                        requireContext(),
                        "Failed to load notifications.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    private void updateAdapterWithEvents(List<NotificationManager.NotificationModel> modelList) {
        notifications.clear();
        for (NotificationManager.NotificationModel evt : modelList) {
            notifications.add(
                    new NotificationAdapter.NotificationItem(
                            evt.id,
                            evt.content,
                            evt.eventName,
                            evt.receiver,
                            evt.sender,
                            evt.timestamp
                    )
            );
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onNotificationClick(NotificationAdapter.NotificationItem item) {
        if (!isAdded()) return;

        Toast.makeText(
                requireContext(),
                "From: " + item.sender + "\nEvent: " + item.eventName,
                Toast.LENGTH_SHORT
        ).show();
    }

    @Override
    public void onDelete(NotificationAdapter.NotificationItem item) {
        if (!isAdded()) return;

        int position = notifications.indexOf(item);
        if (position == -1) return;

        notificationManager.deleteNotificationById(item.id, () -> {
            if (!isAdded()) return;

            notifications.remove(position);
            adapter.notifyItemRemoved(position);

            if (notifications.isEmpty()) {
                Toast.makeText(requireContext(),
                        "No notifications left.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupNavButtons() {
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(NotificationScreen.this)
                        .navigate(NotificationScreenDirections
                                .actionNotificationScreenToHomeScreen(userName))
        );

        binding.btnSendNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(NotificationScreen.this)
                        .navigate(NotificationScreenDirections
                                .actionNotificationScreenToSendNotificationScreen(userName))
        );

        binding.btnProfile.setOnClickListener(v ->
                NavHostFragment.findNavController(NotificationScreen.this)
                        .navigate(NotificationScreenDirections
                                .actionNotificationScreenToProfileScreen(userName))
        );

        binding.btnEventHistory.setOnClickListener(v ->
                NavHostFragment.findNavController(NotificationScreen.this)
                        .navigate(NotificationScreenDirections
                                .actionNotificationScreenToEventHistoryScreen(userName))
        );

        binding.btnOpenEvents.setOnClickListener(v ->
                NavHostFragment.findNavController(NotificationScreen.this)
                        .navigate(NotificationScreenDirections
                                .actionNotificationScreenToOrganizerEventsScreen(userName))
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
