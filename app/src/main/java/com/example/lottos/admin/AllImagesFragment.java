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

public class AllImagesFragment extends Fragment implements AllImagesAdapter.OnImageClickListener {

    private FragmentAllImagesBinding binding;
    private String userName;
    private AllImagesAdapter adapter;
    private final List<EventImageData> eventImageDataList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAllImagesBinding.inflate(inflater, container, false);
        db = FirebaseFirestore.getInstance();
        return binding.getRoot();
    }

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

    private void setupRecyclerView() {
        adapter = new AllImagesAdapter(eventImageDataList, this);
        binding.rvImages.setLayoutManager(new GridLayoutManager(getContext(), 2));
        binding.rvImages.setAdapter(adapter);
    }

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onImageClick(String imageUrl) {
        FullScreenImageDialog dialog = FullScreenImageDialog.newInstance(imageUrl);
        dialog.show(getParentFragmentManager(), "full_screen_image_dialog");
    }

    @Override
    public void onDeleteClick(EventImageData eventData) {
        showDeleteConfirmationDialog(eventData);
    }

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
