package com.example.gallery_da.viewmodels;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

import com.example.gallery_da.data.ImageResponseItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class ImageViewModel extends ViewModel {
    private static final String DATE_FORMAT = "MMM dd, yyyy h:mm:ss a";

    private final String imageUrl;
    private Bitmap imageBmp;

    private final LocalDateTime dateCreated;
    private final LocalDateTime dateUpdated;

    public ImageViewModel(@NonNull ImageResponseItem item) {
        this.imageUrl = item.getUrl();

        this.dateCreated = LocalDateTime.parse(item.getCreated(), DateTimeFormatter.ofPattern(DATE_FORMAT));
        this.dateUpdated = LocalDateTime.parse(item.getUpdated(), DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    public Bitmap getImageBitmap() {
        return imageBmp;
    }

    public void setImageBitmap(@Nullable Bitmap imageBmp) {
        this.imageBmp = imageBmp;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        imageBmp = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageViewModel that = (ImageViewModel) o;
        return imageUrl.equals(that.imageUrl) && dateCreated.equals(that.dateCreated) && dateUpdated.equals(that.dateUpdated);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imageUrl, dateCreated, dateUpdated);
    }
}
