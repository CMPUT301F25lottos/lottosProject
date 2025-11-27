package com.example.lottos.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottos.R;
import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface Listener {
        void onNotificationClick(NotificationItem item);
        void onDelete(NotificationItem item);
    }

    public static class NotificationItem {
        public final String id;     // <- used for delete
        public final String content;
        public final String eventName;
        public final String receiver;
        public final String sender;
        public final String timestamp;


        public NotificationItem(String id,
                                String content,
                                String eventName,
                                String receiver,
                                String sender,
                                String timestamp) {

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

    public NotificationAdapter(List<NotificationItem> notifications,
                               Listener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvEventName;   // TVEvent
        TextView tvMessage;     // TVMessage
        ImageButton btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvEventName = itemView.findViewById(R.id.TVEvent);
            tvMessage = itemView.findViewById(R.id.TVMessage);
            btnDelete = itemView.findViewById(R.id.btnDelete);
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

        // event name + message
        holder.tvEventName.setText(n.eventName);
        holder.tvMessage.setText(n.content);

        // Format date as:
        // NOV
        // 26
        if (n.timestamp != null) {

            try {
                SimpleDateFormat parser =
                        new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
                Date date = parser.parse(n.timestamp);

                if (date != null) {
                    SimpleDateFormat monthFmt = new SimpleDateFormat("MMM", Locale.getDefault());
                    SimpleDateFormat dayFmt = new SimpleDateFormat("dd", Locale.getDefault());

                    String month = monthFmt.format(date).toUpperCase();
                    String day = dayFmt.format(date);

                    holder.tvDate.setText(month + "\n" + day);
                } else {
                    holder.tvDate.setText("");
                }
            } catch (Exception e) {
                holder.tvDate.setText("");
            }

        }


        // Click item → open notification details (if needed)
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    listener.onNotificationClick(notifications.get(pos));
                }
            }
        });


        // Delete button
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