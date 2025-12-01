package com.example.lottos.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.lottos.ImageLoader;
import com.example.lottos.R;
import com.example.lottos.databinding.DialogFullScreenImageBinding;

/**
 * A DialogFragment that displays a single image in a full-screen view.
 * It takes an image URL as an argument and loads the image using a utility class.
 * This dialog includes a close button to dismiss the view. It is styled to cover the entire screen.
 */
public class FullScreenImageDialog extends DialogFragment {

    private static final String ARG_IMAGE_URL = "image_url";
    private DialogFullScreenImageBinding binding;


    /**
     * Creates a new instance of FullScreenImageDialog with the specified image URL.
     * This factory method is the recommended way to instantiate the dialog, as it handles passing arguments correctly.
     *
     * @param imageUrl The URL of the image to be displayed.
     * @return A new instance of FullScreenImageDialog.
     */
    public static FullScreenImageDialog newInstance(String imageUrl) {
        FullScreenImageDialog fragment = new FullScreenImageDialog();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Called when the dialog is being created.
     * This is where the dialog's style is set to a full-screen theme.
     *
     * @param savedInstanceState If the fragment is being re-created from a previous saved state, this is the state.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the style of the dialog to be full-screen
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where the layout for the dialog is inflated and the view binding is initialized.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogFullScreenImageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    /**
     * Called immediately after onCreateView has returned, but before any saved state has been restored into the view.
     * This method retrieves the image URL from the arguments and loads the image into the ImageView.
     * It also sets up the click listener for the close button.
     *
     * @param view               The View returned by onCreateView.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String imageUrl = getArguments().getString(ARG_IMAGE_URL);
            // Use ImageLoader utility to load the image from the URL into the ImageView
            ImageLoader.load(imageUrl, binding.ivFullScreen, R.drawable.sample_event);
        }

        // Set up the close button to dismiss the dialog
        binding.btnClose.setOnClickListener(v -> dismiss());
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
}
