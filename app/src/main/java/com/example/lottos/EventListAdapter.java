package com.example.lottos;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.VH> {

    public interface Listener {
        void onEventClick(String eventId);
        void onEventSelected(String eventId);
    }

    public static class EventItem {
        public final String id;

        public final String name;
        public final boolean isOpen;

        public final String location;
        public final String startTimeText;
        public final String endTimeText;

        public final String posterUrl;

        public EventItem(String id, String name, boolean isOpen) {
            this(id, name, isOpen, null, null, null, null);
        }

        public EventItem(String id, String name, boolean isOpen, String location) {
            this(id, name, isOpen, location, null, null, null);
        }

        public EventItem(String id, String name, boolean isOpen, String location, String startTimeText, String endTimeText) {
            this(id, name, isOpen, location, startTimeText, endTimeText, null);
        }

        // NEW: full constructor with posterUrl
        public EventItem(String id, String name, boolean isOpen, String location, String startTimeText, String endTimeText, String posterUrl) {
            this.id = id;
            this.name = name;
            this.isOpen = isOpen;
            this.location = location;
            this.startTimeText = startTimeText;
            this.endTimeText = endTimeText;
            this.posterUrl = posterUrl;
        }
    }

    private final List<EventItem> events;
    private final Listener listener;
    private int selectedPosition = RecyclerView.NO_POSITION;

    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public EventListAdapter(List<EventItem> events, Listener listener) {
        this.events = events;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEventName, tvTime, tvLocation;
        ImageButton btnArrow;
        ImageView eventImage; // NEW

        VH(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            btnArrow = itemView.findViewById(R.id.btnArrow);
            eventImage = itemView.findViewById(R.id.eventImage);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        EventItem evt = events.get(position);

        holder.tvEventName.setText(evt.name);
        holder.tvLocation.setText(evt.location != null ? evt.location : "");

        if (evt.startTimeText != null && evt.endTimeText != null) {
            holder.tvTime.setText(evt.startTimeText + " - " + evt.endTimeText);
        } else {
            holder.tvTime.setText("");
        }

        holder.itemView.setBackgroundColor(
                position == selectedPosition ? 0x220000FF : 0x00000000
        );

        if (evt.posterUrl != null && !evt.posterUrl.isEmpty()) {
            loadImageInto(holder.eventImage, evt.posterUrl);
        } else {
            // fallback placeholder
            holder.eventImage.setImageResource(R.drawable.sample_event);
        }

        holder.itemView.setOnClickListener(v -> {
            int old = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            if (old != RecyclerView.NO_POSITION) notifyItemChanged(old);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onEventSelected(evt.id);
            }
        });

        holder.btnArrow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(evt.id);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    private void loadImageInto(ImageView imageView, String urlString) {

        imageView.setTag(urlString);
        imageView.setImageResource(R.drawable.sample_event);

        imageExecutor.execute(() -> {
            Bitmap bmp = null;
            try {
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.connect();
                try (InputStream in = conn.getInputStream()) {
                    bmp = BitmapFactory.decodeStream(in);
                }
            } catch (Exception ignored) {}

            Bitmap finalBmp = bmp;
            mainHandler.post(() -> {
                if (finalBmp != null && urlString.equals(imageView.getTag())) {
                    imageView.setImageBitmap(finalBmp);
                }
            });
        });
    }
}
