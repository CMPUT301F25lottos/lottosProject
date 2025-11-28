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

public class ImageLoader {

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    public static void load(String url, ImageView target, int fallbackRes) {
        if (url == null || url.trim().isEmpty()) {
            target.setImageResource(fallbackRes);
            return;
        }

        executor.execute(() -> {
            Bitmap bmp = null;

            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setDoInput(true);
                conn.connect();

                try (InputStream in = conn.getInputStream()) {
                    bmp = BitmapFactory.decodeStream(in);
                }

            } catch (Exception ignored) { }

            Bitmap result = bmp;
            new Handler(Looper.getMainLooper()).post(() -> {
                if (result != null) {
                    target.setImageBitmap(result);
                } else {
                    target.setImageResource(fallbackRes);
                }
            });
        });
    }
}
