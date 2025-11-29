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

import com.example.lottos.R;
import com.example.lottos.databinding.FragmentNotificationScreenBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationScreen extends Fragment implements NotificationAdapter.Listener {

    private FragmentNotificationScreenBinding binding;
    private NotificationManager notificationManager;
    private NotificationAdapter adapter;
    private final List<NotificationAdapter.NotificationItem> notificationItems = new ArrayList<>();
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

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        isAdmin = sharedPreferences.getBoolean("isAdmin", false);

        if (getArguments() != null) {
            userName = NotificationScreenArgs.fromBundle(getArguments()).getUserName();
        }

        if (userName == null) {
            userName = sharedPreferences.getString("userName", null);
        }

        if (userName == null) {
            Toast.makeText(getContext(), "Credentials not found. Please log in.", Toast.LENGTH_LONG).show();
            NavHostFragment.findNavController(this).popBackStack(R.id.WelcomeScreen, false);
            return;
        }

        notificationManager = new NotificationManager();
        setupRecyclerView();
        setupNavButtons();

        if (isAdmin) {
            binding.tvTitle.setText("All Notifications");
            binding.btnSendNotification.setVisibility(View.GONE);
            loadAllNotificationsForAdmin();
        } else {
            binding.tvTitle.setText("My Notifications");
            binding.btnSendNotification.setVisibility(View.VISIBLE);
            loadNotificationsForUser();
        }
    }


    private void setupRecyclerView() {
        adapter = new NotificationAdapter(notificationItems, this);
        adapter.setAdminView(isAdmin);
        binding.rvReceivedNotification.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvReceivedNotification.setAdapter(adapter);
    }

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

    @Override
    public void onNotificationClick(NotificationAdapter.NotificationItem item) {
        if (!isAdded()) return;
        Toast.makeText(getContext(), "Notification from: " + item.sender, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDelete(NotificationAdapter.NotificationItem item) {
        if (!isAdded()) return;
        int position = notificationItems.indexOf(item);
        if (position == -1) return;

        notificationManager.deleteNotificationById(item.id, () -> {
            if (!isAdded()) return;
            notificationItems.remove(position);
            adapter.notifyItemRemoved(position);
        });
    }



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





    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
