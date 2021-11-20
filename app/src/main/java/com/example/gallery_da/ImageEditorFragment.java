package com.example.gallery_da;

import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.example.gallery_da.databinding.FragmentImageeditorBinding;
import com.example.gallery_da.databinding.LayoutChipchoicebuttonBinding;
import com.example.gallery_da.viewmodels.ImageViewModel;
import com.example.gallery_da.viewmodels.ImagesViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.slider.Slider;

import java.text.DecimalFormat;

public class ImageEditorFragment extends Fragment {
    private FragmentImageeditorBinding binding;
    private RequestManager mGlide;
    private Target<Bitmap> mImageViewTarget;

    private ImagesViewModel mImagesViewModel;

    private Bitmap mEditorBitmap;
    private ColorMatrix mFilterColorMatrix = new ColorMatrix();
    private ColorMatrix mCustomColorMatrix = new ColorMatrix();
    private float mCustomSaturation = 1f;
    private float mCustomBrightness = 0f;
    private float mCustomContrast = 1f;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        binding = FragmentImageeditorBinding.inflate(inflater, container, false);

        mImageViewTarget = new ImageViewTarget<Bitmap>(binding.imageView) {
            @Override
            protected void setResource(@Nullable Bitmap resource) {
                // NOTE: Error setting nullable bitmap with ImageView
                mEditorBitmap = resource;
                this.view.setImageDrawable(new BitmapDrawable(this.view.getResources(), mEditorBitmap));
            }
        };

        binding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParentFragmentManager().popBackStack();
            }
        });

        // Set toggle actions
        binding.editorActionbar.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                // Toggle editor view visibility
                binding.filterChipgroup.setVisibility(checkedId == R.id.filters_chip ? View.VISIBLE : View.GONE);
                binding.tunableSliders.getRoot().setVisibility(checkedId == R.id.manualedit_chip ? View.VISIBLE : View.GONE);
            }
        });
        binding.filterChipgroup.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                if (checkedId != View.NO_ID) {
                    Chip selected = group.findViewById(checkedId);
                    ImageFilters selectedFilter = (ImageFilters) selected.getTag();
                    // Handle filter action
                    setImageFilter(selectedFilter);
                }
            }
        });

        // Initialize filter group
        binding.filterChipgroup.setSingleSelection(true);
        for (ImageFilters filterType : ImageFilters.values()) {
            Chip chip = LayoutChipchoicebuttonBinding.inflate(LayoutInflater.from(binding.filterChipgroup.getContext())).getRoot();
            chip.setId(View.generateViewId());

            switch (filterType) {
                default:
                case ORIGINAL:
                    chip.setText(R.string.filter_none);
                    break;
                case GRAYSCALE:
                    chip.setText(R.string.filter_grayscale);
                    break;
                case SEPIA:
                    chip.setText(R.string.filter_sepia);
                    break;
            }

            chip.setTag(filterType);

            binding.filterChipgroup.addView(chip);
            if (filterType == ImageFilters.ORIGINAL) {
                binding.filterChipgroup.check(chip.getId());
            }
        }
        binding.filterChipgroup.setSelectionRequired(true);

        // Set selection programmatically
        binding.editorActionbar.setSingleSelection(true);
        binding.editorActionbar.check(R.id.filters_chip);
        binding.editorActionbar.setSelectionRequired(true);

        // Configure tunable sliders
        DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance();
        df.applyPattern("0");
        binding.tunableSliders.brightnessSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                binding.tunableSliders.brightnessValue.setText(String.format("%s", df.format(value)));
                // Range is -100 -> 100
                // Default range is the same
                setCustomBrightness(value);
            }
        });
        binding.tunableSliders.contrastSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                binding.tunableSliders.contrastValue.setText(String.format("%s", df.format(value)));
                // Range is -100 -> 100
                // Default value should be 1.0; range: 0 -> 2
                // Adjust value as needed
                setCustomContrast((value + 100f) / 100f);
            }
        });
        binding.tunableSliders.saturationSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                binding.tunableSliders.saturationValue.setText(String.format("%s", df.format(value)));
                // Range is -100 -> 100
                // Default value should be 1.0; range: 0 -> 2
                // Adjust value as needed
                setCustomSaturation((value + 100f) / 100f);
            }
        });
        binding.tunableSliders.brightnessValue.setText(df.format(0));
        binding.tunableSliders.contrastValue.setText(df.format(0));
        binding.tunableSliders.saturationValue.setText(df.format(0));

        return binding.getRoot();
    }

    private void setCustomBrightness(float value) {
        mCustomBrightness = value;
        updateCustomColorMatrix();
    }

    private void setCustomContrast(float value) {
        mCustomContrast = value;
        updateCustomColorMatrix();
    }

    private void setCustomSaturation(float value) {
        mCustomSaturation = value;
        updateCustomColorMatrix();
    }

    // https://docs.rainmeter.net/tips/colormatrix-guide/
    private static void setSaturation(@NonNull ColorMatrix colorMatrix, float value) {
        ColorMatrix saturationMatrix = new ColorMatrix();
        saturationMatrix.setSaturation(value);

        colorMatrix.postConcat(saturationMatrix);
    }

    private static void setBrightness(@NonNull ColorMatrix colorMatrix, float value) {
        float[] matrix = new float[]
                {
                        1, 0, 0, 0, value, // R
                        0, 1, 0, 0, value, // G
                        0, 0, 1, 0, value, // B
                        0, 0, 0, 1, 0, // A
                        0, 0, 0, 0, 1 // W
                };
        colorMatrix.postConcat(new ColorMatrix(matrix));
    }

    private static void setContrast(@NonNull ColorMatrix colorMatrix, float value) {
        final float c = value;
        final float t = (1.0f - c) / 2.0f;

        float[] matrix = new float[]
                {
                        c, 0, 0, 0, t,
                        0, c, 0, 0, t,
                        0, 0, c, 0, t,
                        0, 0, 0, 1, 0,
                        0, 0, 0, 0, 1
                };
        colorMatrix.postConcat(new ColorMatrix(matrix));
    }

    private void updateCustomColorMatrix() {
        ColorMatrix matrix = new ColorMatrix();
        setContrast(matrix, mCustomContrast);
        setBrightness(matrix, mCustomBrightness);
        setSaturation(matrix, mCustomSaturation);
        mCustomColorMatrix = matrix;
        updateImageViewFilter();
    }

    private void setImageFilter(@NonNull ImageFilters selectedFilter) {
        mFilterColorMatrix.reset();

        switch (selectedFilter) {
            default:
            case ORIGINAL:
                break;
            case GRAYSCALE: {
                // Grayscale -> Saturation = 0
                // https://stackoverflow.com/a/29720907
                mFilterColorMatrix.setSaturation(0f);
                break;
            }
            case SEPIA: {
                // https://stackoverflow.com/a/9149010
                final ColorMatrix matrixA = new ColorMatrix();
                // making image B&W
                matrixA.setSaturation(0);

                final ColorMatrix matrixB = new ColorMatrix();
                // applying scales for RGB color values
                matrixB.setScale(1f, .95f, .82f, 1.0f);
                matrixA.setConcat(matrixB, matrixA);

                mFilterColorMatrix = matrixA;
                break;
            }
        }

        updateImageViewFilter();
    }

    private void updateImageViewFilter() {
        binding.imageView.setColorFilter(new ColorMatrixColorFilter(getCombinedColorMatrix()));
    }

    private ColorMatrix getCombinedColorMatrix() {
        final ColorMatrix matrix = new ColorMatrix();
        matrix.setConcat(mFilterColorMatrix, mCustomColorMatrix);
        return matrix;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mImagesViewModel = new ViewModelProvider(requireActivity()).get(ImagesViewModel.class);
        mImagesViewModel.getEditorImageData().observe(getViewLifecycleOwner(), new Observer<ImageViewModel>() {
            @Override
            public void onChanged(ImageViewModel imageViewModel) {
                if (imageViewModel.getImageBitmap() == null) {
                    mGlide.asBitmap()
                            .load(imageViewModel.getImageUrl())
                            .dontTransform()
                            //.transition(BitmapTransitionOptions.withCrossFade())
                            .override(Target.SIZE_ORIGINAL)
                            .listener(new RequestListener<Bitmap>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
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
                                    return false;
                                }
                            })
                            .into(mImageViewTarget);
                } else {
                    binding.imageView.setImageDrawable(new BitmapDrawable(binding.imageView.getResources(), imageViewModel.getImageBitmap()));
                }
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
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
