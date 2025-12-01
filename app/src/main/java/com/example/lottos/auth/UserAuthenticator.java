package com.example.lottos.auth;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.google.firebase.installations.FirebaseInstallations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages user authentication and session management against a Firebase Firestore backend.
 * This class provides functionality for:
 * <ul>
 *     <li>User registration (signup) with a username and password.</li>
 *     <li>User login with credentials validation.</li>
 *     <li>Linking a user's account to a specific device using Firebase Installations ID.</li>
 *     <li>Deleting user accounts and cleaning up associated device links.</li>
 * </ul>
 * It uses listener interfaces to communicate results asynchronously.
 */
public class UserAuthenticator {

    private static final String TAG = "UserAuthenticator";
    private final FirebaseFirestore db;
    private final CollectionReference usersRef;

    /**
     * An interface to receive the results of an authentication-related operation.
     */
    public interface AuthListener {
        /**
         * Called when the operation (e.g., login, signup) completes successfully.
         * @param userName The username of the user involved in the operation.
         */
        void onSuccess(String userName);
        /**
         * Called when the operation fails.
         * @param errorMessage A message describing the reason for the failure.
         */
        void onFailure(String errorMessage);
    }

    /**
     * Default constructor. Initializes a new instance of FirebaseFirestore.
     */
    public UserAuthenticator() {
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
    }

    /**
     * Constructor that accepts an existing FirebaseFirestore instance.
     * Used for testing or when a specific instance is required.
     * @param db The FirebaseFirestore instance to use.
     */
    public UserAuthenticator(FirebaseFirestore db) {
        this.db = db;
        this.usersRef = db.collection("users");
    }

    /**
     * Checks a user's login credentials against the Firestore database.
     * If credentials are valid, it cleans up any previous device links for that user
     * and creates a new link for the current device before signaling success.
     * @param userName The username to check.
     * @param password The password to verify.
     * @param listener The callback to be invoked with the result.
     */
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

            Map<String, Object> userInfoMap = (Map<String, Object>) doc.get("userInfo");
            if (userInfoMap == null || userInfoMap.get("password") == null) {
                listener.onFailure("User data missing");
                return;
            }

            String storedPassword = userInfoMap.get("password").toString();

            if (storedPassword.equals(password)) {

                usersRef.document(userName)
                        .collection("devices")
                        .get()
                        .addOnSuccessListener(deviceQuery -> {

                            WriteBatch batch = db.batch();

                            for (DocumentSnapshot d : deviceQuery.getDocuments()) {
                                batch.delete(d.getReference());
                                batch.delete(db.collection("devices").document(d.getId()));
                            }

                            batch.commit()
                                    .addOnSuccessListener(aVoid -> {
                                        linkDeviceToUser(userName, () -> listener.onSuccess(userName));
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Failed removing old devices", e);
                                        listener.onFailure("Device sync error. Try again.");
                                    });

                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to fetch device list", e);
                            listener.onFailure("Device check failed. Try again.");
                        });

                return;
            }

            listener.onFailure("Incorrect password");
        });
    }

    /**
     * Registers a new user in the Firestore database.
     * It first checks if the username already exists. If not, it creates a new user
     * document with the provided details and an initial empty structure for events.
     * After creation, it links the current device to the new user.
     * @param userName The desired username (must be unique).
     * @param displayName The user's display name.
     * @param password The user's password.
     * @param email The user's email address.
     * @param phoneNumber The user's phone number (optional).
     * @param listener The callback to be invoked with the result.
     */
    public void registerUser(@NonNull String userName, @NonNull String displayName, @NonNull String password, @NonNull String email, String phoneNumber, @NonNull AuthListener listener) {

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
                        linkDeviceToUser(userName, () -> listener.onSuccess(userName));
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error creating user", e);
                        listener.onFailure("Error creating account. Please try again.");
                    });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "Error checking username", e);
            listener.onFailure("Error checking username. Try again.");
        });
    }

    /**
     * Links the current device to a specified user account.
     * This involves creating two documents in a batch write: one in the user's "devices"
     * sub-collection and another in the top-level "devices" collection for easy lookup.
     * @param userName The username to link the device to.
     * @param onComplete A callback to be run after the linking process completes (or fails).
     */
    private void linkDeviceToUser(String userName, Runnable onComplete) {

        FirebaseInstallations.getInstance().getId()
                .addOnSuccessListener(deviceId -> {

                    Log.d(TAG, "Device ID = " + deviceId);

                    DocumentReference userDeviceRef =
                            usersRef.document(userName)
                                    .collection("devices")
                                    .document(deviceId);

                    DocumentReference deviceRef =
                            db.collection("devices")
                                    .document(deviceId);

                    Map<String, Object> deviceData = new HashMap<>();
                    deviceData.put("platform", "android");
                    deviceData.put("model", Build.MODEL);
                    deviceData.put("createdAt", Timestamp.now());
                    deviceData.put("lastUsedAt", Timestamp.now());

                    Map<String, Object> deviceLookup = new HashMap<>();
                    deviceLookup.put("userName", userName);
                    deviceLookup.put("linkedAt", Timestamp.now());

                    db.runBatch(batch -> {
                        batch.set(userDeviceRef, deviceData, SetOptions.merge());
                        batch.set(deviceRef, deviceLookup, SetOptions.merge());
                    }).addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Device linking failed", task.getException());
                        }
                        onComplete.run();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Could not get Firebase Installation ID", e);
                    onComplete.run();
                });
    }

    /**
     * Deletes a user's account and removes the associated link for the current device.
     * This is performed in a single batch operation to ensure atomicity. It deletes
     * the user document, the device lookup document, and the device document within the user's sub-collection.
     * @param userName The username of the account to delete.
     * @param onComplete A callback to be run on successful deletion.
     * @param onError A callback to be run if the deletion fails.
     */
    public void deleteUserAndDevice(@NonNull String userName, @NonNull Runnable onComplete, @NonNull Runnable onError) {

        FirebaseInstallations.getInstance().getId()
                .addOnSuccessListener(deviceId -> {

                    DocumentReference userRef = usersRef.document(userName);
                    DocumentReference deviceRef = db.collection("devices").document(deviceId);
                    DocumentReference userDeviceRef =
                            userRef.collection("devices").document(deviceId);

                    db.runBatch(batch -> {
                        batch.delete(userDeviceRef);
                        batch.delete(deviceRef);
                        batch.delete(userRef);
                    }).addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Log.e(TAG, "Delete failed", task.getException());
                            onError.run();
                        } else {
                            onComplete.run();
                        }
                    });

                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retrieving device ID for deletion", e);
                    onError.run();
                });
    }
}
