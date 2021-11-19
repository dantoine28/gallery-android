package com.example.gallery_da.utils;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.File;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class HttpUtils {
    private static OkHttpClient httpClient;

    public static OkHttpClient getHttpClient(@NonNull Context context) {
        if (httpClient == null) {
            final Context appContext = context.getApplicationContext();

            httpClient = new OkHttpClient.Builder()
                    .followRedirects(true)
                    .retryOnConnectionFailure(true)
                    .addNetworkInterceptor(new CacheInterceptor())
                    .cache(new Cache(new File(appContext.getCacheDir(), "okhttp3"), 30L * 1024 * 1024))
                    .build();
        }

        return httpClient;
    }
}
