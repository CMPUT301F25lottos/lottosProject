package com.example.lottos.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lottos.databinding.ItemUserBinding;
import java.util.List;

// This adapter is now simple and correct. It just displays the data.
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    // This is an inner class to hold the data for each user row.
    public static class UserItem {
        public final String userId; // The document ID from Firestore
        public int joinedEventCount = 0;
        public int createdEventCount = 0;

        public UserItem(String userId) {
            this.userId = userId;
        }
    }

    private final List<UserItem> userItemList;
    private final OnItemClickListener onDeleteClickListener; // Listener for the delete button

    public interface OnItemClickListener {
        void onItemClick(UserItem item);
    }

    // The constructor now only takes the list and ONE listener for the delete action.
    public UserAdapter(List<UserItem> userItemList, OnItemClickListener onDeleteClickListener) {
        this.userItemList = userItemList;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserItem item = userItemList.get(position);
        holder.bind(item);

        // Set the listener specifically for the delete button.
        // This is the pattern from EventDetailsScreen.
        holder.binding.btnDeleteUser.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userItemList.size();
    }

    // The ViewHolder now correctly binds the UserItem data.
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserBinding binding;

        public UserViewHolder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        // Inside the public static class UserViewHolder ...

        public void bind(UserItem item) {
            // Set the user's name/ID
            binding.tvUserName.setText(item.userId);

            binding.tvEventsJoined.setText("Events Joined: " + item.joinedEventCount);
            binding.tvEventsCreated.setText("Events Created: " + item.createdEventCount);
        }


    }
}




