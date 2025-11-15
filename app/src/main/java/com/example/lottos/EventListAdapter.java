package com.example.lottos;

import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * A flexible adapter used by both OrganizerEventsScreen and EntrantEventsScreen.
 * The fragment decides:
 * - whether a join/leave button is shown
 * - what click actions do
 */
public class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.VH> {

    public interface Listener {
        void onEventClick(String eventId);
        void onJoinClick(String eventId);
        void onLeaveClick(String eventId);
    }

    public static class EventItem {
        public final String id;
        public final String name;
        public final boolean isOpen;
        public boolean isJoined;

        public EventItem(String id, String name, boolean isOpen, boolean isJoined) {
            this.id = id;
            this.name = name;
            this.isOpen = isOpen;
            this.isJoined = isJoined;
        }
    }

    private final List<EventItem> events;
    private final boolean showJoinButton;
    private final Listener listener;

    public EventListAdapter(List<EventItem> events, boolean showJoinButton, Listener listener) {
        this.events = events;
        this.showJoinButton = showJoinButton;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvName;
        Button btnJoin;

        VH(LinearLayout layout) {
            super(layout);
            tvName = (TextView) layout.getChildAt(0);
            btnJoin = (Button) layout.getChildAt(1);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LinearLayout layout = new LinearLayout(parent.getContext());
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setPadding(16, 16, 16, 16);

        TextView tv = new TextView(parent.getContext());
        layout.setLayoutParams(new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        tv.setTextSize(16);

        Button btn = new Button(parent.getContext());
        btn.setVisibility(showJoinButton ? ViewGroup.VISIBLE : ViewGroup.GONE);

        layout.addView(tv);
        layout.addView(btn);

        return new VH(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {

        EventItem evt = events.get(position);

        // Text for both screens
        holder.tvName.setText(
                (evt.isOpen ? "🟢 Open: " : "🔴 Closed: ") + evt.name
        );

        // Logic only active for entrant view
        if (showJoinButton) {
            holder.btnJoin.setEnabled(evt.isOpen);
            holder.btnJoin.setText(evt.isJoined ? "Leave Waitlist" : "Join Waitlist");

            holder.btnJoin.setOnClickListener(v -> {
                if (evt.isJoined) listener.onLeaveClick(evt.id);
                else listener.onJoinClick(evt.id);
            });
        }

        holder.itemView.setOnClickListener(v -> listener.onEventClick(evt.id));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }
}
