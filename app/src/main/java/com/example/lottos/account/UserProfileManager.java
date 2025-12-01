package com.example.lottos.account;

import android.util.Log;

import androidx.annotation.NonNull; // Import this

import com.example.lottos.auth.UserAuthenticator;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

/**
 * Manages user profile data operations with Firebase Firestore.
 * This class provides methods to load, update, and delete user profile information.
 * It communicates results asynchronously through listener interfaces.
 */
public class UserProfileManager {
    private static final String TAG = "UserProfileManager";
    private final FirebaseFirestore db;

    /**
     * Constructs a UserProfileManager with a given Firestore database instance.
     * @param db The FirebaseFirestore instance to use for database operations.
     */
    public UserProfileManager(FirebaseFirestore db) {
        this.db = db;
    }

    /**
     * Listener interface for profile loading operations.
     */
    public static interface ProfileLoadListener {
        /**
         * Called when the profile data is successfully loaded.
         * @param name The user's name.
         * @param email The user's email.
         * @param phone The user's phone number.
         */
        void onProfileLoaded(String name, String email, String phone);

        /**
         * Called when the user's profile document is not found.
         */
        void onProfileNotFound();

        /**
         * Called when an error occurs during the loading process.
         * @param errorMessage A message describing the error.
         */
        void onError(String errorMessage);
    }

    /**
     * Listener interface for profile update operations.
     */
    public static interface ProfileUpdateListener {
        /**
         * Called when the profile is successfully updated.
         */
        void onUpdateSuccess();

        /**
         * Called when an error occurs during the update process.
         * @param errorMessage A message describing the error.
         */
        void onUpdateFailure(String errorMessage);
    }

    /**
     * Listener interface for user deletion operations.
     */
    public static interface DeleteListener {
        /**
         * Called when the user is successfully deleted.
         */
        void onDeleteSuccess();

        /**
         * Called when an error occurs during the deletion process.
         * @param errorMessage A message describing the error.
         */
        void onDeleteFailure(String errorMessage);
    }

    /**
     * Asynchronously loads a user's profile information from Firestore.
     * @param userName The username (document ID) of the user to load.
     * @param listener The callback listener to handle the result.
     */
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


    /**
     * Extracts user information from a DocumentSnapshot and passes it to the listener.
     * @param snapshot The DocumentSnapshot containing the user's data.
     * @param listener The listener to receive the extracted data.
     */
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

    /**
     * Updates a user's profile information in Firestore.
     * @param userName The username (document ID) of the user to update.
     * @param name The new name for the user.
     * @param email The new email for the user.
     * @param phone The new phone number for the user.
     * @param listener The callback listener to handle the result.
     */
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

    /**
     * Deletes a user's account and associated device link from Firestore.
     * This operation is delegated to the UserAuthenticator.
     * @param userName The username of the user to delete.
     * @param listener The callback listener to handle the result.
     */
    public void deleteUser(String userName, DeleteListener listener) {
        UserAuthenticator auth = new UserAuthenticator(db);

        auth.deleteUserAndDevice(userName,
                () -> listener.onDeleteSuccess(),
                () -> listener.onDeleteFailure("Failed to delete user and device link"));
    }

}
