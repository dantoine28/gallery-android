package com.example.gallery_da.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ImageResponse {

    @SerializedName("ImageResponse")
    private List<ImageResponseItem> imageResponse;

    public void setImageResponse(List<ImageResponseItem> imageResponse) {
        this.imageResponse = imageResponse;
    }

    public List<ImageResponseItem> getImageResponse() {
        return imageResponse;
    }
}