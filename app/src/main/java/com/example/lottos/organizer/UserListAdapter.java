package com.example.lottos.organizer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.lottos.R;

import java.util.List;

/**
 * An adapter for displaying a simple list of user names in a RecyclerView.
 * This adapter is used in the organizer's view to show lists of users,
 * such as those on a waitlist or those selected for an event.
 */
public class UserListAdapter extends RecyclerView.Adapter<UserListAdapter.ViewHolder> {

    /** The list of user names to be displayed. */
    private final List<String> users;

    /**
     * Constructs a new UserListAdapter.
     *
     * @param users A list of strings, where each string is a user's name
     *              to be displayed in the RecyclerView.
     */
    public UserListAdapter(List<String> users) {
        this.users = users;
    }

    /**
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item. This new ViewHolder will be used to display items of the adapter using
     * onBindViewHolder.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_list_row, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position. This method
     * updates the contents of the {@link ViewHolder#itemView} to reflect the item at the
     * given position.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvUserName.setText(users.get(position));
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of users in the list.
     */
    @Override
    public int getItemCount() {
        return users.size();
    }

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     * It holds the UI components for a single row in the user list.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        /** The TextView used to display the user's name. */
        TextView tvUserName;

        /**
         * Constructs a new ViewHolder.
         *
         * @param itemView The view for a single item row, which this ViewHolder will hold.
         */
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tvUserName);
        }
    }
}
