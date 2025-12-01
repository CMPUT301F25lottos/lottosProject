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

/**
 * A RecyclerView.Adapter for displaying a list of notifications.
 *
 * Role: This adapter binds a list of `NotificationItem` data objects to the UI elements
 * in a RecyclerView. It's responsible for creating and managing the ViewHolders for each
 * notification item. It supports two distinct view modes: a standard user view and an
 * admin view, which alters how sender/receiver information is displayed. It communicates
 * user interactions, such as clicking on a notification or deleting it, back to the
 * hosting Fragment or Activity through a `Listener` interface.
 */
public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    /**
     * An interface to notify the hosting component of user actions on a notification item.
     */
    public interface Listener {
        /**
         * Called when a user clicks anywhere on a notification item.
         * @param item The NotificationItem that was clicked.
         */
        void onNotificationClick(NotificationItem item);

        /**
         * Called when a user clicks the delete button on a notification item.
         * @param item The NotificationItem to be deleted.
         * @param position The adapter position of the item, used for efficient removal.
         */
        void onDelete(NotificationItem item, int position);
    }

    /**
     * A simple data class representing a single notification.
     * This class holds all the displayable information for one notification item.
     */
    public static class NotificationItem {
        public final String id;
        public final String content;
        public final String eventName;
        public final String receiver;
        public final String sender;
        public final String timestamp;

        /**
         * Constructs a new NotificationItem.
         * @param id The unique ID of the notification document.
         * @param content The main message content of the notification.
         * @param eventName The name of the event associated with the notification.
         * @param receiver The username of the recipient.
         * @param sender The username of the sender.
         * @param timestamp A formatted string representing when the notification was created.
         */
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
    private String currentUserName;

    /**
     * Constructs the NotificationAdapter.
     * @param notifications The list of NotificationItem objects to be displayed.
     * @param listener The listener that will handle user interactions.
     */
    public NotificationAdapter(List<NotificationItem> notifications, Listener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    /**
     * Configures the adapter to operate in admin view mode.
     * In admin mode, both the sender and receiver are displayed.
     * @param isAdmin True to enable admin view, false for standard user view.
     */
    public void setAdminView(boolean isAdmin) {
        this.isAdminView = isAdmin;
    }

    /**
     * Provides the adapter with the username of the currently logged-in user.
     * This is used to determine how to display sender information in the standard user view.
     * @param userName The username of the current user.
     */
    public void setCurrentUserName(String userName) {
        this.currentUserName = userName;
    }

    /**
     * The ViewHolder class for a notification item.
     * It holds references to the UI views within the item layout.
     */
    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvEventName;
        TextView tvMessage;
        ImageButton btnDelete;
        TextView tvSender;

        /**
         * Constructs a new ViewHolder.
         * @param itemView The root view of the item layout.
         */
        VH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvEventName = itemView.findViewById(R.id.TVEvent);
            tvMessage = itemView.findViewById(R.id.TVMessage);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            tvSender = itemView.findViewById(R.id.tvSender);
        }
    }

    /**
     * Called when the RecyclerView needs a new ViewHolder of the given type to represent an item.
     * This method inflates the item layout XML and creates the ViewHolder.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new VH that holds a View of the given view type.
     */
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(view);
    }

    /**
     * Called by the RecyclerView to display the data at the specified position.
     * This method populates the views within the ViewHolder with data from the
     * NotificationItem at the given position and sets up click listeners.
     * @param holder The ViewHolder which should be updated to represent the contents of the item.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationItem n = notifications.get(position);

        holder.tvEventName.setText(n.eventName);
        holder.tvMessage.setText(n.content);
        holder.tvSender.setVisibility(View.VISIBLE);

        // Logic to determine what to show in the sender text field.
        if (isAdminView) {
            // For Admins, show the full path: "From [sender] to [receiver]".
            holder.tvSender.setText("From: " + n.sender + "  |  To: " + n.receiver);
        } else {
            // For Organizers/Regular Users.
            if (currentUserName != null && currentUserName.equals(n.sender)) {
                // If the current user SENT this notification.
                holder.tvSender.setText("Sent by you");
            } else {
                // If the current user RECEIVED this notification.
                holder.tvSender.setText("From: " + n.sender);
            }
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
                    listener.onDelete(notifications.get(pos), pos);
                }
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of notifications in the list.
     */
    @Override
    public int getItemCount() {
        return notifications.size();
    }
}
