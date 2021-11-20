package com.example.gallery_da;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.ImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.example.gallery_da.databinding.FragmentImageviewBinding;
import com.example.gallery_da.viewmodels.ImageViewModel;
import com.example.gallery_da.viewmodels.ImagesViewModel;
import com.google.android.material.transition.MaterialContainerTransform;

public class ImageViewFragment extends Fragment {
    private FragmentImageviewBinding binding;
    private RequestManager mGlide;
    private Target<Bitmap> mImageViewTarget;

    private ImagesViewModel mImagesViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setSharedElementEnterTransition(new MaterialContainerTransform());

        mGlide = Glide.with(this);

        requireActivity().getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                this.remove();
                getParentFragmentManager().popBackStack();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentImageviewBinding.inflate(inflater, container, false);

        Drawable navIcon = binding.toolbar.getNavigationIcon();
        if (navIcon != null) {
            Drawable wrapped = DrawableCompat.wrap(navIcon);
            DrawableCompat.setTint(wrapped, Color.WHITE);
            binding.toolbar.setNavigationIcon(wrapped);
        }

        binding.toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().popBackStack();
            }
        });

        mImageViewTarget = new ImageViewTarget<Bitmap>(binding.imageView) {
            @Override
            protected void setResource(@Nullable Bitmap resource) {
                // NOTE: Error setting nullable bitmap with ImageView
                this.view.setImageDrawable(new BitmapDrawable(this.view.getResources(), resource));
            }
        };

        binding.editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ImageEditorFragment(), "imageeditor")
                        .addToBackStack("imageeditor")
                        .setReorderingAllowed(true)
                        .commit();
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        postponeEnterTransition();
        mImagesViewModel = new ViewModelProvider(requireActivity()).get(ImagesViewModel.class);
        mImagesViewModel.getEditorImageData().observe(getViewLifecycleOwner(), new Observer<ImageViewModel>() {
            @Override
            public void onChanged(ImageViewModel imageViewModel) {
                mGlide.asBitmap()
                        .load(imageViewModel.getImageUrl())
                        .dontTransform()
                        //.transition(BitmapTransitionOptions.withCrossFade())
                        .override(Target.SIZE_ORIGINAL)
                        .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                startPostponedEnterTransition();
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                LiveData<ImageViewModel> data = mImagesViewModel.getEditorImageData();
                                if (data != null) {
                                    ImageViewModel imageData = data.getValue();
                                    if (imageData != null) {
                                        imageData.setImageBitmap(resource);
                                    }
                                }

                                startPostponedEnterTransition();
                                return false;
                            }
                        })
                        .into(mImageViewTarget);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mGlide.clear(mImageViewTarget);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
