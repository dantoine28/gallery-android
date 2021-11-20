package com.example.gallery_da;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class UploadDialogFragment extends DialogFragment {
    private DialogInterface.OnCancelListener onDialogCancelledListener = null;

    public void setOnDialogCancelledListener(@Nullable DialogInterface.OnCancelListener listener) {
        this.onDialogCancelledListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new MaterialAlertDialogBuilder(requireContext())
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setView(R.layout.layout_uploadview)
                .setCancelable(false)
                .create();
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        if (onDialogCancelledListener != null) {
            onDialogCancelledListener.onCancel(dialog);
        }
    }
}
