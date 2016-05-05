package com.cleveroad.splittransformation;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;

/**
 * Simple view that applies transformation to bitmap.
 */
public class TransformerView extends View {

    private Bitmap bitmap;
    private BitmapTransformer bitmapTransformer;

    public TransformerView(Context context) {
        this(context, null);
    }

    public TransformerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TransformerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TransformerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setBitmapTransformer(@NonNull BitmapTransformer bitmapTransformer) {
        this.bitmapTransformer = bitmapTransformer;
    }

    public void setup(int rows, int cols, int marginTop, float translationX, float translationY, float piecesSpacing) {
        if (bitmapTransformer != null) {
            bitmapTransformer.setup(rows, cols, marginTop, translationX, translationY, piecesSpacing);
        }
    }

    public void setBitmap(Bitmap bitmap, int width, int height) {
        this.bitmap = bitmap;
        if (bitmapTransformer != null) {
            bitmapTransformer.setBitmap(bitmap, width, height);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bitmapTransformer != null) {
            bitmapTransformer.onDraw(canvas);
        }
    }

    public void onTransformPage(float position) {
        if (bitmapTransformer != null) {
            bitmapTransformer.onTransformPage(position);
        }
        invalidate();
    }

    public boolean hasBitmap() {
        return bitmap != null;
    }
}
