package com.cleveroad.splittransformation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.GridLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.util.Random;

/**
 * Created by Александр on 21.03.2016.
 */
public class TransformationAdapterWrapper extends PagerAdapter implements ViewPager.PageTransformer {

    private static final String TAG = TransformationAdapterWrapper.class.getSimpleName();
    private static final int ROWS = 8;
    private static final int COLUMNS = 8;
    private static final int PIECES = ROWS * COLUMNS;
    private static final int OFFSET_PAGES = 3;

    private final LayoutInflater inflater;
    private final PagerAdapter innerAdapter;
    private final LruCache<String, Bitmap> imagesCache;
    private CustomEvaluator translationXEvaluator;
    private CustomEvaluator translationYEvaluator;

    public TransformationAdapterWrapper(@NonNull Context context, @NonNull PagerAdapter innerAdapter) {
        this.inflater = LayoutInflater.from(context);
        this.innerAdapter = innerAdapter;
        this.imagesCache = new LruCache<>(PIECES * OFFSET_PAGES);
        float tx = 300;
        float ty = 300;
        float spacing = 60;
        float[] randomSpacingsX = new float[PIECES];
        float[] randomSpacingsY = new float[PIECES];
        Random random = new Random();
        for (int i = 0; i < PIECES; i++) {
            randomSpacingsX[i] = spacing + random.nextFloat() * spacing / 4 * (random.nextBoolean() ? 1 : -1);
            randomSpacingsY[i] = spacing + random.nextFloat() * spacing / 4 * (random.nextBoolean() ? 1 : -1);
        }
        translationXEvaluator = new SimpleXEvaluator(-tx, tx, randomSpacingsX);
        translationYEvaluator = new SimpleYEvaluator(-ty, randomSpacingsY);
    }

    @Override
    public int getCount() {
        return innerAdapter.getCount();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.pager_item_with_grid_layout, container, false);
        view.setTag("" + position);
        container.addView(view);
//        ViewPager.LayoutParams params = (ViewPager.LayoutParams) view.getLayoutParams();
//        params.gravity = Gravity.CENTER;
//        view.setLayoutParams(params);
        FrameLayout cont = (FrameLayout) view.findViewById(R.id.item_container);
        GridLayout gridLayout = (GridLayout) view.findViewById(R.id.grid_layout);
        gridLayout.setRowCount(ROWS);
        gridLayout.setColumnCount(COLUMNS);
        innerAdapter.instantiateItem(cont, position);
        fillGridLayout(view, cont);
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        FrameLayout cont = (FrameLayout) view.findViewById(R.id.item_container);
        innerAdapter.destroyItem(cont, position, cont.getChildAt(0));
        container.removeView(view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    private void fillGridLayout(GridLayout gridLayout) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                ImageView imageView = (ImageView) inflater.inflate(R.layout.grid_layout_item, gridLayout, false);
                GridLayout.LayoutParams params = (GridLayout.LayoutParams) imageView.getLayoutParams();
                params.width = GridLayout.LayoutParams.WRAP_CONTENT;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1);
                params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1);
                params.setGravity(Gravity.CENTER);
                imageView.setLayoutParams(params);
                gridLayout.addView(imageView);
            }
        }
    }

    private void fillGridLayout(RelativeLayout gridLayout, View cont) {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int firstInRow = row * COLUMNS;
                int prevFirstInRow = (row - 1) * COLUMNS;
                int pos = col + firstInRow;
                ImageView imageView = (ImageView) inflater.inflate(R.layout.grid_layout_item, gridLayout, false);
                imageView.setId(pos);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
                params.width = GridLayout.LayoutParams.WRAP_CONTENT;
                params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                if (pos == 0) {
                    params.topMargin = cont.getTop();
                    params.leftMargin = cont.getLeft();
                }
                if (pos > COLUMNS - 1) {
                    params.addRule(RelativeLayout.BELOW, prevFirstInRow);
                }
                if (pos > 0 && pos != firstInRow) {
                    params.addRule(RelativeLayout.RIGHT_OF, pos - 1);
                }
                imageView.setLayoutParams(params);
                gridLayout.addView(imageView);
            }
        }
    }

    @Override
    public void transformPage(View page, float position) {
        FrameLayout cont = (FrameLayout) page.findViewById(R.id.item_container);
        RelativeLayout gridLayout = (RelativeLayout) page;
        View view = cont.getChildAt(0);
        float fract = position % 1;
        if (Math.abs(fract) > 0) {
            cont.setVisibility(View.INVISIBLE);
            gridLayout.setVisibility(View.VISIBLE);
        } else {
            cont.setVisibility(View.VISIBLE);
            gridLayout.setVisibility(View.INVISIBLE);
        }
        Bitmap[] images = new Bitmap[PIECES];
        boolean recreatePieces = false;
        for (int i = 0; i < images.length; i++) {
            images[i] = imagesCache.get(view.hashCode() + "_" + i);
            if (images[i] == null) {
                recreatePieces = true;
                break;
            }
        }
        if (recreatePieces) {
            recreatePieces(view, images);
            for (int i = 2; i < gridLayout.getChildCount(); i++) {
                ImageView iv = (ImageView) gridLayout.getChildAt(i);
                iv.setImageBitmap(images[i-2]);
            }
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                ImageView iv = (ImageView) gridLayout.getChildAt(col + row * COLUMNS + 2);
                float tX = translationXEvaluator.evaluate(Math.abs(fract), fract < 0, row, col);
                float tY = translationYEvaluator.evaluate(Math.abs(fract), fract < 0, row, col);
                iv.setTranslationX(tX);
                iv.setTranslationY(tY);
            }
        }
    }

    private void recreatePieces(View page, Bitmap[] images) {
        createPieces(page, images);
        for (int i = 0; i < images.length; i++) {
            if (images[i] == null)
                Log.w(TAG, "recreatePieces: image is null: " + page.hashCode() + "_" + i);
            imagesCache.put(page.hashCode() + "_" + i, images[i]);
        }
    }

    private void createPieces(View view, Bitmap[] images) {
        Log.d(TAG, "createPieces: " + view.hashCode());
        view.setDrawingCacheEnabled(true);
        view.buildDrawingCache();
        Bitmap bitmap = view.getDrawingCache();
        if (bitmap != null) {
            int stepW = bitmap.getWidth() / COLUMNS;
            int stepH = bitmap.getHeight() / ROWS;
            RectF rectF = new RectF();
            Paint paint = new Paint();
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    int w = stepW;
                    int h = stepH;
                    int x = stepW * col;
                    int y = stepH * row;
                    if (x + w > bitmap.getWidth()) {
                        w = bitmap.getWidth() - x;
                    }
                    if (y + h > bitmap.getHeight()) {
                        h = bitmap.getHeight() - y;
                    }
                    Bitmap piece = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(piece);
                    Bitmap part = Bitmap.createBitmap(bitmap, x, y, w, h);
                    if (part == null) {
                        Log.w(TAG, String.format("createPieces: bitmap is null: x, y, w, h = %d, %d, %d, %d", x, y, w, h));
                    } else {
                        canvas.drawBitmap(part, 0, 0, null);
                        rectF.set(0, 0, part.getWidth(), part.getHeight());
                        canvas.drawRect(rectF, paint);
                        part.recycle();
                    }
                    images[col + row * COLUMNS] = piece;
                }
            }
        }
        view.destroyDrawingCache();
    }

    private static class SimpleXEvaluator implements CustomEvaluator {

        private final float leftVal, rightVal;
        private final float[] randomSpacingX;

        public SimpleXEvaluator(float leftVal, float rightVal, float[] randomSpacingX) {
            this.leftVal = leftVal;
            this.rightVal = rightVal;
            this.randomSpacingX = randomSpacingX;
        }

        @Override
        public float evaluate(float fraction, boolean leftSide, int row, int col) {
            int pos = col + row * COLUMNS;
            if (leftSide) {
                return (leftVal - randomSpacingX[pos] * (COLUMNS - col)) * fraction;
            }
            return (rightVal + randomSpacingX[pos] * col) * fraction;
        }
    }

    private static class SimpleYEvaluator implements CustomEvaluator {

        private final float yValue;
        private final float[] randomSpacingY;

        public SimpleYEvaluator(float yValue, float[] randomSpacingY) {
            this.yValue = yValue;
            this.randomSpacingY = randomSpacingY;
        }

        @Override
        public float evaluate(float fraction, boolean leftSide, int row, int col) {
            return (yValue - randomSpacingY[col + row * COLUMNS] * (ROWS - row)) * fraction;
        }
    }

    public interface CustomEvaluator {
        float evaluate(float fraction, boolean leftSide, int row, int col);
    }
}
