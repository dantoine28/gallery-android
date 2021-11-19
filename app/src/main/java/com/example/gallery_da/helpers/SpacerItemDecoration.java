package com.example.gallery_da.helpers;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class SpacerItemDecoration extends RecyclerView.ItemDecoration {
    private Integer horizontalSpace;
    private Integer verticalSpace;

    public SpacerItemDecoration() {
        super();
    }

    public SpacerItemDecoration(int space) {
        this();
        horizontalSpace = space;
        verticalSpace = space;
    }

    public void setHorizontalSpace(int horizontalSpace) {
        this.horizontalSpace = horizontalSpace;
    }

    public void setVerticalSpace(int verticalSpace) {
        this.verticalSpace = verticalSpace;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);

        if (verticalSpace != null) {
            outRect.top = verticalSpace / 2;
            outRect.bottom = verticalSpace / 2;
        }

        if (horizontalSpace != null) {
            outRect.left = horizontalSpace / 2;
            outRect.right = horizontalSpace / 2;
        }
    }
}
