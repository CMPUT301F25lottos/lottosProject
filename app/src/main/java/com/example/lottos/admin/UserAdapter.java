package com.example.lottos.admin;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.lottos.databinding.ItemUserBinding;
import java.util.List;

/**
 * A RecyclerView.Adapter for displaying a list of users in the admin panel.
 * Each item shows the user's ID, the number of events they have joined,
 * and the number of events they have created. It also provides a delete button
 * for each user, with the action handled by a listener interface.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    /**
     * A data model class representing a single user in the list.
     * It holds the user's ID and statistics about their event participation.
     */
    public static class UserItem {
        /**
         * The unique identifier (username) for the user.
         */
        public final String userId;
        /**
         * The count of events the user has joined.
         */
        public int joinedEventCount = 0;
        /**
         * The count of events the user has created.
         */
        public int createdEventCount = 0;

        /**
         * Constructs a new UserItem.
         * @param userId The unique identifier for the user.
         */
        public UserItem(String userId) {
            this.userId = userId;
        }
    }

    private final List<UserItem> userItemList;
    private final OnItemClickListener onDeleteClickListener;

    /**
     * An interface for receiving click events on items in the RecyclerView.
     */
    public interface OnItemClickListener {
        /**
         * Called when an item's delete button is clicked.
         * @param item The UserItem associated with the clicked item.
         */
        void onItemClick(UserItem item);
    }

    /**
     * Constructs the UserAdapter.
     * @param userItemList The list of UserItem objects to display.
     * @param onDeleteClickListener The listener to handle delete button clicks.
     */
    public UserAdapter(List<UserItem> userItemList, OnItemClickListener onDeleteClickListener) {
        this.userItemList = userItemList;
        this.onDeleteClickListener = onDeleteClickListener;
    }

    /**
     * Called when RecyclerView needs a new ViewHolder of the given type to represent an item.
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new UserViewHolder that holds the view for each user item.
     */
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemUserBinding binding = ItemUserBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new UserViewHolder(binding);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method binds the data from the UserItem to the views in the UserViewHolder.
     * @param holder The UserViewHolder which should be updated.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserItem item = userItemList.get(position);
        holder.bind(item);

        holder.binding.btnDeleteUser.setOnClickListener(v -> {
            if (onDeleteClickListener != null) {
                onDeleteClickListener.onItemClick(item);
            }
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     * @return The total number of users in the list.
     */
    @Override
    public int getItemCount() {
        return userItemList.size();
    }

    /**
     * A ViewHolder that describes an item view and metadata about its place within the RecyclerView.
     * It holds the binding for the item_user layout.
     */
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserBinding binding;

        /**
         * Constructs the UserViewHolder.
         * @param binding The view binding for the item_user layout.
         */
        public UserViewHolder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Binds a UserItem object to the views in the ViewHolder.
         * This sets the text for the username and event counts.
         * @param item The UserItem containing the data to display.
         */
        public void bind(UserItem item) {
            binding.tvUserName.setText(item.userId);
            binding.tvEventsJoined.setText("Events Joined: " + item.joinedEventCount);
            binding.tvEventsCreated.setText("Events Created: " + item.createdEventCount);
        }
    }
}
