package com.example.lottos;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.lottos.databinding.FragmentNotificationScreenBinding;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationScreen extends Fragment {

    private FragmentNotificationScreenBinding binding;
    private FirebaseFirestore db;
    private String userName;

    private List<String> receivedNotifications = new ArrayList<>();
    private List<String> sentNotifications = new ArrayList<>();

    private ArrayAdapter<String> receivedAdapter;
    private ArrayAdapter<String> sentAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNotificationScreenBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        NotificationScreenArgs args = NotificationScreenArgs.fromBundle(getArguments());
        userName = args.getUserName();

        db = FirebaseFirestore.getInstance();

        // navigation
        binding.btnBack.setOnClickListener(v ->
                NavHostFragment.findNavController(NotificationScreen.this)
                        .navigate(NotificationScreenDirections.actionNotificationScreenToHomeScreen(userName))
        );
        binding.btnSendNotification.setOnClickListener(v ->
                NavHostFragment.findNavController(NotificationScreen.this)
                        .navigate(NotificationScreenDirections.actionNotificationScreenToSendNotificationScreen(userName))
        );

        // adapters
        receivedAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, receivedNotifications);
        binding.lvReceivedNotication.setAdapter(receivedAdapter);

        sentAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, sentNotifications);
        binding.lvSentNotication.setAdapter(sentAdapter);

        // load both sections
        fetchReceivedNotifications();
        fetchFirestoreNotifications();
    }

    /** gets auto-generated invite/uninvite messages from user document */
    private void fetchReceivedNotifications() {
        db.collection("users")
                .document(userName)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) return;

                    receivedNotifications.clear();

                    Map<String, Object> selectedMap = (Map<String, Object>) documentSnapshot.get("selectedEvents");
                    if (selectedMap != null && selectedMap.containsKey("events")) {
                        List<String> selectedList = (List<String>) selectedMap.get("events");
                        for (String event : selectedList) {
                            receivedNotifications.add("🎉 Congrats! You're selected for " + event);
                        }
                    }

                    Map<String, Object> unselectedMap = (Map<String, Object>) documentSnapshot.get("notSelectedEvents");
                    if (unselectedMap != null && unselectedMap.containsKey("events")) {
                        List<String> unselectedList = (List<String>) unselectedMap.get("events");
                        for (String event : unselectedList) {
                            receivedNotifications.add("😢 Sorry! You're not selected for " + event);
                        }
                    }

                    if (receivedNotifications.isEmpty()) {
                        receivedNotifications.add("No auto notifications.");
                    }

                    receivedAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error loading selected/unselected", e));
    }

    /** pulls stored notifications where user is sender OR receiver */
    private void fetchFirestoreNotifications() {
        // received messages
        db.collection("notification")
                .whereEqualTo("receiver", userName)
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String content = doc.getString("content");
                        String eventName = doc.getString("eventName");
                        receivedNotifications.add("📩 " + eventName + ": " + content);
                    }
                    receivedAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Failed to load received notifications", e));

        // sent messages
        db.collection("notification")
                .whereEqualTo("sender", userName)
                .get()
                .addOnSuccessListener(query -> {
                    sentNotifications.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String content = doc.getString("content");
                        String eventName = doc.getString("eventName");
                        sentNotifications.add("📤 " + eventName + ": " + content);
                    }
                    if (sentNotifications.isEmpty()) {
                        sentNotifications.add("No sent notifications.");
                    }
                    sentAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Failed to load sent notifications", e));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
