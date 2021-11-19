package com.example.gallery_da.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

public class MainThreadExecutor implements Executor {
    public static Executor Instance = new MainThreadExecutor();

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(@NonNull Runnable runnable) {
        mMainHandler.post(runnable);
    }
}
