package com.example.gallery_da.utils;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AsyncTask {
    private static final ExecutorService sThreadPool = Executors.newCachedThreadPool();

    public static <T> ListenableFuture<T> run(@NonNull Callable<T> callable, @NonNull Callback<T> callback) {
        final ListenableFuture<T> task = Futures.submit(callable, sThreadPool);

        Futures.addCallback(task, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                callback.onSuccess(result);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                callback.onFailure(t);
            }
        }, MainThreadExecutor.Instance);

        return task;
    }

    public interface Callback<T> {
        void onSuccess(T result);

        void onFailure(@NonNull Throwable error);
    }
}
