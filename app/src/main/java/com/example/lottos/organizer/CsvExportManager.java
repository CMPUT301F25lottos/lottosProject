package com.example.lottos.organizer;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages the creation and sharing of CSV files for event data.
 *
 * Role: This class is a utility responsible for taking raw event data,
 * extracting a specific list of users (e.g., enrolled users), formatting this
 * data into a CSV string, and saving it to a file in the app's external
 * storage. After creating the file, it automatically triggers a system "Share"
 * intent, allowing the user to send the CSV file to other apps like email or
 * cloud storage.
 */
public class CsvExportManager {

    /**
     * A callback interface to report the outcome of the CSV export operation.
     */
    public interface CsvExportCallback {
        /**
         * Called when the CSV file is successfully created and saved.
         * @param path The absolute path to the newly created CSV file.
         */
        void onSuccess(String path);
        /**
         * Called when an error occurs during the export process.
         * @param errorMessage A message describing the failure.
         */
        void onFailure(String errorMessage);
    }

    private final Context context;

    /**
     * Constructs a new CsvExportManager.
     * @param context The Android context, required for file operations and starting activities.
     */
    public CsvExportManager(Context context) {
        this.context = context;
    }

    /**
     * Exports the list of enrolled users for a given event to a CSV file.
     * It extracts the enrolled users, builds a CSV-formatted string, saves it to a file,
     * and then initiates a share action.
     *
     * @param eventData A map containing the data for the event, including the 'enrolledList'.
     * @param callback The callback to be invoked with the result of the operation.
     */
    public void exportEnrolledUsers(Map<String, Object> eventData, CsvExportCallback callback) {
        // Extract enrolled users from event data
        Map<String, Object> enrolled = (Map<String, Object>) eventData.get("enrolledList");
        List<String> enrolledUsers = new ArrayList<>();

        if (enrolled != null && enrolled.get("users") instanceof List) {
            enrolledUsers = (List<String>) enrolled.get("users");
        }

        if (enrolledUsers.isEmpty()) {
            callback.onFailure("No enrolled users to export.");
            return;
        }

        // Build the CSV content
        StringBuilder sb = new StringBuilder();
        sb.append("Event Name,").append("Organizer,").append("Total Enrolled\n");
        sb.append(safe(eventData.get("eventName"))).append(",").append(safe(eventData.get("organizer"))).append(",").append(enrolledUsers.size()).append("\n\n");
        sb.append("Enrolled Users\n");
        for (String user : enrolledUsers) {
            sb.append(user).append("\n");
        }

        String fileName = safe(eventData.get("eventName")).replaceAll("[^a-zA-Z0-9.-]", "_") + "_enrolled.csv";
        try {
            File file = new File(context.getExternalFilesDir(null), fileName);
            FileWriter writer = new FileWriter(file);
            writer.append(sb.toString());
            writer.flush();
            writer.close();
            callback.onSuccess(file.getAbsolutePath());

            // Trigger the share intent
            shareCsvFile(file);

        } catch (IOException e) {
            callback.onFailure("Export failed: " + e.getMessage());
        }
    }

    /**
     * Creates and starts an Intent to share a given file.
     * It uses a FileProvider to create a secure, shareable URI for the file.
     *
     * @param file The CSV file to be shared.
     */
    private void shareCsvFile(File file) {
        Uri uri = FileProvider.getUriForFile(
                context,
                context.getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(Intent.createChooser(intent, "Share CSV"));
    }

    /**
     * A helper method to safely convert an object to its string representation.
     * Returns an empty string if the object is null.
     *
     * @param o The object to convert.
     * @return The string value of the object, or "" if the object is null.
     */
    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
