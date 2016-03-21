package com.cleveroad.splittransformation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.CancellationSignal;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * Created by Александр on 21.03.2016.
 */
final class CellFillerAndSplitter implements TransformationAdapterWrapper.GridFiller, TransformationAdapterWrapper.ImageSplitter {

    private static final String TAG = CellFillerAndSplitter.class.getSimpleName();

    private final int rows, columns;
    private final LayoutInflater inflater;
    private final int screenWidth;

    public CellFillerAndSplitter(@NonNull Context context, int rows, int columns) {
        this.inflater = LayoutInflater.from(context);
        this.rows = rows;
        this.columns = columns;
        this.screenWidth = context.getResources().getDisplayMetrics().widthPixels;
    }

    @Override
    public void populateGrid(@NonNull RelativeLayout gridLayout, View view) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int firstInRow = row * columns;
                int prevFirstInRow = (row - 1) * columns;
                int pos = col + firstInRow;
                ImageView imageView = (ImageView) inflater.inflate(R.layout.grid_layout_item, gridLayout, false);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
                params.width = GridLayout.LayoutParams.WRAP_CONTENT;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                if (pos == 0) {
                    params.leftMargin = (screenWidth - view.getMeasuredWidth()) / 2;
                    params.topMargin = gridLayout.getResources().getDimensionPixelSize(R.dimen.padding);
                }
                if (pos > 0 && pos <= columns - 1) {
                    params.addRule(RelativeLayout.ALIGN_TOP, 1);
                } else if (pos > columns - 1) {
                    params.addRule(RelativeLayout.BELOW, prevFirstInRow + 1);
                }
                if (pos > 0 && pos == firstInRow) {
                    params.addRule(RelativeLayout.ALIGN_LEFT, prevFirstInRow + 1);
                }
                if (pos > 0 && pos != firstInRow) {
                    params.addRule(RelativeLayout.RIGHT_OF, pos);
                }
                imageView.setId(pos + 1);
                imageView.setLayoutParams(params);
                gridLayout.addView(imageView);
            }
        }
    }

    @Override
    public void fillGrid(@NonNull RelativeLayout gridLayout, @Nullable Bitmap[] pieces) {
        if (pieces != null) {
            for (int i = 0; i < gridLayout.getChildCount(); i++) {
                ImageView iv = (ImageView) gridLayout.getChildAt(i);
                iv.setImageBitmap(pieces[i]);
            }
        }
    }

    @Nullable
    @Override
    public Bitmap[] split(@Nullable Bitmap image, @NonNull CancellationSignal cancellationSignal) {
        if (image == null) {
            return null;
        }
        Bitmap[] outImages = new Bitmap[rows * columns];
        int stepW = image.getWidth() / columns;
        int stepH = image.getHeight() / rows;
        RectF rectF = new RectF();
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                if (cancellationSignal.isCanceled())
                    return null;
                int w = stepW;
                int h = stepH;
                int x = stepW * col;
                int y = stepH * row;
                if (x + w > image.getWidth()) {
                    w = image.getWidth() - x;
                }
                if (y + h > image.getHeight()) {
                    h = image.getHeight() - y;
                }
                Bitmap piece = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(piece);
                if (image.isRecycled()) {
                    return null;
                }
                Bitmap part = Bitmap.createBitmap(image, x, y, w, h);
                if (part == null) {
                    Log.w(TAG, String.format("createPieces: bitmap is null: x, y, w, h = %d, %d, %d, %d", x, y, w, h));
                } else {
                    canvas.drawBitmap(part, 0, 0, null);
                    rectF.set(0, 0, part.getWidth(), part.getHeight());
//                        canvas.drawRect(rectF, paint);
                    part.recycle();
                }
                outImages[col + row * columns] = piece;
            }
        }
        return outImages;
    }
}
