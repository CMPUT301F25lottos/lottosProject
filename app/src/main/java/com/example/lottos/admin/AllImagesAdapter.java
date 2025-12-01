package com.example.lottos.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottos.ImageLoader;
import com.example.lottos.R;

import java.util.List;

/**
 * A RecyclerView adapter for displaying a grid of event images.
 * Each item in the grid shows an event's poster, its name, the organizer's name,
 * and a delete button. This adapter is used in the admin section to manage all event images.
 * It uses an interface to delegate click and delete actions back to the hosting Fragment or Activity.
 */
public class AllImagesAdapter extends RecyclerView.Adapter<AllImagesAdapter.ImageViewHolder> {

    /**
     * Interface for handling click events on items in the RecyclerView.
     */
    public interface OnImageClickListener {
        /**
         * Called when an image is clicked.
         * @param imageUrl The URL of the clicked image.
         */
        void onImageClick(String imageUrl);

        /**
         * Called when the delete button for an image is clicked.
         * @param eventData The data object associated with the image to be deleted.
         */
        void onDeleteClick(EventImageData eventData);
    }

    private final List<EventImageData> eventImages;
    private final OnImageClickListener listener;

    /**
     * Constructs the adapter.
     * @param eventImages A list of EventImageData objects to be displayed.
     * @param listener The listener that will handle item click events.
     */
    public AllImagesAdapter(List<EventImageData> eventImages, OnImageClickListener listener) {
        this.eventImages = eventImages;
        this.listener = listener;
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ImageViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the ViewHolder to reflect the item at the given position.
     * @param holder The ImageViewHolder which should be updated to represent the contents of the item at the given position.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        EventImageData currentItem = eventImages.get(position);

        holder.tvEventName.setText(currentItem.eventName);
        holder.tvOrganizerName.setText("by " + currentItem.organizerName);
        ImageLoader.load(currentItem.posterUrl, holder.imageView, R.drawable.sample_event);


        holder.imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(currentItem.posterUrl);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteClick(currentItem);
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return eventImages.size();
    }

    /**
     * A ViewHolder that describes an item view and metadata about its place within the RecyclerView.
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvEventName;
        TextView tvOrganizerName;
        ImageButton btnDelete;

        /**
         * Constructs the ImageViewHolder.
         * @param itemView The view that represents a single item in the list.
         */
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivGridImage);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvOrganizerName = itemView.findViewById(R.id.tvOrganizerName);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}
