package com.cleveroad.splittransformation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.LruCache;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
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
    private final Map<String, Float> positionsMap;

    private final int rows, columns;
    private final int marginTop;
    private final float translationX;
    private final float translationY;
    private final float piecesSpacing;
    private final float bitmapScale;
    private final ComplexViewDetector complexViewDetector;
    private final BitmapTransformer.Factory bitmapTransformerFactory;

    /**
     * Wrap existing page adapter and return a wrapper.
     *
     * @param context      instance of context
     * @param innerAdapter inner pager adapter
     * @return wrapped adapter
     */
    public static Builder wrap(@NonNull Context context, @NonNull PagerAdapter innerAdapter) {
        return new Builder(context, innerAdapter);
    }

    private TransformationAdapterWrapper(@NonNull Builder builder) {
        this.inflater = LayoutInflater.from(builder.context);
        this.innerAdapter = builder.innerAdapter;
        this.rows = builder.rows;
        this.columns = builder.columns;
        this.marginTop = builder.marginTop;
        this.imagesCache = new LruCache<>(OFFSET_PAGES);
        this.translationX = builder.translationX;
        this.translationY = builder.translationY;
        this.piecesSpacing = builder.piecesSpacing;
        this.complexViewDetector = builder.complexViewDetector;
        this.bitmapTransformerFactory = builder.bitmapTransformerFactory;
        this.bitmapScale = builder.bitmapScale;
        this.itemsMap = new HashMap<>();
        this.positionsMap = new HashMap<>();
    }

    @Override
    public int getCount() {
        return innerAdapter.getCount();
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        RelativeLayout view = (RelativeLayout) inflater.inflate(R.layout.trans_pager_item_with_grid_layout, container, false);
        ViewHolder viewHolder = new ViewHolder();
        viewHolder.transformerView = (TransformerView) view.findViewById(R.id.split_view);
        viewHolder.transformerView.setBitmapTransformer(bitmapTransformerFactory.newTransformer(position));
        viewHolder.transformerView.setup(rows, columns, marginTop, translationX, translationY, piecesSpacing);
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
            generateBitmap(v, viewHolder.transformerView);
        }
        itemsMap.put(view, viewHolder);
        return view;
    }

    private void generateBitmap(View view, TransformerView transformerView) {
        if (view.getWidth() == 0) {
            int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            view.measure(spec, spec);
            view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        }
        Bitmap image = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(image);
        view.draw(canvas);
        int w = image.getWidth();
        int h = image.getHeight();
        Bitmap scaledImage = Bitmap.createScaledBitmap(image, (int) (image.getWidth() * bitmapScale), (int) (image.getHeight() * bitmapScale), false);
        if (scaledImage != image) {
            image.recycle();
        }
        image = scaledImage;
        imagesCache.put(view.hashCode() + "", image);
        transformerView.setBitmap(image, w, h);
        transformerView.invalidate();
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
        positionsMap.remove(viewHolder.itemContainer.getChildAt(0).hashCode() + "");
        innerAdapter.destroyItem(viewHolder.itemContainer, position, viewHolder.innerObject);
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
            viewHolder.itemContainer.setVisibility(View.GONE);
            viewHolder.transformerView.setVisibility(View.VISIBLE);
            Float prevPosition = positionsMap.get(innerView.hashCode() + "");
            // regenerate image if necessary
            if (!viewHolder.transformerView.hasBitmap() || (prevPosition == null || prevPosition == 0) && complexViewDetector.isComplexView(innerView)) {
                generateBitmap(innerView, viewHolder.transformerView);
            }
            viewHolder.transformerView.onTransformPage(position);
        } else {
            viewHolder.itemContainer.setVisibility(View.VISIBLE);
            viewHolder.transformerView.setVisibility(View.GONE);
        }
        // update page's position
        positionsMap.put(innerView.hashCode() + "", position);
    }

    private static class ComplexViewDetectorImpl implements ComplexViewDetector {

        @Override
        public boolean isComplexView(@NonNull View view) {
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
    }

    private static class BitmapTransformerImpl implements BitmapTransformer {

        private final Random random = new Random();
        private final RectF canvasPiece = new RectF();
        private final Rect bitmapPiece = new Rect();
        private int rows, cols;
        private int marginTop;
        private float translationX, translationY;
        private float[] randomSpacingsX;
        private float[] randomSpacingsY;
        private float[] randomRotations;
        private Bitmap bitmap;
        private int originalWidth, originalHeight;
        private float position, absPosition;

        @Override
        public void setup(int rows, int cols, int marginTop, float translationX, float translationY, float piecesSpacing) {
            this.rows = rows;
            this.cols = cols;
            this.marginTop = marginTop;
            this.translationX = translationX;
            this.translationY = translationY;
            int pieces = rows * cols;
            randomSpacingsX = new float[pieces];
            randomSpacingsY = new float[pieces];
            randomRotations = new float[pieces];
            for (int i = 0; i < pieces; i++) {
                randomSpacingsX[i] = piecesSpacing + random.nextFloat() * piecesSpacing / 4 * (random.nextBoolean() ? 1 : -1);
                randomSpacingsY[i] = piecesSpacing + random.nextFloat() * piecesSpacing / 4 * (random.nextBoolean() ? 1 : -1);
                randomRotations[i] = 20 + random.nextFloat() * 70 * (random.nextBoolean() ? 1 : -1);
            }
        }

        @Override
        public void setBitmap(Bitmap bitmap, int originalWidth, int originalHeight) {
            this.bitmap = bitmap;
            this.originalWidth = originalWidth;
            this.originalHeight = originalHeight;
        }

        @Override
        public void onDraw(@NonNull Canvas canvas) {
            if (bitmap != null) {
                float left = (canvas.getWidth() - originalWidth) / 2;
                float top = marginTop;
                int wStep = originalWidth / cols;
                int hStep = originalHeight / rows;
                int wBmStep = bitmap.getWidth() / cols;
                int hBmStep = bitmap.getHeight() / rows;
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        int index = i * cols + j;
                        float tX;
                        if (position < 0) {
                            tX = (-translationX - randomSpacingsX[index] * (cols - j)) * absPosition;
                        } else {
                            tX = (translationX + randomSpacingsX[index] * j) * absPosition;
                        }
                        float tY = (translationY - randomSpacingsY[index] * (rows - i)) * absPosition;
                        bitmapPiece.set(wBmStep * j, hBmStep * i, wBmStep * (j + 1), hBmStep * (i + 1));
                        canvasPiece.set(
                                wStep * j + left + tX,
                                hStep * i + top + tY,
                                wStep * (j + 1) + left + tX,
                                hStep * (i + 1) + top + tY
                        );
                        // draw only visible pieces
                        if (canvasPiece.left < canvas.getWidth() || canvasPiece.right > 0 ||
                                canvasPiece.top < canvas.getHeight() || canvasPiece.bottom > 0) {
                            float angle = randomRotations[index] * absPosition;
                            canvas.save();
                            canvas.rotate(angle, canvasPiece.centerX(), canvasPiece.centerY());
                            canvas.drawBitmap(bitmap, bitmapPiece, canvasPiece, null);
                            canvas.restore();
                        }
                    }
                }
            }
        }

        @Override
        public void onTransformPage(float position) {
            this.position = position;
            this.absPosition = Math.abs(position);
        }

        private static class Factory implements BitmapTransformer.Factory {

            @Override
            public BitmapTransformer newTransformer(int position) {
                return new BitmapTransformerImpl();
            }
        }
    }


    /**
     * View holder class for single page item.
     */
    private static class ViewHolder {

        /**
         * Layout with splitted images.
         */
        private TransformerView transformerView;

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
        private static final float DEFAULT_BITMAP_SCALE = 1.0f;

        private final PagerAdapter innerAdapter;
        private final Context context;

        private int rows = DEFAULT_ROWS, columns = DEFAULT_COLUMNS;
        private int marginTop;
        private float translationX;
        private float translationY;
        private float piecesSpacing;
        private float bitmapScale;

        private ComplexViewDetector complexViewDetector;
        private BitmapTransformer.Factory bitmapTransformerFactory;

        private Builder(@NonNull Context context, @NonNull PagerAdapter pagerAdapter) {
            this.context = context;
            this.innerAdapter = pagerAdapter;
            this.marginTop = 0;
            this.piecesSpacing = context.getResources().getDimension(R.dimen.trans_pieces_spacing);
            this.translationX = context.getResources().getDimension(R.dimen.trans_translation_x);
            this.translationY = context.getResources().getDimension(R.dimen.trans_translation_y);
            this.bitmapScale = DEFAULT_BITMAP_SCALE;
        }

        /**
         * Set number of rows to split image. Default value: 8.
         *
         * @param rows number of rows to split image
         */
        public Builder rows(int rows) {
            this.rows = rows;
            return this;
        }

        /**
         * Set number of columns to split image. Default value: 8.
         *
         * @param columns number of columns to split image
         */
        public Builder columns(int columns) {
            this.columns = columns;
            return this;
        }

        /**
         * Set top margin. Default value: 24dp.
         *
         * @param marginTop top margin
         */
        public Builder marginTop(int marginTop) {
            this.marginTop = marginTop;
            return this;
        }

        /**
         * Set translationX for animation. Default value: 200dp.
         *
         * @param translationX translationX for animation
         */
        public Builder translationX(float translationX) {
            this.translationX = translationX;
            return this;
        }

        /**
         * Set translationY for animation. Default value: -150dp.
         *
         * @param translationY translationY for animation
         */
        public Builder translationY(float translationY) {
            this.translationY = translationY;
            return this;
        }

        /**
         * Set pieces spacing. Default value: 40dp.
         *
         * @param piecesSpacing pieces spacing
         */
        public Builder piecesSpacing(float piecesSpacing) {
            this.piecesSpacing = piecesSpacing;
            return this;
        }

        /**
         * Set complex view detector.
         *
         * @param complexViewDetector detector or null
         */
        public Builder complexViewDetector(@Nullable ComplexViewDetector complexViewDetector) {
            this.complexViewDetector = complexViewDetector;
            return this;
        }

        /**
         * Set factory that produces bitmap transformers.
         *
         * @param bitmapTransformerFactory factory or null
         */
        public Builder bitmapTransformerFactory(@Nullable BitmapTransformer.Factory bitmapTransformerFactory) {
            this.bitmapTransformerFactory = bitmapTransformerFactory;
            return this;
        }

        /**
         * Set bitmap scale coefficient. Default values is 1.0.
         *
         * @param bitmapScale scale coefficient in range {@code (0, 1]}
         */
        public Builder bitmapScale(float bitmapScale) {
            this.bitmapScale = bitmapScale;
            return this;
        }

        /**
         * Create new wrapper.
         *
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
            if (bitmapScale <= 0 || bitmapScale > 1) {
                throw new IllegalArgumentException("Bitmap scale coefficient must be in range (0, 1]");
            }
            if (complexViewDetector == null) {
                complexViewDetector = new ComplexViewDetectorImpl();
            }
            if (bitmapTransformerFactory == null) {
                bitmapTransformerFactory = new BitmapTransformerImpl.Factory();
            }
            return new TransformationAdapterWrapper(this);
        }
    }
}
