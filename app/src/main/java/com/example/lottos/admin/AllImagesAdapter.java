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

public class AllImagesAdapter extends RecyclerView.Adapter<AllImagesAdapter.ImageViewHolder> {

    public interface OnImageClickListener {
        void onImageClick(String imageUrl);
        void onDeleteClick(EventImageData eventData);
    }

    private final List<EventImageData> eventImages;
    private final OnImageClickListener listener;

    public AllImagesAdapter(List<EventImageData> eventImages, OnImageClickListener listener) {
        this.eventImages = eventImages;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

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
                // We'll pass the whole object in case we need the ID later
                listener.onDeleteClick(currentItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return eventImages.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView tvEventName;
        TextView tvOrganizerName;
        ImageButton btnDelete;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivGridImage);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvOrganizerName = itemView.findViewById(R.id.tvOrganizerName);
            btnDelete = itemView.findViewById(R.id.btnDeleteImage);
        }
    }
}

