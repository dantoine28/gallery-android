package com.example.gallery_da.adapters;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.gallery_da.databinding.LayoutImagelistitemBinding;
import com.example.gallery_da.helpers.ListAdapterOnClickInterface;
import com.example.gallery_da.viewmodels.ImageViewModel;

import java.util.Objects;

public class ImageListAdapter extends ListAdapter<ImageViewModel, ImageListAdapter.ImageItemViewHolder> {
    private RequestManager mGlide = null;

    private ListAdapterOnClickInterface<ImageViewModel> mOnClickListener = null;

    protected ImageListAdapter(@NonNull DiffUtil.ItemCallback<ImageViewModel> diffCallback) {
        super(diffCallback);
    }

    public ImageListAdapter() {
        super(new DiffUtil.ItemCallback<ImageViewModel>() {
            @Override
            public boolean areItemsTheSame(@NonNull ImageViewModel oldItem, @NonNull ImageViewModel newItem) {
                return Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ImageViewModel oldItem, @NonNull ImageViewModel newItem) {
                return Objects.equals(oldItem, newItem);
            }
        });
    }

    public void setOnClickListener(@Nullable ListAdapterOnClickInterface<ImageViewModel> listener) {
        this.mOnClickListener = listener;
    }

    protected class ImageItemViewHolder extends RecyclerView.ViewHolder {
        private final LayoutImagelistitemBinding binding;

        public ImageItemViewHolder(@NonNull LayoutImagelistitemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ImageViewModel model) {
            // Disable interactions
            enableInteractions(false);

            binding.progressBar.show();

            mGlide.clear(binding.imageView);

            mGlide.asDrawable()
                    .load(model.getImageUrl())
                    .thumbnail(0.5f)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            binding.progressBar.hide();
                            enableInteractions(true);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            binding.progressBar.hide();
                            enableInteractions(true);
                            return false;
                        }
                    })
                    .into(binding.imageView);
        }

        private void enableInteractions(boolean enable) {
            binding.getRoot().setEnabled(enable);
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (mGlide == null) {
            mGlide = Glide.with(recyclerView);
        }
    }

    @NonNull
    @Override
    public ImageItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mGlide == null) {
            mGlide = Glide.with(parent);
        }

        final RecyclerView recyclerView = (RecyclerView) parent;
        final GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

        LayoutImagelistitemBinding binding = LayoutImagelistitemBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );

        // Set item size
        final int parentWidth = parent.getMeasuredWidth() - parent.getPaddingStart() - parent.getPaddingEnd();
        final int availColumns = layoutManager.getSpanCount();
        int itemSize = parentWidth / availColumns;
        binding.getRoot().setLayoutParams(new RecyclerView.LayoutParams(itemSize, itemSize));

        return new ImageItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageItemViewHolder holder, int position) {
        final ImageViewModel model = getItem(position);

        holder.bind(model);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(v, model);
                }
            }
        });
    }
}
