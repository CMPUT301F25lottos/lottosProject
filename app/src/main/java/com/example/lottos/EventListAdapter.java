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

/**
 * A RecyclerView.Adapter for displaying a list of events.
 *
 * Role: This adapter is responsible for taking a list of `EventItem` data objects
 * and binding them to the visual elements defined in the `item_event.xml` layout.
 * It manages the creation and recycling of views for efficiency. Key features include:
 * <ul>
 *     <li>Displaying event details such as name, location, and time.</li>
 *     <li>Asynchronously loading and displaying a poster image from a URL.</li>
 *     <li>Handling user interactions, like clicks on the entire item or a specific button,
 *         and communicating these events back through a `Listener` interface.</li>
 *     <li>Highlighting the currently selected item in the list.</li>
 * </ul>
 */
public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.VH> {

    /**
     * An interface to notify a listening component (like a Fragment) of user interactions.
     */
    public interface Listener {
        /**
         * Called when a user clicks the primary "details" button (e.g., an arrow) on an event item.
         * @param eventId The unique ID of the clicked event.
         */
        void onEventClick(String eventId);

        /**
         * Called when a user clicks anywhere on an event item's main body to select it.
         * @param eventId The unique ID of the selected event.
         */
        void onEventSelected(String eventId);
    }

    /**
     * A static inner class that serves as a simple data model for a single event item.
     * This class holds all the displayable information for one row in the RecyclerView.
     */
    public static class EventItem {
        public final String id;
        public final String name;
        public final boolean isOpen;
        public final String location;
        public final String startTimeText;
        public final String endTimeText;
        public final String posterUrl;

        /**
         * Constructs a basic EventItem with only an ID, name, and open status.
         * @param id The unique ID of the event.
         * @param name The name of the event.
         * @param isOpen A flag indicating if the event is open.
         */
        public EventItem(String id, String name, boolean isOpen) {
            this(id, name, isOpen, null, null, null, null);
        }

        /**
         * Constructs an EventItem with location information.
         * @param id The unique ID of the event.
         * @param name The name of the event.
         * @param isOpen A flag indicating if the event is open.
         * @param location The location of the event.
         */
        public EventItem(String id, String name, boolean isOpen, String location) {
            this(id, name, isOpen, location, null, null, null);
        }

        /**
         * Constructs an EventItem with location and time information.
         * @param id The unique ID of the event.
         * @param name The name of the event.
         * @param isOpen A flag indicating if the event is open.
         * @param location The location of the event.
         * @param startTimeText A formatted string for the event's start time.
         * @param endTimeText A formatted string for the event's end time.
         */
        public EventItem(String id, String name, boolean isOpen, String location, String startTimeText, String endTimeText) {
            this(id, name, isOpen, location, startTimeText, endTimeText, null);
        }


        /**
         * The full constructor for an EventItem, including a URL for the poster image.
         * @param id The unique ID of the event.
         * @param name The name of the event.
         * @param isOpen A flag indicating if the event is open.
         * @param location The location of the event.
         * @param startTimeText A formatted string for the event's start time.
         * @param endTimeText A formatted string for the event's end time.
         * @param posterUrl The URL of the event's poster image.
         */
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

    /**
     * Constructs the EventListAdapter.
     * @param events The list of EventItem objects to be displayed.
     * @param listener The listener that will handle user interactions.
     */
    public EventListAdapter(List<EventItem> events, Listener listener) {
        this.events = events;
        this.listener = listener;
    }

    /**
     * The ViewHolder class for an event item.
     * It holds references to the UI views within the `item_event.xml` layout.
     */
    static class VH extends RecyclerView.ViewHolder {
        TextView tvEventName, tvTime, tvLocation;
        ImageButton btnArrow;
        ImageView eventImage;

        /**
         * Constructs a new ViewHolder and finds the views within the item layout.
         * @param itemView The root view of the item layout.
         */
        VH(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            btnArrow = itemView.findViewById(R.id.btnArrow);
            eventImage = itemView.findViewById(R.id.eventImage);
        }
    }

    /**
     * Called when the RecyclerView needs a new ViewHolder.
     * This method inflates the item layout XML and creates the ViewHolder.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new VH that holds the view for an event item.
     */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new VH(view);
    }

    /**
     * Called by the RecyclerView to display the data at the specified position.
     * This method populates the views in the ViewHolder with data from the EventItem
     * and sets up the item's click listeners.
     * @param holder The ViewHolder which should be updated.
     * @param position The position of the item within the adapter's data set.
     */
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

        // Set the background color to highlight the selected item.
        holder.itemView.setBackgroundColor(
                position == selectedPosition ? 0x220000FF : 0x00000000
        );

        // Load the event poster image if a URL is available.
        if (evt.posterUrl != null && !evt.posterUrl.isEmpty()) {
            loadImageInto(holder.eventImage, evt.posterUrl);
        } else {

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

        // Handle clicks on the arrow button to navigate to details.
        holder.btnArrow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEventClick(evt.id);
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of events in the list.
     */
    @Override
    public int getItemCount() {
        return events.size();
    }

    /**
     * Asynchronously loads an image from a URL into an ImageView.
     * This is done on a background thread to avoid blocking the UI.
     * A placeholder is shown initially, and the final image is set on the main thread.
     * @param imageView The ImageView to load the image into.
     * @param urlString The URL of the image to download.
     */
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
