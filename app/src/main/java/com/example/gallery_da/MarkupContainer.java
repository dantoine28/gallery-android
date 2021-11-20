package com.example.gallery_da;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MarkupContainer extends FrameLayout {
    private Matrix mTransformationMatrix = new Matrix();

    public MarkupContainer(@NonNull Context context) {
        this(context, null);
    }

    public MarkupContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkupContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MarkupContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onViewAdded(View child) {
        super.onViewAdded(child);
        transformChild(child, mTransformationMatrix);
    }

    public void setTransformationMatrix(@NonNull Matrix matrix) {
        this.mTransformationMatrix = matrix;
        // Invalidate children
        for (int i = 0; i < this.getChildCount(); i++) {
            View v = getChildAt(i);
            transformChild(v, mTransformationMatrix);
        }
    }

    private static void transformChild(View child, Matrix matrix) {
        Matrix m = new Matrix();
        matrix.invert(matrix);

        float[] mValues = new float[9];
        m.getValues(mValues);

        float scaleX = mValues[Matrix.MSCALE_X];
        float scaleY = mValues[Matrix.MSCALE_Y];
        float translateX = mValues[Matrix.MTRANS_X];
        float translateY = mValues[Matrix.MTRANS_Y];

        child.setScaleX(scaleX);
        child.setScaleY(scaleY);
        child.setTranslationX(translateX / scaleX);
        child.setTranslationY(translateY / scaleY);
    }
}
