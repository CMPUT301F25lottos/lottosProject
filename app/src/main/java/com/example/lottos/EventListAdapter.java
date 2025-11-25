package com.example.lottos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.VH> {

    // Simple click listener for "go to details"
    public interface Listener {
        void onEventClick(String eventId);
    }

    public static class EventItem {
        public final String id;
        public final String name;
        public final boolean isOpen;

        public final String location;
        public final String startTimeText; // optional
        public final String endTimeText;   // optional

        // Old constructor: still works
        public EventItem(String id, String name, boolean isOpen) {
            this(id, name, isOpen, null, null, null);
        }

        // New: id + name + isOpen + location
        public EventItem(String id, String name, boolean isOpen, String location) {
            this(id, name, isOpen, location, null, null);
        }

        // Full constructor (if you later want times too)
        public EventItem(String id, String name, boolean isOpen,
                         String location,
                         String startTimeText,
                         String endTimeText) {
            this.id = id;
            this.name = name;
            this.isOpen = isOpen;
            this.location = location;
            this.startTimeText = startTimeText;
            this.endTimeText = endTimeText;
        }

    }

    private final List<EventItem> events;
    private final Listener listener;

    public EventListAdapter(List<EventItem> events, Listener listener) {
        this.events = events;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEventName;
        TextView tvTime;
        TextView tvLocation;
        ImageButton btnArrow;

        VH(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            btnArrow = itemView.findViewById(R.id.btnArrow);
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

        holder.tvLocation.setText(
                evt.location != null ? evt.location : ""
        );

        String timeRange = "";
        if (evt.startTimeText != null || evt.endTimeText != null) {
            String start = evt.startTimeText != null ? evt.startTimeText : "N/A";
            String end = evt.endTimeText != null ? evt.endTimeText : "N/A";
            timeRange = start + " - " + end;
        }

        holder.tvTime.setText(timeRange);

        // click → go to details
        View.OnClickListener clickListener = v -> {
            if (listener != null) {
                listener.onEventClick(evt.id);
            }
        };

        holder.itemView.setOnClickListener(clickListener);
        if (holder.btnArrow != null) {
            holder.btnArrow.setOnClickListener(clickListener);
        }
    }

    @Override
    public int getItemCount() {
        return events.size();
    }
}
