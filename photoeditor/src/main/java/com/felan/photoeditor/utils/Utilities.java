package com.felan.photoeditor.utils;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatRadioButton;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.widget.CompoundButtonCompat;

import com.felan.photoeditor.R;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.nio.ByteBuffer;

public class Utilities {
    static {
        System.loadLibrary("image-lib");
    }

    public native static void calcCDT(ByteBuffer hsvBuffer, int width, int height, ByteBuffer buffer);

    public static LinearLayout makeRadioButtonsForLabels(Context context, String... labels) {
        return makeRadioButtonsForLabels(context, labels, null);
    }

    public static LinearLayout makeRadioButtonsForLabels(Context context, String[] labels, int[] colors) {
        LinearLayout parent = new LinearLayout(context);
        parent.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < labels.length; i++) {
            String label = labels[i];
            LinearLayout item = new LinearLayout(context);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            MaterialRadioButton radio = new MaterialRadioButton(context);
            if (colors != null)
                CompoundButtonCompat.setButtonTintList(radio, ColorStateList.valueOf(colors[i]));
            item.addView(radio, new LinearLayout.MarginLayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            TextView textView = new TextView(context);
            ColorStateList textColor;
            if (colors != null)
                textColor = ColorStateList.valueOf(colors[i]);
            else
                textColor = ResourcesCompat.getColorStateList(context.getResources(), R.color.blur_radio_text_color, context.getTheme());
            textView.setTextColor(textColor);
            textView.setText(label);
            ViewGroup.MarginLayoutParams lp = new LinearLayout.MarginLayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = context.getResources().getDimensionPixelSize(R.dimen.margin_small);
            item.addView(textView, lp);

            parent.addView(item, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        }

        return parent;
    }
}
