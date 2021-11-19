package com.example.gallery_da.viewmodels;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class ImagesViewModel extends ViewModel {
    private final MutableLiveData<List<ImageViewModel>> imageData = new MutableLiveData<>();
    private final MutableLiveData<ImageViewModel> editorImageData = new MutableLiveData<>();

    public MutableLiveData<List<ImageViewModel>> getImageData() {
        return imageData;
    }

    public MutableLiveData<ImageViewModel> getEditorImageData() {
        return editorImageData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}
