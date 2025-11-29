package com.example.lottos.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottos.R;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface Listener {
        void onNotificationClick(NotificationItem item);
        void onDelete(NotificationItem item);
    }

    public static class NotificationItem {
        public final String id;
        public final String content;
        public final String eventName;
        public final String receiver;
        public final String sender;
        public final String timestamp;

        public NotificationItem(String id, String content, String eventName, String receiver, String sender, String timestamp) {
            this.id = id;
            this.content = content;
            this.eventName = eventName;
            this.receiver = receiver;
            this.sender = sender;
            this.timestamp = timestamp;
        }
    }

    private final List<NotificationItem> notifications;
    private final Listener listener;
    private boolean isAdminView = false;

    public NotificationAdapter(List<NotificationItem> notifications, Listener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    /**
     * Call this from NotificationScreen to tell the adapter it's in admin mode.
     */
    public void setAdminView(boolean isAdmin) {
        this.isAdminView = isAdmin;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvEventName;
        TextView tvMessage;
        ImageButton btnDelete;

        TextView tvSender;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvEventName = itemView.findViewById(R.id.TVEvent);
            tvMessage = itemView.findViewById(R.id.TVMessage);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            tvSender = itemView.findViewById(R.id.tvSender);

        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationItem n = notifications.get(position);

        holder.tvEventName.setText(n.eventName);
        holder.tvMessage.setText(n.content);


        if (isAdminView) {
            // If it's the admin view, make the sender TextView visible
            holder.tvSender.setVisibility(View.VISIBLE);
            // And set its text to show who sent it
            holder.tvSender.setText("from: " + n.sender);
        } else {
            // For regular users, make sure it is hidden
            holder.tvSender.setVisibility(View.GONE);
        }


        if (n.timestamp != null && !n.timestamp.isEmpty()) {
            holder.tvDate.setText(n.timestamp);
        } else {
            holder.tvDate.setText("");
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onNotificationClick(notifications.get(pos));
                }
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onDelete(notifications.get(pos));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }
}
