package com.example.lottos.auth;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles user authentication and registration with Firestore.
 *
 * Role:
 * - Provides centralized logic for verifying login credentials and creating new users.
 * Responsibilities:
 * - Authenticates users against stored Firestore records.
 * - Registers new users with initialized event lists.
 * - Encapsulates all Firestore access for user-related actions.
 */
public class UserAuthenticator {

    private static final String TAG = "UserAuthenticator";
    private final CollectionReference usersRef;

    public interface AuthListener {
        void onSuccess(String userName);
        void onFailure(String errorMessage);
    }

    public UserAuthenticator() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        this.usersRef = db.collection("users");
    }

    public void checkUserLogin(@NonNull String userName, @NonNull String password, @NonNull AuthListener listener) {
        DocumentReference userDoc = usersRef.document(userName);
        userDoc.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting user document", task.getException());
                listener.onFailure("Login failed. Try again.");
                return;
            }

            DocumentSnapshot doc = task.getResult();
            if (doc == null || !doc.exists()) {
                listener.onFailure("Username not found");
                return;
            }

            try {
                Map<String, Object> userInfoMap = (Map<String, Object>) doc.get("userInfo");
                if (userInfoMap == null || userInfoMap.get("password") == null) {
                    listener.onFailure("User data missing");
                    return;
                }

                String storedPassword = userInfoMap.get("password").toString();
                if (storedPassword.equals(password)) {
                    listener.onSuccess(userName);
                } else {
                    listener.onFailure("Incorrect password");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error reading user data for " + userName, e);
                listener.onFailure("Data format error");
            }
        });
    }

    public void registerUser(
            @NonNull String userName,
            @NonNull String displayName,
            @NonNull String password,
            @NonNull String email,
            String phoneNumber,
            @NonNull AuthListener listener
    ) {
        DocumentReference userDoc = usersRef.document(userName);

        userDoc.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                listener.onFailure("Username already taken. Please choose another.");
                return;
            }

            Map<String, Object> emptyEventsMap = new HashMap<>();
            emptyEventsMap.put("events", new ArrayList<String>());

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("displayName", displayName);
            userInfo.put("email", email);
            userInfo.put("name", displayName);
            userInfo.put("password", password);
            userInfo.put("phoneNumber", phoneNumber);

            Map<String, Object> userData = new HashMap<>();
            userData.put("userName", userName);
            userData.put("userInfo", userInfo);

            userData.put("closedEvents", new HashMap<>(emptyEventsMap));
            userData.put("declinedEvents", new HashMap<>(emptyEventsMap));
            userData.put("enrolledEvents", new HashMap<>(emptyEventsMap));
            userData.put("selectedEvents", new HashMap<>(emptyEventsMap));
            userData.put("notSelectedEvents", new HashMap<>(emptyEventsMap));
            userData.put("organizedEvents", new HashMap<>(emptyEventsMap));
            userData.put("waitListedEvents", new HashMap<>(emptyEventsMap));

            userDoc.set(userData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "User account created: " + userName);
                        listener.onSuccess(userName);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating user: " + e.getMessage(), e);
                        listener.onFailure("Error creating account. Please try again.");
                    });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking username: " + e.getMessage(), e);
            listener.onFailure("Error checking username. Try again.");
        });
    }
}
