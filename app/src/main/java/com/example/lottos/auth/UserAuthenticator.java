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
 * Updated UserAuthenticator:
 * - Supports username/password Firestore auth (your existing system)
 * - Adds device-linking using Firebase Installations ID
 * - Supports login, signup, logout, account deletion with device cleanup
 */
public class UserAuthenticator {

    private static final String TAG = "UserAuthenticator";
    private final FirebaseFirestore db;
    private final CollectionReference usersRef;

    public interface AuthListener {
        void onSuccess(String userName);
        void onFailure(String errorMessage);
    }

    public UserAuthenticator() {
        db = FirebaseFirestore.getInstance();
        usersRef = db.collection("users");
    }

    public UserAuthenticator(FirebaseFirestore db) {
        this.db = db;
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
