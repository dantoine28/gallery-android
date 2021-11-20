package com.example.gallery_da.data;

import com.google.gson.annotations.SerializedName;

public class UploadResponse {

    @SerializedName("url")
    private String url;

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}