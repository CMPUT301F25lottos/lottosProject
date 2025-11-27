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

    public interface Listener {
        void onEventClick(String eventId);      // arrow click
        void onEventSelected(String eventId);   // row click
    }

    public static class EventItem {
        public final String id;
        public final String name;
        public final boolean isOpen;

        public final String location;
        public final String startTimeText;
        public final String endTimeText;

        public EventItem(String id, String name, boolean isOpen) {
            this(id, name, isOpen, null, null, null);
        }

        public EventItem(String id, String name, boolean isOpen, String location) {
            this(id, name, isOpen, location, null, null);
        }

        public EventItem(String id,
                         String name,
                         boolean isOpen,
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

    private int selectedPosition = RecyclerView.NO_POSITION;

    public EventListAdapter(List<EventItem> events, Listener listener) {
        this.events = events;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEventName, tvTime, tvLocation;
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
        holder.tvLocation.setText(evt.location != null ? evt.location : "");

        if (evt.startTimeText != null && evt.endTimeText != null) {
            holder.tvTime.setText(evt.startTimeText + " - " + evt.endTimeText);
        } else {
            holder.tvTime.setText("");
        }

        // Highlight selected item
        holder.itemView.setBackgroundColor(
                position == selectedPosition ? 0x220000FF : 0x00000000
        );

        // Row click = select event
        holder.itemView.setOnClickListener(v -> {
            int old = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            notifyItemChanged(old);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onEventSelected(evt.id);
            }
        });

        // Arrow click = open details
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
}
