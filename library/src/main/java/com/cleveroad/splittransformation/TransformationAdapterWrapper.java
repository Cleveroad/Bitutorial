package com.cleveroad.splittransformation;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.os.CancellationSignal;
import android.support.v4.util.LruCache;
import android.support.v4.util.SparseArrayCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * PagerAdapter wrapper with explode transformation functionality.
 */
public class TransformationAdapterWrapper extends PagerAdapter implements ViewPager.PageTransformer {

    private static final int OFFSET_PAGES = 3;

    private static final int ITEM_CONTAINER_START_ID = 1000;

    private final LayoutInflater inflater;
    private final PagerAdapter innerAdapter;
    private final LruCache<String, Bitmap> imagesCache;
    private final Map<Object, ViewHolder> itemsMap;
    private final Map<View, Float> positionsMap;
    private final SparseArrayCompat<SplitterAsyncTask> tasks;
    private final int screenWidth;
    
    private final int rows, columns;
    private final int pieces;
    private final int marginTop;
    private final float[] randomSpacingsX;
    private final float[] randomSpacingsY;
    private final float translationX;
    private final float translationY;

    /**
     * Wrap existing page adapter and return wrapper.
     * @param context instance of context
     * @param innerAdapter inner pager adapter
     * @return wrapped adapter
     */
    public static Builder wrap(@NonNull Context context, @NonNull PagerAdapter innerAdapter) {
        return new Builder(context, innerAdapter);
    }

    private TransformationAdapterWrapper(@NonNull Context context, @NonNull PagerAdapter innerAdapter, 
                                         int rows, int columns, int marginTop,
                                         float translationX, float translationY, float piecesSpacing) {
        this.inflater = LayoutInflater.from(context);
        this.innerAdapter = innerAdapter;
        this.rows = rows;
        this.columns = columns;
        this.pieces = rows * columns;
        this.marginTop = marginTop;
        this.imagesCache = new LruCache<>(pieces * OFFSET_PAGES);
        this.randomSpacingsX = new float[pieces];
        this.randomSpacingsY = new float[pieces];
        this.translationX = translationX;
        this.translationY = translationY;
        Random random = new Random();
        for (int i = 0; i < pieces; i++) {
            randomSpacingsX[i] = piecesSpacing + random.nextFloat() * piecesSpacing / 4 * (random.nextBoolean() ? 1 : -1);
            randomSpacingsY[i] = piecesSpacing + random.nextFloat() * piecesSpacing / 4 * (random.nextBoolean() ? 1 : -1);
        }
        screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        tasks = new SparseArrayCompat<>(OFFSET_PAGES);
        itemsMap = new HashMap<>();
        positionsMap = new HashMap<>();
    }

    @Override
    public int getCount() {
        return innerAdapter.getCount();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.pager_item_with_grid_layout, container, false);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.gridLayout = (RelativeLayout) view.findViewById(R.id.grid);
        viewHolder.itemContainer = (FrameLayout) view.findViewById(R.id.item_container);
        viewHolder.itemContainer.setId(ITEM_CONTAINER_START_ID + position);
        viewHolder.itemContainer.setPadding(0, marginTop, 0, 0);
        container.addView(view);
        Object object = innerAdapter.instantiateItem(viewHolder.itemContainer, position);
        viewHolder.innerObject = object;
        View v = null;
        if (object instanceof View) {
            v = (View) object;
        } else if (object instanceof Fragment) {
            v = ((Fragment) object).getView();
        }
        if (v != null) {
            populateGrid(viewHolder.gridLayout, v);
        }
        itemsMap.put(view, viewHolder);
        return view;
    }

    @Override
    public void finishUpdate(ViewGroup container) {
        super.finishUpdate(container);
        innerAdapter.finishUpdate(container);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View view = (View) object;
        ViewHolder viewHolder = itemsMap.get(object);
        viewHolder.gridLayout.removeAllViews();
        innerAdapter.destroyItem(viewHolder.itemContainer, position, viewHolder.innerObject);
        positionsMap.remove(viewHolder.itemContainer.getChildAt(0));
        itemsMap.remove(object);
        container.removeView(view);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void transformPage(View page, float position) {
        // page is off the screen
        if (position < -1 || position > 1) {
            page.setAlpha(0f);
            return;
        }
        page.setAlpha(1f);
        ViewHolder viewHolder = itemsMap.get(page);
        if (viewHolder == null || viewHolder.itemContainer.getChildCount() == 0)
            return;
        View innerView = viewHolder.itemContainer.getChildAt(0);
        float absPosition = Math.abs(position);
        if (absPosition > 0) {
            // recreate images if necessary
            recreatePieces(innerView, viewHolder.gridLayout);
            viewHolder.itemContainer.setVisibility(View.INVISIBLE);
            viewHolder.gridLayout.setVisibility(View.VISIBLE);
        } else {
            // cancel running task
            cancelRecreationTask(innerView);
            viewHolder.itemContainer.setVisibility(View.VISIBLE);
            viewHolder.gridLayout.setVisibility(View.INVISIBLE);
        }
        // update page's position
        positionsMap.put(innerView, position);
        // get images from cache
        Bitmap[] images = new Bitmap[pieces];
        for (int i = 0; i < images.length; i++) {
            images[i] = imagesCache.get(innerView.hashCode() + "_" + i);
        }
        // populate grid (for fragments)
        if (viewHolder.gridLayout.getChildCount() == 0) {
            populateGrid(viewHolder.gridLayout, innerView);
        }
        // update animation
        if (viewHolder.gridLayout.getChildCount() > 0) {
            for (int row = 0; row < rows; row++) {
                for (int col = 0; col < columns; col++) {
                    ImageView iv = (ImageView) viewHolder.gridLayout.getChildAt(col + row * columns);
                    float tX;
                    int pos = col + row * columns;
                    if (position < 0) {
                        tX = (-translationX - randomSpacingsX[pos] * (columns - col)) * absPosition;
                    } else {
                        tX  = (translationX + randomSpacingsX[pos] * col) * absPosition;
                    }
                    float tY = (translationY - randomSpacingsY[col + row * columns] * (rows - row)) * absPosition;
                    iv.setTranslationX(tX);
                    iv.setTranslationY(tY);
                }
            }
        }
    }

    /**
     * Check if pieces exist in cache and recreate them if necessary.
     * @param page view used for image creation
     * @param gridLayout grid layout
     */
    private void recreatePieces(final View page, final RelativeLayout gridLayout) {
        Bitmap[] images = new Bitmap[pieces];
        boolean recreate = false;
        for (int i = 0; i < pieces; i++) {
            images[i] = imagesCache.get(page.hashCode() + "_" + i);
            if (images[i] == null) {
                recreate = true;
                break;
            }
        }
        Float prevPosition = positionsMap.get(page);
        if (!recreate && (prevPosition == null || prevPosition == 0) && isComplexView(page)) {
            recreate = true;
        }
        if (recreate) {
            SplitterAsyncTask task = tasks.get(page.hashCode());
            if (task == null || task.isCompleted()) {
                task = new SplitterAsyncTask(page, gridLayout);
                tasks.put(page.hashCode(), task);
                task.execute();
            }
        }
    }

    /**
     * Check if view is a complex view.
     * @param view some view
     * @return true if view is complex, false otherwise
     */
    private boolean isComplexView(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                if (isComplexView(viewGroup.getChildAt(i)))
                    return true;
            }
        }
        if (view instanceof EditText || view instanceof CompoundButton || view instanceof ProgressBar || view instanceof AdapterView)
            return true;
        return false;
    }

    /**
     * Cancel recreation task.
     * @param page view associated with task
     */
    private void cancelRecreationTask(final View page) {
        SplitterAsyncTask task = tasks.get(page.hashCode());
        if (task != null && !task.isCompleted()) {
            task.cancel();
        }
        tasks.remove(page.hashCode());
    }

    /**
     * Add image views to grid layout.
     * @param gridLayout instance of grid layout
     * @param view inner view used for finding proper right margin
     */
    private void populateGrid(@NonNull RelativeLayout gridLayout, View view) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int firstInRow = row * columns;
                int prevFirstInRow = (row - 1) * columns;
                int pos = col + firstInRow;
                ImageView imageView = new ImageView(gridLayout.getContext());
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                if (pos == 0) {
                    int w;
                    if (view.getWidth() == 0) {
                        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                        view.measure(spec, spec);
                        w = view.getMeasuredWidth();
                    } else {
                        w = view.getWidth();
                    }
                    params.leftMargin = (screenWidth - w) / 2;
                    params.topMargin = marginTop;
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


    /**
     * AsyncTask that split image into pieces and fill grid layout.
     */
    private class SplitterAsyncTask extends AsyncTask<Void, Void, Bitmap[]> {

        private final View page;
        private final RelativeLayout gridLayout;
        private CancellationSignal cancellationSignal;
        private boolean completed;

        private SplitterAsyncTask(View page, RelativeLayout gridLayout) {
            this.page = page;
            this.gridLayout = gridLayout;
            this.cancellationSignal = new CancellationSignal();
        }

        /**
         * Cancel task.
         */
        private void cancel() {
            cancel(true);
            cancellationSignal.cancel();
        }

        @Override
        protected Bitmap[] doInBackground(Void... params) {
            Bitmap[] images = createPieces(page);
            if (images != null) {
                // put pieces into cache
                for (int i = 0; i < images.length; i++) {
                    if (images[i] != null) {
                        imagesCache.put(page.hashCode() + "_" + i, images[i]);
                    }
                }
            }
            return images;
        }

        @Nullable
        private Bitmap[] createPieces(View view) {
            view.setDrawingCacheEnabled(true);
            Bitmap bitmap = view.getDrawingCache();
            Bitmap[] images = split(bitmap, cancellationSignal);
            view.destroyDrawingCache();
            view.setDrawingCacheEnabled(false);
            return images;
        }

        @Override
        protected void onPostExecute(Bitmap[] images) {
            super.onPostExecute(images);
            completed = true;
            fillGrid(gridLayout, images);
            cancelRecreationTask(page);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            cancelRecreationTask(page);
        }

        /**
         * Check if task is completed.
         * @return true if task completed, false otherwise
         */
        private boolean isCompleted() {
            return completed;
        }

        /**
         * Set pieces to proper image view
         * @param gridLayout grid layout with image views
         * @param pieces splitted image
         */
        private void fillGrid(@NonNull RelativeLayout gridLayout, @Nullable Bitmap[] pieces) {
            if (pieces != null) {
                for (int i = 0; i < gridLayout.getChildCount(); i++) {
                    ImageView iv = (ImageView) gridLayout.getChildAt(i);
                    iv.setImageBitmap(pieces[i]);
                }
            }
        }

        /**
         * Split image into pieces.
         * @param image whole image
         * @param cancellationSignal cancellation signal for operation
         * @return splitted image or null if image was null or operation was cancelled
         */
        @Nullable
        private Bitmap[] split(@Nullable Bitmap image, @NonNull CancellationSignal cancellationSignal) {
            if (image == null) {
                return null;
            }
            Bitmap[] outImages = new Bitmap[rows * columns];
            int stepW = image.getWidth() / columns;
            int stepH = image.getHeight() / rows;
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
                    if (image.isRecycled()) {
                        return null;
                    }
                    Bitmap part = Bitmap.createBitmap(image, x, y, w, h);
                    outImages[col + row * columns] = part;
                }
            }
            return outImages;
        }
    }

    /**
     * View holder class for single page item.
     */
    private static class ViewHolder {
        
        /**
         * Layout with splitted images.
         */
        private RelativeLayout gridLayout;

        /**
         * Container for inner view.
         */
        private FrameLayout itemContainer;

        /**
         * Inner object created by {@link PagerAdapter#instantiateItem(ViewGroup, int)} method.
         */
        private Object innerObject;
    }
    
    public static class Builder {

        private static final int MIN_ROWS = 2;
        private static final int MIN_COLUMNS = 2;
        private static final int DEFAULT_ROWS = 8;
        private static final int DEFAULT_COLUMNS = 8;

        private final PagerAdapter innerAdapter;
        private final Context context;
        
        private int rows = DEFAULT_ROWS, columns = DEFAULT_COLUMNS;
        private int marginTop;
        private float translationX;
        private float translationY;
        private float piecesSpacing;
        
        private Builder(@NonNull Context context, @NonNull PagerAdapter pagerAdapter) {
            this.context = context;
            this.innerAdapter = pagerAdapter;
            this.marginTop = context.getResources().getDimensionPixelSize(R.dimen.marging_top);
            this.piecesSpacing = context.getResources().getDimension(R.dimen.pieces_spacing);
            this.translationX = context.getResources().getDimension(R.dimen.translation_x);
            this.translationY = context.getResources().getDimension(R.dimen.translation_y);
        }

        /**
         * Set number of rows to split image. Default value: 8.
         * @param rows number of rows to split image
         */
        public Builder rows(int rows) {
            this.rows = rows;
            return this;
        }

        /**
         * Set number of columns to split image. Default value: 8.
         * @param columns number of columns to split image
         */
        public Builder columns(int columns) {
            this.columns = columns;
            return this;
        }

        /**
         * Set top margin. Default value: 24dp.
         * @param marginTop top margin
         */
        public Builder marginTop(int marginTop) {
            this.marginTop = marginTop;
            return this;
        }

        /**
         * Set translationX for animation. Default value: 200dp.
         * @param translationX translationX for animation
         */
        public Builder translationX(float translationX) {
            this.translationX = translationX;
            return this;
        }

        /**
         * Set translationY for animation. Default value: -150dp.
         * @param translationY translationY for animation
         */
        public Builder translationY(float translationY) {
            this.translationY = translationY;
            return this;
        }

        /**
         * Set pieces spacing. Default value: 40dp.
         * @param piecesSpacing pieces spacing
         */
        public Builder piecesSpacing(float piecesSpacing) {
            this.piecesSpacing = piecesSpacing;
            return this;
        }

        /**
         * Create new wrapper.
         * @return new wrapper
         */
        public TransformationAdapterWrapper build() {
            if (rows < MIN_ROWS) {
                throw new IllegalArgumentException("Rows can't be lower than " + MIN_ROWS);
            }
            if (columns < MIN_COLUMNS) {
                throw new IllegalArgumentException("Columns can't be lower than " + MIN_COLUMNS);
            }
            if (piecesSpacing < 0) {
                throw new IllegalArgumentException("Pieces spacing can't be lower than 0");
            }
            return new TransformationAdapterWrapper(context, innerAdapter, rows, columns, marginTop, translationX, translationY, piecesSpacing);
        }
    }
}
