package com.cleveroad.splittransformation;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.support.annotation.NonNull;

/**
 * Helper interface for transforming bitmaps.
 */
public interface BitmapTransformer {

    /**
     * Setup bitmap transformer.
     * @param rows number of rows
     * @param cols number of columns
     * @param marginTop top margin
     * @param translationX translation on axis X
     * @param translationY translation on axis Y
     * @param piecesSpacing spacing between pieces
     */
    void setup(int rows, int cols, int marginTop, float translationX, float translationY, float piecesSpacing);

    /**
     * Set bitmap.
     * @param bitmap scaled bitmap
     * @param originalWidth original width
     * @param originalHeight original height
     */
    void setBitmap(Bitmap bitmap, int originalWidth, int originalHeight);


    /**
     * Called when transformer must apply a transformation to bitmap.
     * @param position Position of page relative to the current front-and-center position of the pager. 0 is front and center. 1 is one full page position to the right, and -1 is one page position to the left.
     */
    void onTransformPage(float position);

    /**
     * Called when transformer should draw the pieces.
     * @param canvas some canvas
     */
    void onDraw(@NonNull Canvas canvas);

    /**
     * Factory that produces new bitmap transformers.
     */
    interface Factory {

        /**
         * Provide new bitmap transformer.
         * @param position view's position in adapter
         * @return new bitmap transformer
         */
        BitmapTransformer newTransformer(int position);
    }
}
