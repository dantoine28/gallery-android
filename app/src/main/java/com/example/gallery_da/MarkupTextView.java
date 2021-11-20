package com.example.gallery_da;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.gallery_da.databinding.DialogEdittextBinding;
import com.example.gallery_da.databinding.LayoutMarkuptextviewBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class MarkupTextView extends FrameLayout {
    private LayoutMarkuptextviewBinding binding;

    public MarkupTextView(@NonNull Context context) {
        this(context, null);
    }

    public MarkupTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkupTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public MarkupTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initialize(context);
    }

    private void initialize(@NonNull Context context) {
        binding = LayoutMarkuptextviewBinding.inflate(LayoutInflater.from(context), this);

        binding.cardViewOutline.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditTextDialog();
            }
        });
    }

    private void showEditTextDialog() {
        DialogEdittextBinding dialogBinding = DialogEdittextBinding.inflate(LayoutInflater.from(getContext()));

        Dialog d = new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.title_edittext)
                .setView(dialogBinding.getRoot())
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(R.string.action_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ViewParent parent = getParent();
                        if (parent instanceof ViewGroup) {
                            final ViewGroup parentLayout = (ViewGroup) parent;
                            parentLayout.removeView(MarkupTextView.this);
                        }
                    }
                })
                .show();

        dialogBinding.editText.setText(binding.textView.getText());

        dialogBinding.editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.textView.setText(s.toString());
            }
        });

        d.show();
    }
}
