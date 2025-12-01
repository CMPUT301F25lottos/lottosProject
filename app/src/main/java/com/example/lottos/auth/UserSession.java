package com.example.lottos.auth;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Manages the user's session state using SharedPreferences. * This utility class provides static methods to save, retrieve, and clear
 * the logged-in user's username, effectively handling the session lifecycle.
 */
public class UserSession {

    private static final String PREFS = "AppSession";
    private static final String KEY_USERNAME = "loggedInUser";

    /**
     * Saves the username of the currently logged-in user to SharedPreferences.
     * This establishes a user session.
     *
     * @param context  The application context, used to access SharedPreferences.
     * @param userName The username of the user to save.
     */
    public static void saveUser(Context context, String userName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USERNAME, userName).apply();
    }

    /**
     * Retrieves the saved username from SharedPreferences.
     *
     * @param context The application context, used to access SharedPreferences.
     * @return The username of the logged-in user, or null if no user is logged in.
     */
    public static String getUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, null);
    }

    /**
     * Checks if there is a user currently logged in.
     *
     * @param context The application context, used to access SharedPreferences.
     * @return true if a username is saved in the session, false otherwise.
     */
    public static boolean isLoggedIn(Context context) {
        return getUser(context) != null;
    }

    /**
     * Clears the user session by removing the username from SharedPreferences.
     * This effectively logs the user out.
     *
     * @param context The application context, used to access SharedPreferences.
     */
    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_USERNAME).apply();
    }
}
