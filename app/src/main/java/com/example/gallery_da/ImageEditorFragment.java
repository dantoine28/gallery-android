package com.example.gallery_da;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.example.gallery_da.data.UploadResponse;
import com.example.gallery_da.databinding.FragmentImageeditorBinding;
import com.example.gallery_da.databinding.LayoutChipchoicebuttonBinding;
import com.example.gallery_da.utils.AsyncTask;
import com.example.gallery_da.utils.HttpUtils;
import com.example.gallery_da.utils.JSONParser;
import com.example.gallery_da.viewmodels.ImageViewModel;
import com.example.gallery_da.viewmodels.ImagesViewModel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.slider.Slider;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.ortiz.touchview.OnTouchImageViewListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImageEditorFragment extends Fragment {
    private FragmentImageeditorBinding binding;
    private RequestManager mGlide;
    private Target<Bitmap> mImageViewTarget;

    private ImagesViewModel mImagesViewModel;
    private ImageViewModel mImageViewModel;

    private Bitmap mOriginalBitmap;
    private ColorMatrix mFilterColorMatrix = new ColorMatrix();
    private ColorMatrix mCustomColorMatrix = new ColorMatrix();
    private float mCustomSaturation = 1f;
    private float mCustomBrightness = 0f;
    private float mCustomContrast = 1f;

    private Bitmap mEditedBitmap;
    private final Paint mEditorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

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
                this.view.setImageDrawable(new BitmapDrawable(this.view.getResources(), resource));
                updateMarkupContainerMatrix();
            }
        };

        binding.cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(v.getContext())
                        .setMessage(R.string.message_discardchanges)
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                getParentFragmentManager().popBackStack();
                            }
                        })
                        .show();
            }
        });

        binding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Build bitmap image
                if (mEditedBitmap != null && !mEditedBitmap.isRecycled()) {
                    mEditedBitmap.recycle();
                    mEditedBitmap = null;
                }
                mEditedBitmap = buildFinalBitmap();

                // Confirm user upload
                new MaterialAlertDialogBuilder(v.getContext())
                        .setTitle(R.string.title_savechanges)
                        .setMessage(R.string.message_savechanges)
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                performBitmapUpload();
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });

        // Set toggle actions
        binding.editorActionbar.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                // Toggle editor view visibility
                binding.filterChipgroup.setVisibility(checkedId == R.id.filters_chip ? View.VISIBLE : View.GONE);
                binding.tunableSliders.getRoot().setVisibility(checkedId == R.id.manualedit_chip ? View.VISIBLE : View.GONE);
                binding.addtextButton.setVisibility(checkedId == R.id.markup_chip ? View.VISIBLE : View.GONE);
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

        // Text markup
        binding.addtextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                binding.textContainer.addView(new MarkupTextView(v.getContext()));
                updateMarkupContainerMatrix();
            }
        });

        binding.imageView.setOnTouchImageViewListener(new OnTouchImageViewListener() {
            @Override
            public void onMove() {
                updateMarkupContainerMatrix();
            }
        });

        return binding.getRoot();
    }

    private void updateMarkupContainerMatrix() {
        Matrix m = binding.imageView.getImageMatrix();

        float[] values = new float[9];
        m.getValues(values);

        // TODO: issue with text translation using gestures
        float translationX = values[Matrix.MTRANS_X];
        float translationY = values[Matrix.MTRANS_Y];
        float scaleX = values[Matrix.MSCALE_X];
        float scaleY = values[Matrix.MSCALE_Y];

        binding.textContainer.setScaleX(scaleX);
        binding.textContainer.setScaleY(scaleY);
        //binding.textContainer.setTranslationX(translationX);
        //binding.textContainer.setTranslationY(translationY);

        Drawable d = binding.imageView.getDrawable();
        if (d != null) {
            Rect bounds = d.getBounds();

            ViewGroup.LayoutParams lp = binding.textContainer.getLayoutParams();
            lp.height = (int) bounds.height();
            lp.width = (int) bounds.width();
            binding.textContainer.setLayoutParams(lp);
        }

        binding.textContainer.setTransformationMatrix(new Matrix(m));
    }

    private Bitmap buildFinalBitmap() {
        Bitmap result = Bitmap.createBitmap(mOriginalBitmap.getWidth(), mOriginalBitmap.getHeight(), mOriginalBitmap.getConfig());
        mEditorPaint.setColorFilter(binding.imageView.getColorFilter());

        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(mOriginalBitmap, 0, 0, mEditorPaint);
        binding.textContainer.draw(canvas);

        return result;
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
                mImageViewModel = imageViewModel;

                if (imageViewModel.getImageBitmap() == null) {
                    mGlide.asBitmap()
                            .load(imageViewModel.getImageUrl())
                            .dontTransform()
                            //.transition(BitmapTransitionOptions.withCrossFade())
                            .override(Target.SIZE_ORIGINAL)
                            .listener(new RequestListener<Bitmap>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                    mOriginalBitmap = null;
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                    LiveData<ImageViewModel> data = mImagesViewModel.getEditorImageData();
                                    if (data != null) {
                                        ImageViewModel imageData = data.getValue();
                                        if (imageData != null) {
                                            mOriginalBitmap = resource;
                                            imageData.setImageBitmap(resource);
                                        }
                                    }
                                    return false;
                                }
                            })
                            .into(mImageViewTarget);
                } else {
                    mOriginalBitmap = imageViewModel.getImageBitmap();
                    binding.imageView.setImageDrawable(new BitmapDrawable(binding.imageView.getResources(), imageViewModel.getImageBitmap()));
                    updateMarkupContainerMatrix();
                }
            }
        });
    }

    private void performBitmapUpload() {
        final Context ctx = requireContext().getApplicationContext();

        final UploadDialogFragment f = new UploadDialogFragment();

        ListenableFuture<Void> task = AsyncTask.run(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                //fileUploadTest(ctx);

                OkHttpClient client = HttpUtils.getHttpClient(ctx);
                Request getUploadUrlRequest = new Request.Builder()
                        .get()
                        .url("https://eulerity-hackathon.appspot.com/upload")
                        .build();

                try (Response getUploadUrlResponse = client.newCall(getUploadUrlRequest).execute()) {
                    if (getUploadUrlResponse.isSuccessful()) {
                        InputStream stream = getUploadUrlResponse.body().byteStream();
                        UploadResponse urlResponse = JSONParser.deserializer(stream, UploadResponse.class);
                        final String uploadURL = urlResponse.getUrl();

                        // Upload image
                        Request uploadRequest = new Request.Builder()
                                .post(
                                        new MultipartBody.Builder()
                                                .addFormDataPart(
                                                        "appid",
                                                        ctx.getPackageName()
                                                )
                                                .addFormDataPart(
                                                        "original",
                                                        mImageViewModel.getImageUrl()
                                                )
                                                .addFormDataPart(
                                                        "file",
                                                        "upload.jpg",
                                                        RequestBody.create(cacheToFile(ctx), MediaType.parse("image/jpeg"))
                                                )
                                                .build()
                                )
                                .url(uploadURL)
                                .build();

                        Response uploadResponse = client.newCall(uploadRequest).execute();
                        if (!uploadResponse.isSuccessful()) {
                            throw new IllegalStateException(uploadResponse.message());
                        }
                    } else {
                        throw new IllegalStateException(getUploadUrlResponse.message());
                    }
                }

                return null;
            }
        }, new AsyncTask.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                f.dismiss();

                Toast.makeText(ctx, R.string.message_uploadsuccess, Toast.LENGTH_SHORT).show();
                // Exit editor
                getParentFragmentManager().popBackStack();
            }

            @Override
            public void onFailure(@NonNull Throwable error) {
                f.dismiss();

                Log.d("ImageEditor", "error saving image", error);
                if (error instanceof CancellationException || error instanceof InterruptedException) {
                    Snackbar.make(binding.getRoot(), R.string.message_uploadcancelled, Snackbar.LENGTH_SHORT).show();
                } else {
                    Snackbar.make(binding.getRoot(), R.string.message_uploaderror, Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        f.setOnDialogCancelledListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                task.cancel(true);
            }
        });

        f.show(getParentFragmentManager(), null);
    }

    private File cacheToFile(@NonNull Context context) throws IOException {
        File rootDir = context.getCacheDir();

        File file = new File(rootDir, "upload.jpg");
        FileOutputStream fStream = new FileOutputStream(file);

        mEditedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fStream);
        fStream.flush();
        fStream.close();

        return file;
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
