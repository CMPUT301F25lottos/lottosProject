package com.example.lottos.auth;

import android.content.Context;
import android.content.SharedPreferences;

public class UserSession {

    private static final String PREFS = "AppSession";
    private static final String KEY_USERNAME = "loggedInUser";

    public static void saveUser(Context context, String userName) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_USERNAME, userName).apply();
    }

    public static String getUser(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_USERNAME, null);
    }

    public static boolean isLoggedIn(Context context) {
        return getUser(context) != null;
    }

    public static void logout(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_USERNAME).apply();
    }
}
