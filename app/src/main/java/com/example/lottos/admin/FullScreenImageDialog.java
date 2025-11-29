package com.example.lottos.admin;

import android.os.Bundle;
import android.view.LayoutInflater;import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.lottos.ImageLoader;
import com.example.lottos.R;
import com.example.lottos.databinding.DialogFullScreenImageBinding;

public class FullScreenImageDialog extends DialogFragment {

    private static final String ARG_IMAGE_URL = "image_url";
    private DialogFullScreenImageBinding binding;


    public static FullScreenImageDialog newInstance(String imageUrl) {
        FullScreenImageDialog fragment = new FullScreenImageDialog();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogFullScreenImageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            String imageUrl = getArguments().getString(ARG_IMAGE_URL);
            ImageLoader.load(imageUrl, binding.ivFullScreen, R.drawable.sample_event);
        }

        binding.btnClose.setOnClickListener(v -> dismiss());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
