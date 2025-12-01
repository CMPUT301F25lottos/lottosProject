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
        // MODIFIED: The listener now requires the position for reliable deletion
        void onDelete(NotificationItem item, int position);
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
    private String currentUserName; // NEW: Field to store the name of the logged-in user

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

    /**
     * NEW: Call this from NotificationScreen to provide the current user's name.
     */
    public void setCurrentUserName(String userName) {
        this.currentUserName = userName;
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
        holder.tvSender.setVisibility(View.VISIBLE); // Always show the sender view

        // --- MODIFIED: New logic to determine what to show in the sender text field ---
        if (isAdminView) {
            // For Admins, show the full path: "From [sender] to [receiver]"
            holder.tvSender.setText("From: " + n.sender + "  |  To: " + n.receiver);
        } else {
            // For Organizers/Regular Users
            if (currentUserName != null && currentUserName.equals(n.sender)) {
                // If the current user SENT this notification
                holder.tvSender.setText("Sent by you");
            } else {
                // If the current user RECEIVED this notification
                holder.tvSender.setText("From: " + n.sender);
            }
        }
        // --- End of Modified Block ---


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
                    listener.onDelete(notifications.get(pos), pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }
}

