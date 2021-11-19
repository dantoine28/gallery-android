package com.example.gallery_da;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.view.OneShotPreDrawListener;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.gallery_da.adapters.ImageListAdapter;
import com.example.gallery_da.data.ImageResponseItem;
import com.example.gallery_da.databinding.FragmentImagelistBinding;
import com.example.gallery_da.helpers.ListAdapterOnClickInterface;
import com.example.gallery_da.utils.AsyncTask;
import com.example.gallery_da.utils.HttpUtils;
import com.example.gallery_da.utils.JSONParser;
import com.example.gallery_da.viewmodels.ImageViewModel;
import com.example.gallery_da.viewmodels.ImagesViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.transition.Hold;
import com.google.android.material.transition.MaterialElevationScale;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import okhttp3.CacheControl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ImageListFragment extends Fragment {
    private static final String TAG = "ImageListFragment";

    private FragmentImagelistBinding binding;
    private GridLayoutManager mLayoutManager;
    private ImageListAdapter mAdapter;

    private ImagesViewModel mImagesViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setExitTransition(new Hold());
        setExitTransition(new MaterialElevationScale(false));
        setReenterTransition(new MaterialElevationScale(true));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentImagelistBinding.inflate(inflater, container, false);

        mLayoutManager = new GridLayoutManager(
                requireContext(),
                requireContext().getResources().getInteger(R.integer.min_grid_columncount),
                GridLayoutManager.VERTICAL,
                false
        );
        mAdapter = new ImageListAdapter();

        binding.recyclerView.setHasFixedSize(true);

        binding.recyclerView.setLayoutManager(mLayoutManager);
        binding.recyclerView.setAdapter(mAdapter);

        binding.refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshImages();
            }
        });

        binding.refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshImages();
            }
        });

        mAdapter.setOnClickListener(new ListAdapterOnClickInterface<ImageViewModel>() {
            @Override
            public void onClick(View view, ImageViewModel item) {
                mImagesViewModel.getEditorImageData().setValue(item);

                ViewCompat.setTransitionName(view, "shared_element_container");

                requireActivity().getSupportFragmentManager().beginTransaction()
                        .addSharedElement(view, "shared_element_container")
                        .replace(R.id.fragment_container, new ImageViewFragment(), "imageviewer")
                        .addToBackStack("imageviewer")
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
        // Adjust column size
        OneShotPreDrawListener.add(binding.recyclerView, new Runnable() {
            @Override
            public void run() {
                startPostponedEnterTransition();

                // Set grid span count
                final int parentWidth = binding.recyclerView.getMeasuredWidth() - binding.recyclerView.getPaddingStart() - binding.recyclerView.getPaddingEnd();
                final int minColumns = requireContext().getResources().getInteger(R.integer.min_grid_columncount);
                final int itemMinWidth = requireContext().getResources().getDimensionPixelSize(R.dimen.default_imagelist_size);
                int availColumns;
                if (parentWidth / itemMinWidth <= 1) {
                    availColumns = minColumns;
                } else {
                    availColumns = parentWidth / itemMinWidth;
                }

                mLayoutManager.setSpanCount(availColumns);
            }
        });

        mImagesViewModel = new ViewModelProvider(requireActivity()).get(ImagesViewModel.class);

        mImagesViewModel.getImageData().observe(getViewLifecycleOwner(), new Observer<List<ImageViewModel>>() {
            @Override
            public void onChanged(List<ImageViewModel> imageViewModels) {
                binding.refreshLayout.setRefreshing(false);
                binding.errorCardview.setVisibility(View.GONE);

                if (imageViewModels == null || imageViewModels.isEmpty()) {
                    binding.noImagesPrompt.setVisibility(View.VISIBLE);
                    mAdapter.submitList(Collections.emptyList());
                } else {
                    binding.noImagesPrompt.setVisibility(View.GONE);
                    mAdapter.submitList(imageViewModels);
                }
            }
        });

        refreshImages();
    }

    private void refreshImages() {
        binding.refreshLayout.setRefreshing(true);

        Context appContext = requireContext().getApplicationContext();
        AsyncTask.run(new Callable<List<ImageResponseItem>>() {
            @Override
            public List<ImageResponseItem> call() throws Exception {
                final OkHttpClient client = HttpUtils.getHttpClient(appContext);
                final String IMAGE_URL = "https://eulerity-hackathon.appspot.com/image";

                Request request = new Request.Builder()
                        .url(IMAGE_URL)
                        .cacheControl(
                                // NOTE: temporary
                                new CacheControl.Builder()
                                        .maxAge(1, TimeUnit.DAYS)
                                        .build()
                        )
                        .build();

                try (Response response = client.newCall(request).execute()) {

                    if (response.isSuccessful()) {
                        InputStream stream = response.body().byteStream();

                        TypeToken<List<ImageResponseItem>> token = new TypeToken<List<ImageResponseItem>>() {
                        };
                        List<ImageResponseItem> responseItems = JSONParser.deserializer(stream, token.getType());

                        return responseItems;
                    }
                }

                return Collections.emptyList();
            }
        }, new AsyncTask.Callback<List<ImageResponseItem>>() {
            @Override
            public void onSuccess(List<ImageResponseItem> result) {
                mImagesViewModel.getImageData().postValue(imageResponseMapper.apply(result));
            }

            @Override
            public void onFailure(@NonNull Throwable error) {
                Log.d(TAG, "refreshImages: error retrieving images", error);

                binding.refreshLayout.setRefreshing(false);

                if (mAdapter.getItemCount() > 0) {
                    Snackbar snackbar = Snackbar.make(binding.getRoot(), R.string.message_error_retrieveimages, Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.action_refresh, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            refreshImages();
                        }
                    });
                    snackbar.show();
                } else {
                    binding.errorCardview.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private final Function<List<ImageResponseItem>, List<ImageViewModel>> imageResponseMapper = new Function<List<ImageResponseItem>, List<ImageViewModel>>() {
        @Override
        public List<ImageViewModel> apply(List<ImageResponseItem> input) {
            if (input == null || input.isEmpty()) {
                return Collections.emptyList();
            }

            final List<ImageViewModel> models = new ArrayList<>(input.size());

            for (ImageResponseItem item : input) {
                models.add(new ImageViewModel(item));
            }

            return models;
        }
    };

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
