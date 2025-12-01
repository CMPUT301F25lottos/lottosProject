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

public class CsvExportManager {

    public interface CsvExportCallback {
        void onSuccess(String path);
        void onFailure(String errorMessage);
    }

    private final Context context;

    public CsvExportManager(Context context) {
        this.context = context;
    }

    public void exportEnrolledUsers(Map<String, Object> eventData, CsvExportCallback callback) {
        // 1. Extract enrolled users from event data
        Map<String, Object> enrolled = (Map<String, Object>) eventData.get("enrolledList");
        List<String> enrolledUsers = new ArrayList<>();

        if (enrolled != null && enrolled.get("users") instanceof List) {
            enrolledUsers = (List<String>) enrolled.get("users");
        }

        if (enrolledUsers.isEmpty()) {
            callback.onFailure("No enrolled users to export.");
            return;
        }

        // 2. Build the CSV content
        StringBuilder sb = new StringBuilder();
        sb.append("Event Name,").append("Organizer,").append("Total Enrolled\n");
        sb.append(safe(eventData.get("eventName"))).append(",").append(safe(eventData.get("organizer"))).append(",").append(enrolledUsers.size()).append("\n\n");
        sb.append("Enrolled Users\n");
        for (String user : enrolledUsers) {
            sb.append(user).append("\n");
        }

        // 3. Create and save the file
        String fileName = safe(eventData.get("eventName")).replaceAll("[^a-zA-Z0-9.-]", "_") + "_enrolled.csv";
        try {
            File file = new File(context.getExternalFilesDir(null), fileName);
            FileWriter writer = new FileWriter(file);
            writer.append(sb.toString());
            writer.flush();
            writer.close();
            callback.onSuccess(file.getAbsolutePath());

            // 4. Trigger the share intent
            shareCsvFile(file);

        } catch (IOException e) {
            callback.onFailure("Export failed: " + e.getMessage());
        }
    }

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

    private String safe(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
