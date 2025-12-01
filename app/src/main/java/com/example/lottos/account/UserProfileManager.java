package com.example.lottos.account;

import android.util.Log;

import androidx.annotation.NonNull; // Import this

import com.example.lottos.auth.UserAuthenticator;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class UserProfileManager {
    private static final String TAG = "UserProfileManager";
    private final FirebaseFirestore db;

    public UserProfileManager(FirebaseFirestore db) {
        this.db = db;
    }

    public static interface ProfileLoadListener {
        void onProfileLoaded(String name, String email, String phone);
        void onProfileNotFound();
        void onError(String errorMessage);
    }

    public static interface ProfileUpdateListener {
        void onUpdateSuccess();
        void onUpdateFailure(String errorMessage);
    }

    public static interface DeleteListener {
        void onDeleteSuccess();
        void onDeleteFailure(String errorMessage);
    }

    public void loadUserProfile(String userName, ProfileLoadListener listener) {
        DocumentReference ref = db.collection("users").document(userName);

        ref.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                extractUserInfo(snapshot, listener);
            } else {
                Log.d(TAG, "User profile not found for: " + userName);
                listener.onProfileNotFound();
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to load profile", e);
            listener.onError("Failed to load profile info.");
        });
    }


    private void extractUserInfo(DocumentSnapshot snapshot, ProfileLoadListener listener) {
        Map<String, Object> userInfo = (Map<String, Object>) snapshot.get("userInfo");

        if (userInfo == null) {
            Log.w(TAG, "User document exists but is missing 'userInfo' map.");
            listener.onError("No profile data found.");
            return;
        }

        String name = (String) userInfo.get("name");
        String email = (String) userInfo.getOrDefault("email", "N/A");
        String phone = (String) userInfo.getOrDefault("phoneNumber", "N/A");

        if (name == null) {
            Log.w(TAG, "User document is missing 'name' inside 'userInfo' map.");
            listener.onError("Profile data is incomplete.");
            return;
        }

        listener.onProfileLoaded(name, email, phone);
    }

    public void updateUserProfile(String userName, String name, String email, String phone, ProfileUpdateListener listener) {
        DocumentReference doc = db.collection("users").document(userName);

        doc.update(
                        "userInfo.name", name,
                        "userInfo.email", email,
                        "userInfo.phoneNumber", phone
                ).addOnSuccessListener(aVoid -> listener.onUpdateSuccess())
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Update failed", e);
                    listener.onUpdateFailure("Failed to update profile.");
                });
    }

    public void deleteUser(String userName, DeleteListener listener) {
        UserAuthenticator auth = new UserAuthenticator(db);

        auth.deleteUserAndDevice(userName,
                () -> listener.onDeleteSuccess(),
                () -> listener.onDeleteFailure("Failed to delete user and device link"));
    }

}
