package com.cleveroad.splittransformation;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.CancellationSignal;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
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
    private PieceTransformer translationXEvaluator;
    private PieceTransformer translationYEvaluator;
    private ImageSplitter imageSplitter;
    private GridFiller gridFiller;
    private SplitterAsyncTask[] tasks;

    public TransformationAdapterWrapper(@NonNull Context context, @NonNull PagerAdapter innerAdapter) {
        this.inflater = LayoutInflater.from(context);
        this.innerAdapter = innerAdapter;
        this.imagesCache = new LruCache<>(PIECES * OFFSET_PAGES);
        float tx = 400;
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
        CellFillerAndSplitter cellFillerAndSplitter = new CellFillerAndSplitter(context, ROWS, COLUMNS);
        imageSplitter = cellFillerAndSplitter;
        gridFiller = cellFillerAndSplitter;
        tasks = new SplitterAsyncTask[2];
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
        FrameLayout cont = (FrameLayout) view.findViewById(R.id.item_container);
        RelativeLayout grid = (RelativeLayout) view.findViewById(R.id.grid);
        Object object = innerAdapter.instantiateItem(cont, position);
        View v = (View) object;
        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        v.measure(spec, spec);
        v.layout(0, 0, v.getMeasuredWidth(), v.getMeasuredHeight());
        gridFiller.populateGrid(grid, v);
        recreatePieces(v, grid);
        Log.d(TAG, "instantiateItem: " + position + ", w:h = " + v.getMeasuredWidth() + ":" + v.getMeasuredHeight());
        return view;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.grid);
        layout.removeAllViews();
        FrameLayout cont = (FrameLayout) view.findViewById(R.id.item_container);
        innerAdapter.destroyItem(cont, position, cont.getChildAt(0));
        container.removeView(view);
        Log.d(TAG, "destroyItem: " + position);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void transformPage(View page, float position) {
        FrameLayout cont = (FrameLayout) page.findViewById(R.id.item_container);
        RelativeLayout gridLayout = (RelativeLayout) page.findViewById(R.id.grid);
        View view = cont.getChildAt(0);
        float fract = position % 1;
        if (Math.abs(fract) > 0) {
            cont.setVisibility(View.INVISIBLE);
            gridLayout.setVisibility(View.VISIBLE);
        } else {
            cont.setVisibility(View.VISIBLE);
            gridLayout.setVisibility(View.INVISIBLE);
            Log.d(TAG, "transformPage: " + position);
        }
        Bitmap[] images = new Bitmap[PIECES];
        for (int i = 0; i < images.length; i++) {
            images[i] = imagesCache.get(view.hashCode() + "_" + i);
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                ImageView iv = (ImageView) gridLayout.getChildAt(col + row * COLUMNS);
//                if (row == 0 && col == 0) {
//                    RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) iv.getLayoutParams();
//                    params.leftMargin = 0;
//                    params.topMargin = 0;
//                    iv.setLayoutParams(params);
//                }
                float tX = translationXEvaluator.evaluate(Math.abs(fract), fract < 0, row, col);
                float tY = translationYEvaluator.evaluate(Math.abs(fract), fract < 0, row, col);
                iv.setTranslationX(tX);
                iv.setTranslationY(tY);
            }
        }
    }

    private void recreatePieces(final View page, final RelativeLayout gridLayout) {
        if (tasks[1] != null) {
            tasks[1].cancel();
        }
        SplitterAsyncTask task = new SplitterAsyncTask(page, gridLayout);
        task.execute();
        tasks[1] = tasks[0];
        tasks[0] = task;
    }

    private static class SimpleXEvaluator implements PieceTransformer {

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

    private static class SimpleYEvaluator implements PieceTransformer {

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

    public interface PieceTransformer {
        float evaluate(float fraction, boolean leftSide, int row, int col);
    }

    /**
     * Grid layout filler interface.
     */
    public interface GridFiller {

        /**
         * Populate grid with views for displaying pieces.
         * @param gridLayout instance of RelativeLayout
         * @param view view created by inner pager adapter
         */
        void populateGrid(@NonNull RelativeLayout gridLayout, View view);

        /**
         * Fill views with images.
         * @param gridLayout instance of populated RelativeLayout
         * @param pieces image splitted into pieces
         */
        void fillGrid(@NonNull RelativeLayout gridLayout, @Nullable Bitmap[] pieces);
    }

    /**
     * Image splitter interface.
     */
    public interface ImageSplitter {

        /**
         * Split image into pieces.
         * @param image some image
         * @param cancellationSignal cancellation signal object
         * @return pieces of image
         */
        @Nullable
        Bitmap[] split(@Nullable Bitmap image, @NonNull CancellationSignal cancellationSignal);
    }

    private class SplitterAsyncTask extends AsyncTask<Void, Void, Bitmap[]> {

        private final View page;
        private final RelativeLayout gridLayout;
        private CancellationSignal cancellationSignal;

        public SplitterAsyncTask(View page, RelativeLayout gridLayout) {
            this.page = page;
            this.gridLayout = gridLayout;
            this.cancellationSignal = new CancellationSignal();
        }

        public void cancel() {
            cancel(true);
            cancellationSignal.cancel();
        }

        @Override
        protected Bitmap[] doInBackground(Void... params) {
            Bitmap[] images = createPieces(page);
            if (images != null) {
                for (int i = 0; i < images.length; i++) {
                    if (images[i] == null)
                        Log.w(TAG, "recreatePieces: image is null: " + page.hashCode() + "_" + i);
                    else
                        imagesCache.put(page.hashCode() + "_" + i, images[i]);
                }
            }
            return images;
        }

        @Nullable
        private Bitmap[] createPieces(View view) {
            Log.d(TAG, "createPieces: " + view.hashCode());
            view.setDrawingCacheEnabled(true);
            view.buildDrawingCache();
            Bitmap bitmap = view.getDrawingCache();
            Bitmap[] images = imageSplitter.split(bitmap, cancellationSignal);
            view.destroyDrawingCache();
            return images;
        }

        @Override
        protected void onPostExecute(Bitmap[] images) {
            super.onPostExecute(images);
            gridFiller.fillGrid(gridLayout, images);
        }

    }
}
