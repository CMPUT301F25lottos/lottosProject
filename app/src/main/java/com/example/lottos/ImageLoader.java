package com.example.lottos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A utility class for asynchronously loading images from a URL into an ImageView.
 *
 * Role: This class provides a simple, static method to handle the boilerplate of
 * downloading an image from the internet on a background thread and then setting
 * it on an ImageView on the main UI thread. It uses a fixed-size thread pool
 * to manage concurrent image downloads. If the URL is null, empty, or if the
 * download fails, it sets a specified fallback drawable resource on the ImageView.
 */
public class ImageLoader {

    // A static thread pool to handle multiple image download requests concurrently.
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    /**
     * Asynchronously downloads an image from a given URL and sets it on an ImageView.
     *
     * This method first checks if the URL is valid. If not, it immediately sets the
     * fallback resource. Otherwise, it submits a task to a background thread to
     * download the image. On successful download, the resulting Bitmap is set on the
     * target ImageView on the main UI thread. If the download fails for any reason,
     * the fallback resource is used instead.
     *
     * @param url The string representation of the image URL to download.
     * @param target The ImageView widget that will display the loaded image.
     * @param fallbackRes The drawable resource ID (e.g., R.drawable.placeholder)
     *                    to be used if the URL is invalid or the download fails.
     */
    public static void load(String url, ImageView target, int fallbackRes) {
        // 1. Basic validation: If URL is null or empty, use the fallback immediately.
        if (url == null || url.trim().isEmpty()) {
            target.setImageResource(fallbackRes);
            return;
        }

        // 2. Execute the download on a background thread from the thread pool.
        executor.execute(() -> {
            Bitmap bmp = null;

            try {
                // Standard boilerplate for downloading an image via HTTP.
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setDoInput(true);
                conn.connect();

                try (InputStream in = conn.getInputStream()) {
                    // Decode the input stream into a Bitmap.
                    bmp = BitmapFactory.decodeStream(in);
                }

            } catch (Exception ignored) {
                // If any part of the download fails, the exception is ignored,
                // and 'bmp' will remain null.
            }

            // 3. Post the result back to the main UI thread.
            Bitmap result = bmp;
            new Handler(Looper.getMainLooper()).post(() -> {
                // This code runs on the UI thread.
                if (result != null) {
                    // If download was successful, set the bitmap.
                    target.setImageBitmap(result);
                } else {
                    // If download failed, set the fallback resource.
                    target.setImageResource(fallbackRes);
                }
            });
        });
    }
}
