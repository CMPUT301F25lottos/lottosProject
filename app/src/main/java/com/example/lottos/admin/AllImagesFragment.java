package com.example.lottos.admin;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.lottos.R;
import com.example.lottos.databinding.FragmentAllImagesBinding;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

/**
 * A Fragment for administrators to view and manage all event posters in the system.
 * It displays posters in a grid format and provides functionality to delete them.
 * Deleting a poster removes it from Firebase Storage and clears the poster URL field
 * in the corresponding event document in Firestore.
 */
public class AllImagesFragment extends Fragment implements AllImagesAdapter.OnImageClickListener {

    private FragmentAllImagesBinding binding;
    private String userName;
    private AllImagesAdapter adapter;
    private final List<EventImageData> eventImageDataList = new ArrayList<>();
    private FirebaseFirestore db;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout is inflated and view binding and Firestore instances are initialized.
     * @param inflater The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAllImagesBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored in to the view.
     * This method retrieves arguments, sets up the RecyclerView and navigation buttons, and initiates fetching of event posters.
     * @param view The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            userName = AllImagesFragmentArgs.fromBundle(getArguments()).getUserName();
        }

        setupRecyclerView();
        setupNavButtons();
        fetchEventPosters();
    }

    /**
     * Initializes the RecyclerView with a GridLayoutManager and sets up the AllImagesAdapter.
     */
    private void setupRecyclerView() {
        adapter = new AllImagesAdapter(eventImageDataList, this);
        binding.rvImages.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvImages.setAdapter(adapter);
    }

    /**
     * Sets up the OnClickListener for all navigation buttons in the bottom bar.
     */
    private void setupNavButtons() {
        binding.btnHome.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(AllImagesFragmentDirections.actionAllImagesFragmentToHomeScreen(userName)));
        binding.btnProfile.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(AllImagesFragmentDirections.actionAllImagesFragmentToProfileScreen(userName)));
        binding.btnNotification.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(AllImagesFragmentDirections.actionAllImagesFragmentToNotificationScreen(userName)));
        binding.btnOpenEvents.setImageResource(R.drawable.outline_add_photo_alternate_24);
        binding.btnOpenEvents.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Refreshing images...", Toast.LENGTH_SHORT).show();
            fetchEventPosters();
        });
        binding.btnEventHistory.setImageResource(R.drawable.outline_article_person_24);
        binding.btnEventHistory.setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(AllImagesFragmentDirections.actionAllImagesFragmentToViewUsersScreen(userName)));
    }


    /**
     * Fetches event data from the "open events" collection in Firestore, extracts poster URLs
     * and associated metadata, and populates the RecyclerView.
     */
    private void fetchEventPosters() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.rvImages.setVisibility(View.GONE);

        db.collection("open events").get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;

                    binding.progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        eventImageDataList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String eventId = document.getId();
                            String url = document.getString("posterUrl");
                            String eventName = document.getString("eventName");
                            String organizer = document.getString("organizer");

                            if (url != null && !url.isEmpty()) {
                                eventImageDataList.add(new EventImageData(eventId, url, eventName, organizer));
                            }
                        }
                        adapter.notifyDataSetChanged();
                        binding.rvImages.setVisibility(View.VISIBLE);

                        if (eventImageDataList.isEmpty()) {
                            Toast.makeText(getContext(), "No event posters found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Error fetching images: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    /**
     * Called when the view previously created by onCreateView has been detached from the fragment.
     * This is where the view binding is cleaned up to prevent memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * Handles the click event on an image in the RecyclerView.
     * This implementation shows the image in a full-screen dialog.
     * @param imageUrl The URL of the clicked image.
     */
    @Override
    public void onImageClick(String imageUrl) {
        FullScreenImageDialog dialog = FullScreenImageDialog.newInstance(imageUrl);
        dialog.show(getParentFragmentManager(), "full_screen_image_dialog");
    }

    /**
     * Handles the click event on the delete button for an image.
     * This implementation shows a confirmation dialog before proceeding with deletion.
     * @param eventData The data object associated with the image to be deleted.
     */
    @Override
    public void onDeleteClick(EventImageData eventData) {
        showDeleteConfirmationDialog(eventData);
    }

    /**
     * Displays a confirmation dialog to the administrator before deleting an event poster.
     * @param eventData The data for the event whose poster is being considered for deletion.
     */
    private void showDeleteConfirmationDialog(EventImageData eventData) {
        if (getContext() == null) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Delete Poster")
                .setMessage("Are you sure you want to delete the poster for \"" + eventData.eventName + "\"? This cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    deleteImageFromFirebase(eventData);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Deletes the image file from Firebase Storage and then updates the event document in Firestore
     * to remove the poster URL.
     * @param eventData The data for the event whose poster is to be deleted.
     */
    private void deleteImageFromFirebase(EventImageData eventData) {
        if (getContext() == null || eventData.posterUrl == null || eventData.posterUrl.isEmpty()) {
            Toast.makeText(getContext(), "Image reference is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Deleting poster...", Toast.LENGTH_SHORT).show();

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imageRef = storage.getReferenceFromUrl(eventData.posterUrl);

        imageRef.delete().addOnSuccessListener(aVoid -> {
            DocumentReference eventDocRef = db.collection("open events").document(eventData.eventId);

            eventDocRef.update("posterUrl", null).addOnSuccessListener(aVoid2 -> {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Poster deleted successfully.", Toast.LENGTH_SHORT).show();

                int position = eventImageDataList.indexOf(eventData);
                if (position != -1) {
                    eventImageDataList.remove(position);
                    adapter.notifyItemRemoved(position);
                }

            }).addOnFailureListener(e -> {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Failed to update event document: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });

        }).addOnFailureListener(e -> {
            if (!isAdded()) return;
            Toast.makeText(getContext(), "Failed to delete image file: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
