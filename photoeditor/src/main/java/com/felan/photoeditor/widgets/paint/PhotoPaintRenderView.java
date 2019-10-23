package com.felan.photoeditor.widgets.paint;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.widget.PopupMenuCompat;

import com.felan.photoeditor.R;
import com.felan.photoeditor.utils.AndroidUtilities;
import com.felan.photoeditor.utils.ApplicationLoader;
import com.felan.photoeditor.utils.DispatchQueue;
import com.felan.photoeditor.utils.EventHandler;
import com.felan.photoeditor.utils.FileLog;
import com.felan.photoeditor.utils.SizeFX;

@SuppressLint("NewApi")
public class PhotoPaintRenderView extends FrameLayout implements EntityView.EntityViewDelegate {

    private Bitmap bitmapToEdit;
    private int orientation;
    private UndoStore undoStore;

    private int currentBrush;
    private Brush[] brushes = new Brush[]{
            new Brush.Radial(),
            new Brush.Elliptical(),
            new Brush.Neon()
    };

//    private FrameLayout toolsView;
//    private TextView cancelTextView;
//    private TextView doneTextView;

    //    private FrameLayout curtainView;
    private RenderView renderView;
    private EntitiesContainerView entitiesView;
    //    private FrameLayout dimView;
    private FrameLayout textDimView;
    private FrameLayout selectionContainerView;
//    private ColorPicker colorPicker;

//    private ImageView paintButton;

    private EntityView currentEntityView;

    private boolean editingText;
    private Point2 editedTextPosition;
    private float editedTextRotation;
    private float editedTextScale;
    private String initialText;

//    private Rect popupRect;

    private SizeFX paintingSize;

    private boolean selectedStroke = true;

//    private Animator colorPickerAnimator;

    private DispatchQueue queue;

//    private final static int gallery_menu_done = 1;

    public PhotoPaintRenderView(Context context, Bitmap bitmap, int rotation) {
        super(context);

        AndroidUtilities.density = context.getResources().getDisplayMetrics().density;

        queue = new DispatchQueue("Paint");

        bitmapToEdit = bitmap;
        orientation = rotation;
        undoStore = new UndoStore();
//        undoStore.setDelegate(() -> colorPicker.setUndoEnabled(undoStore.canUndo()));

//        curtainView = new FrameLayout(context);
//        curtainView.setBackgroundColor(0xff000000);
//        curtainView.setVisibility(INVISIBLE);
//        addView(curtainView);

        renderView = new RenderView(context, new Painting(getPaintingSize()), bitmap, orientation);
        renderView.setDelegate(new RenderView.RenderViewDelegate() {

            @Override
            public void onBeganDrawing() {
                if (currentEntityView != null) {
                    selectEntity(null);
                }
            }

            @Override
            public void onFinishedDrawing(boolean moved) {
//                colorPicker.setUndoEnabled(undoStore.canUndo());
            }

            @Override
            public boolean shouldDraw() {
                boolean draw = currentEntityView == null;
                if (!draw) {
                    selectEntity(null);
                }
                return draw;
            }
        });
        renderView.setUndoStore(undoStore);
        renderView.setQueue(queue);
        renderView.setVisibility(View.INVISIBLE);
        renderView.setBrush(brushes[0]);
        addView(renderView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        entitiesView = new EntitiesContainerView(context, new EntitiesContainerView.EntitiesContainerViewDelegate() {
            @Override
            public boolean shouldReceiveTouches() {
                return textDimView.getVisibility() != VISIBLE;
            }

            @Override
            public EntityView onSelectedEntityRequest() {
                return currentEntityView;
            }

            @Override
            public void onEntityDeselect() {
                selectEntity(null);
            }
        });
        entitiesView.setPivotX(0);
        entitiesView.setPivotY(0);
        addView(entitiesView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

//        dimView = new FrameLayout(context);
//        dimView.setAlpha(0);
//        dimView.setBackgroundColor(0x66000000);
//        dimView.setVisibility(GONE);
//        addView(dimView);

        textDimView = new FrameLayout(context);
        textDimView.setAlpha(0);
        textDimView.setBackgroundColor(0x66000000);
        textDimView.setVisibility(GONE);
        textDimView.setOnClickListener(v -> closeTextEnter(true));

        selectionContainerView = new FrameLayout(context) {
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return false;
            }
        };
        addView(selectionContainerView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
//        selectionContainerView.setBackgroundColor(Color.RED);

//        colorPicker = new ColorPicker(context);
//        addView(colorPicker);
//        colorPicker.setDelegate(new ColorPicker.ColorPickerDelegate() {
//            @Override
//            public void onBeganColorPicking() {
//                if (!(currentEntityView instanceof TextPaintView)) {
//                    setDimVisibility(true);
//                }
//            }
//
//            @Override
//            public void onColorValueChanged() {
//                setCurrentSwatch(colorPicker.getSwatch(), false);
//            }
//
//            @Override
//            public void onFinishedColorPicking() {
//                setCurrentSwatch(colorPicker.getSwatch(), false);
//
//                if (!(currentEntityView instanceof TextPaintView)) {
//                    setDimVisibility(false);
//                }
//            }
//
//            @Override
//            public void onSettingsPressed() {
//                if (currentEntityView != null) {
//                    if (currentEntityView instanceof StickerView) {
//                        mirrorSticker();
//                    } else if (currentEntityView instanceof TextPaintView) {
//                        showTextSettings();
//                    }
//                } else {
//                    showBrushSettings();
//                }
//            }

//            @Override
//            public void onUndoPressed() {
//                undoStore.undo();
//            }
//        });

//        toolsView = new FrameLayout(context);
//        toolsView.setBackgroundColor(0xff000000);
//        addView(toolsView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.LEFT | Gravity.BOTTOM));

//        cancelTextView = new TextView(context);
//        cancelTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
//        cancelTextView.setTextColor(0xffffffff);
//        cancelTextView.setGravity(Gravity.CENTER);
//        cancelTextView.setBackgroundDrawable(Theme.createSelectorDrawable(Theme.ACTION_BAR_PICKER_SELECTOR_COLOR, 0));
//        cancelTextView.setPadding(AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20), 0);
//        cancelTextView.setText(LocaleController.getString("Cancel", R.string.Cancel).toUpperCase());
//        cancelTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
//        toolsView.addView(cancelTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));

//        doneTextView = new TextView(context);


//        paintButton = new ImageView(context);
//        paintButton.setOnClickListener(v -> selectEntity(null));

//        ImageView textButton = new ImageView(context);
//        textButton.setOnClickListener(v -> createText());

//        colorPicker.setUndoEnabled(false);
//        setCurrentSwatch(colorPicker.getSwatch(), false);
        updateSettingsButton();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentEntityView != null) {
            if (editingText) {
                closeTextEnter(true);
            } else {
                selectEntity(null);
            }
        }
        return true;
    }

    private SizeFX getPaintingSize() {
        if (paintingSize != null) {
            return paintingSize;
        }
        float width = isSidewardOrientation() ? bitmapToEdit.getHeight() : bitmapToEdit.getWidth();
        float height = isSidewardOrientation() ? bitmapToEdit.getWidth() : bitmapToEdit.getHeight();

        SizeFX size = new SizeFX(width, height);
        size.width = 1280;
        size.height = (float) Math.floor(size.width * height / width);
        if (size.height > 1280) {
            size.height = 1280;
            size.width = (float) Math.floor(size.height * width / height);
        }
        paintingSize = size;
        return size;
    }

    private boolean isSidewardOrientation() {
        return orientation % 360 == 90 || orientation % 360 == 270;
    }


    public final EventHandler<PaintControlsView.PaintMode> paintModeChanged = new EventHandler<>();

    private void updateSettingsButton() {
//        int resource = R.drawable.photo_paint_brush;
//        if (currentEntityView != null) {
//           if (currentEntityView instanceof TextPaintView) {
//                resource = R.drawable.photo_outline;
//            }
//            paintButton.setImageResource(R.drawable.photo_paint);
//            paintButton.setColorFilter(null);
//        } else {
//            paintButton.setColorFilter(new PorterDuffColorFilter(0xff51bdf3, PorterDuff.Mode.MULTIPLY));
//            paintButton.setImageResource(R.drawable.photo_paint);
//        }
//
//        colorPicker.setSettingsButtonImage(resource);
        if (currentEntityView != null) {
            if (currentEntityView instanceof TextPaintView)
                paintModeChanged.invoke(PaintControlsView.PaintMode.TEXT);
        } else
            paintModeChanged.invoke(PaintControlsView.PaintMode.DRAW);

    }

    public void init() {
        renderView.setVisibility(View.VISIBLE);
    }

    public void shutdown() {
        renderView.shutdown();
        entitiesView.setVisibility(GONE);
        selectionContainerView.setVisibility(GONE);

        queue.postRunnable(() -> {
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quit();
            }
        });
    }

//    public FrameLayout getToolsView() {
//        return toolsView;
//    }

//    public TextView getDoneTextView() {
//        return doneTextView;
//    }

//    public TextView getCancelTextView() {
//        return cancelTextView;
//    }

//    public ColorPicker getColorPicker() {
//        return colorPicker;
//    }

    private boolean hasChanges() {
        return undoStore.canUndo() || entitiesView.entitiesCount() > 0;
    }

    public Bitmap getResultBitmap() {
//        Bitmap bitmap = renderView.getResultBitmap();
        Bitmap bitmap = renderView.getBitmap();
        if (bitmap != null && entitiesView.entitiesCount() > 0) {
            android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
//            canvas.save();
//            canvas.scale(paintingSize.width / entitiesView.getWidth(), paintingSize.height / entitiesView.getHeight());
            for (int i = 0; i < entitiesView.getChildCount(); i++) {
                View v = entitiesView.getChildAt(i);
                canvas.save();
                if (v instanceof EntityView) {
                    EntityView entity = (EntityView) v;

                    canvas.translate(entity.getPosition().x, entity.getPosition().y);
                    canvas.scale(v.getScaleX(), v.getScaleY());
                    canvas.rotate(v.getRotation());
                    canvas.translate(-entity.getWidth() / 2, -entity.getHeight() / 2);

                    if (v instanceof TextPaintView) {
                        Bitmap b = createBitmap(v.getWidth(), v.getHeight(), Bitmap.Config.ARGB_8888);
                        Canvas c = new Canvas(b);
                        v.draw(c);
                        canvas.drawBitmap(b, null, new Rect(0, 0, b.getWidth(), b.getHeight()), null);
                        try {
                            c.setBitmap(null);
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                        b.recycle();
                    } else {
                        v.draw(canvas);
                    }
                }
//                canvas.restore();
            }
        }
        return bitmap;
    }

    public static Bitmap createBitmap(int width, int height, Bitmap.Config config) {
        Bitmap bitmap;
        if (Build.VERSION.SDK_INT < 21) {
            bitmap = Bitmap.createBitmap(width, height, config);
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inDither = true;
//            options.inPreferredConfig = config;
//            options.inPurgeable = true;
//            options.inSampleSize = 1;
//            options.inMutable = true;
//            byte[] array = jpegData.get();
//            array[76] = (byte) (height >> 8);
//            array[77] = (byte) (height & 0x00ff);
//            array[78] = (byte) (width >> 8);
//            array[79] = (byte) (width & 0x00ff);
//            bitmap = BitmapFactory.decodeByteArray(array, 0, array.length, options);
//            Utilities.pinBitmap(bitmap);
//            bitmap.setHasAlpha(true);
//            bitmap.eraseColor(0);
        } else {
            bitmap = Bitmap.createBitmap(width, height, config);
        }
        if (config == Bitmap.Config.ARGB_8888 || config == Bitmap.Config.ARGB_4444) {
            bitmap.eraseColor(Color.TRANSPARENT);
        }
        return bitmap;
    }


    public void setCurrentBrushWeight(float weight) {
        renderView.setBrushSize(weight);
    }

    public void setCurrentColor(int color) {
        if (currentEntityView instanceof TextPaintView)
            ((TextPaintView) currentEntityView).setColor(color);
        else
            renderView.setColor(color);
    }

    public int getCurrentColor() {
        if (currentEntityView instanceof TextPaintView)
            return ((TextPaintView) currentEntityView).getColor();
        else
            return renderView.getCurrentColor();
    }


//    private void setCurrentSwatch(Swatch swatch, boolean updateInterface) {
//        renderView.setColor(swatch.color);
//        renderView.setBrushSize(swatch.brushWeight);
//
//        if (updateInterface) {
//            colorPicker.setSwatch(swatch);
//        }
//
//        if (currentEntityView instanceof TextPaintView) {
//            ((TextPaintView) currentEntityView).setColor(swatch.color);
//        }
//    }

    //    private void setDimVisibility(final boolean visible) {
//        Animator animator;
//        if (visible) {
//            dimView.setVisibility(VISIBLE);
//            animator = ObjectAnimator.ofFloat(dimView, "alpha", 0.0f, 1.0f);
//        } else {
//            animator = ObjectAnimator.ofFloat(dimView, "alpha", 1.0f, 0.0f);
//        }
//        animator.addListener(new AnimatorListenerAdapter() {
//            @Override
//            public void onAnimationEnd(Animator animation) {
//                if (!visible) {
//                    dimView.setVisibility(GONE);
//                }
//            }
//        });
//        animator.setDuration(200);
//        animator.start();
//    }
//
    private void setTextDimVisibility(final boolean visible, EntityView view) {
        Animator animator;

        if (visible && view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (textDimView.getParent() != null) {
                ((EntitiesContainerView) textDimView.getParent()).removeView(textDimView);
            }
            parent.addView(textDimView, parent.indexOfChild(view));
        }

        view.setSelectionVisibility(!visible);

        if (visible) {
            textDimView.setVisibility(VISIBLE);
            animator = ObjectAnimator.ofFloat(textDimView, "alpha", 0.0f, 1.0f);
        } else {
            animator = ObjectAnimator.ofFloat(textDimView, "alpha", 1.0f, 0.0f);
        }
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (!visible) {
                    textDimView.setVisibility(GONE);
                    if (textDimView.getParent() != null) {
                        ((EntitiesContainerView) textDimView.getParent()).removeView(textDimView);
                    }
                }
            }
        });
        animator.setDuration(200);
        animator.start();
    }

//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//
//        setMeasuredDimension(width, height);
//
//        float bitmapW;
//        float bitmapH;
//        int fullHeight = 700;
//        int maxHeight = fullHeight - AndroidUtilities.dp(48);
//        if (bitmapToEdit != null) {
//            bitmapW = isSidewardOrientation() ? bitmapToEdit.getHeight() : bitmapToEdit.getWidth();
//            bitmapH = isSidewardOrientation() ? bitmapToEdit.getWidth() : bitmapToEdit.getHeight();
//        } else {
//            bitmapW = width;
//            bitmapH = height - 100 - AndroidUtilities.dp(48);
//        }
//
//        float renderWidth = width;
//        float renderHeight = (float) Math.floor(renderWidth * bitmapH / bitmapW);
//        if (renderHeight > maxHeight) {
//            renderHeight = maxHeight;
//            renderWidth = (float) Math.floor(renderHeight * bitmapW / bitmapH);
//        }
//
//        renderView.measure(MeasureSpec.makeMeasureSpec((int) renderWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) renderHeight, MeasureSpec.EXACTLY));
//        entitiesView.measure(MeasureSpec.makeMeasureSpec((int) paintingSize.width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec((int) paintingSize.height, MeasureSpec.EXACTLY));
////        dimView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
//        selectionContainerView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY));
////        colorPicker.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.EXACTLY));
////        toolsView.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(48), MeasureSpec.EXACTLY));
//
//    }
//
//    @Override
//    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
//        int width = right - left;
//        int height = bottom - top;
//
//        int status = (Build.VERSION.SDK_INT >= 21 ? AndroidUtilities.statusBarHeight : 0);
//        int actionBarHeight = 100;
//        int actionBarHeight2 = 100 + status;
//
//        float bitmapW;
//        float bitmapH;
//        int maxHeight = 700;
//        if (bitmapToEdit != null) {
//            bitmapW = isSidewardOrientation() ? bitmapToEdit.getHeight() : bitmapToEdit.getWidth();
//            bitmapH = isSidewardOrientation() ? bitmapToEdit.getWidth() : bitmapToEdit.getHeight();
//        } else {
//            bitmapW = width;
//            bitmapH = height - actionBarHeight - AndroidUtilities.dp(48);
//        }
//
//        float renderWidth = width;
//        float renderHeight = (float) Math.floor(renderWidth * bitmapH / bitmapW);
//        if (renderHeight > maxHeight) {
//            renderHeight = maxHeight;
//            renderWidth = (float) Math.floor(renderHeight * bitmapW / bitmapH);
//        }
//
//        int x = (int) Math.ceil((width - renderView.getMeasuredWidth()) / 2);
//        int y = actionBarHeight2 + (height - actionBarHeight2 - AndroidUtilities.dp(48) - renderView.getMeasuredHeight()) / 2 - 100 + AndroidUtilities.dp(8);
//
//        renderView.layout(x, y, x + renderView.getMeasuredWidth(), y + renderView.getMeasuredHeight());
//
//        float scale = renderWidth / paintingSize.width;
//        entitiesView.setScaleX(scale);
//        entitiesView.setScaleY(scale);
//        entitiesView.layout(x, y, x + entitiesView.getMeasuredWidth(), y + entitiesView.getMeasuredHeight());
////        dimView.layout(0, status, dimView.getMeasuredWidth(), status + dimView.getMeasuredHeight());
//        selectionContainerView.layout(0, status, selectionContainerView.getMeasuredWidth(), status + selectionContainerView.getMeasuredHeight());
////        colorPicker.layout(0, actionBarHeight2, colorPicker.getMeasuredWidth(), actionBarHeight2 + colorPicker.getMeasuredHeight());
////        toolsView.layout(0, height - toolsView.getMeasuredHeight(), toolsView.getMeasuredWidth(), height);
////        curtainView.layout(0, 0, width, maxHeight);
//
//        if (currentEntityView != null) {
//            currentEntityView.updateSelectionView(); //TODO this is bug
//            currentEntityView.setOffset(entitiesView.getLeft() - selectionContainerView.getLeft(), entitiesView.getTop() - selectionContainerView.getTop());
//        }
//    }

    @Override
    public boolean onEntitySelected(EntityView entityView) {
        return selectEntity(entityView);
    }

    @Override
    public boolean onEntityLongClicked(EntityView entityView) {
        showMenuForEntity(entityView);
        return true;
    }

    @Override
    public boolean allowInteraction(EntityView entityView) {
        return !editingText;
    }

    private Point2 centerPositionForEntity() {
        return new Point2(selectionContainerView.getWidth() / 2.0f, selectionContainerView.getHeight() / 3.0f);
    }

    private Point2 startPositionRelativeToEntity(EntityView entityView) {
        final float offset = 200.0f;

        if (entityView != null) {
            Point2 position = entityView.getPosition();
            return new Point2(position.x + offset, position.y + offset);
        } else {
            final float minimalDistance = 100.0f;
            Point2 position = centerPositionForEntity();

            while (true) {
                boolean occupied = false;
                for (int index = 0; index < entitiesView.getChildCount(); index++) {
                    View view = entitiesView.getChildAt(index);
                    if (!(view instanceof EntityView))
                        continue;

                    Point2 location = ((EntityView) view).getPosition();
                    float distance = (float) Math.sqrt(Math.pow(location.x - position.x, 2) + Math.pow(location.y - position.y, 2));
                    if (distance < minimalDistance) {
                        occupied = true;
                    }
                }

                if (!occupied)
                    break;
                else
                    position = new Point2(position.x + offset, position.y + offset);
            }

            return position;
        }
    }

    private boolean selectEntity(EntityView entityView) {
        boolean changed = false;

        if (currentEntityView != null) {
            if (currentEntityView == entityView) {
                if (!editingText)
                    showMenuForEntity(currentEntityView);
                ;
                return true;
            } else {
                currentEntityView.deselect();
            }
            changed = true;
        }

        currentEntityView = entityView;

        if (currentEntityView != null) {
            currentEntityView.select(selectionContainerView);
            entitiesView.bringViewToFront(currentEntityView);

            if (currentEntityView instanceof TextPaintView) {
//                setCurrentSwatch(((TextPaintView) currentEntityView).getSwatch(), true);
//                setCurrentColor(((TextPaintView) currentEntityView).getColor());
            }

            changed = true;
        }

        updateSettingsButton();

        return changed;
    }

    private void removeEntity(EntityView entityView) {
        if (entityView == currentEntityView) {
            currentEntityView.deselect();
            if (editingText) {
                closeTextEnter(false);
            }
            currentEntityView = null;
            updateSettingsButton();
        }
        entitiesView.removeView(entityView);
        undoStore.unregisterUndo(entityView.getUUID());
    }

    private void duplicateSelectedEntity() {
        if (currentEntityView == null)
            return;

        EntityView entityView = null;
        Point2 position = startPositionRelativeToEntity(currentEntityView);

        if (currentEntityView instanceof TextPaintView) {
            TextPaintView newTextPaintView = new TextPaintView(getContext(), (TextPaintView) currentEntityView, position);
            newTextPaintView.setDelegate(this);
            newTextPaintView.setMaxWidth((int) (getPaintingSize().width - 20));
            entitiesView.addView(newTextPaintView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
            entityView = newTextPaintView;
        }

        registerRemovalUndo(entityView);
        selectEntity(entityView);

        updateSettingsButton();
    }

    private void registerRemovalUndo(final EntityView entityView) {
        undoStore.registerUndo(entityView.getUUID(), () -> removeEntity(entityView));
    }

    private int baseFontSize() {
        return (int) (getPaintingSize().width / 9);
    }

    private void createText() {
//        Swatch currentSwatch = colorPicker.getSwatch();
//        Swatch whiteSwatch = new Swatch(Color.WHITE, 1.0f, currentSwatch.brushWeight);
//        Swatch blackSwatch = new Swatch(Color.BLACK, 0.85f, currentSwatch.brushWeight);
//        setCurrentSwatch(selectedStroke ? blackSwatch : whiteSwatch, true);

        TextPaintView view = new TextPaintView(getContext(), startPositionRelativeToEntity(null), baseFontSize(), "", renderView.getCurrentColor(), selectedStroke);
        view.setDelegate(this);
        view.setMaxWidth((int) (getPaintingSize().width - 20));
        entitiesView.addView(view, new FrameLayout.LayoutParams(-2, -2));

        registerRemovalUndo(view);
        selectEntity(view);
        editSelectedTextEntity();
    }

    private void editSelectedTextEntity() {
        if (!(currentEntityView instanceof TextPaintView) || editingText) {
            return;
        }

//        curtainView.setVisibility(View.VISIBLE);

        final TextPaintView textPaintView = (TextPaintView) currentEntityView;
        initialText = textPaintView.getText();
        editingText = true;

        editedTextPosition = textPaintView.getPosition();
        editedTextRotation = textPaintView.getRotation();
        editedTextScale = textPaintView.getScale();

        textPaintView.setPosition(centerPositionForEntity());
        textPaintView.setRotation(0.0f);
        textPaintView.setScale(1.0f);

//        toolsView.setVisibility(GONE);

        setTextDimVisibility(true, textPaintView);
        textPaintView.beginEditing();

        InputMethodManager inputMethodManager = (InputMethodManager) ApplicationLoader.applicationContext.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.toggleSoftInputFromWindow(textPaintView.getFocusedView().getWindowToken(), InputMethodManager.SHOW_FORCED, 0);
    }

    public void closeTextEnter(boolean apply) {
        if (!editingText || !(currentEntityView instanceof TextPaintView)) {
            return;
        }

        TextPaintView textPaintView = (TextPaintView) currentEntityView;

//        toolsView.setVisibility(VISIBLE);

//        AndroidUtilities.hideKeyboard(textPaintView.getFocusedView());

        textPaintView.getFocusedView().clearFocus();
        textPaintView.endEditing();

        if (!apply) {
            textPaintView.setText(initialText);
        }

        if (textPaintView.getText().trim().length() == 0) {
            entitiesView.removeView(textPaintView);
            selectEntity(null);
        } else {
            textPaintView.setPosition(editedTextPosition);
            textPaintView.setRotation(editedTextRotation);
            textPaintView.setScale(editedTextScale);

            editedTextPosition = null;
            editedTextRotation = 0.0f;
            editedTextScale = 0.0f;
        }

        setTextDimVisibility(false, textPaintView);

        editingText = false;
        initialText = null;

//        curtainView.setVisibility(View.GONE);
    }

    public void setBrush(int brush) {
        renderView.setBrush(brushes[currentBrush = brush]);
    }

    public int getCurrentBrush() {
        return currentBrush;
    }

    public void setStroke(boolean stroke) {
        selectedStroke = stroke;
        if (currentEntityView instanceof TextPaintView) {
//            Swatch currentSwatch = colorPicker.getSwatch();
//            if (stroke && currentSwatch.color == Color.WHITE) {
//                Swatch blackSwatch = new Swatch(Color.BLACK, 0.85f, currentSwatch.brushWeight);
//                setCurrentSwatch(blackSwatch, true);
//            } else if (!stroke && currentSwatch.color == Color.BLACK) {
//                Swatch blackSwatch = new Swatch(Color.WHITE, 1.0f, currentSwatch.brushWeight);
//                setCurrentSwatch(blackSwatch, true);
//            }
            ((TextPaintView) currentEntityView).setStroke(stroke);
        }
    }

    public boolean getSelectedStroke() {
        if (currentEntityView instanceof TextPaintView)
            return ((TextPaintView) currentEntityView).getStroke();
        return selectedStroke;
    }

    public void undo() {
        undoStore.undo();
    }


//    private FrameLayout buttonForBrush(final int brush, int resource, boolean selected) {
//        FrameLayout button = new FrameLayout(getContext());
//        button.setBackgroundDrawable(Theme.getSelectorDrawable(false));
//        button.setOnClickListener(v -> {
//            setBrush(brush);
//
//            if (popupWindow != null && popupWindow.isShowing()) {
//                popupWindow.dismiss(true);
//            }
//        });
//
//        ImageView preview = new ImageView(getContext());
//        preview.setImageResource(resource);
//        button.addView(preview, LayoutHelper.createFrame(165, 44, Gravity.LEFT | Gravity.CENTER_VERTICAL, 46, 0, 8, 0));
//
//        if (selected) {
//            ImageView check = new ImageView(getContext());
//            check.setImageResource(R.drawable.ic_ab_done);
//            check.setScaleType(ImageView.ScaleType.CENTER);
//            check.setColorFilter(new PorterDuffColorFilter(0xff2f8cc9, PorterDuff.Mode.MULTIPLY));
//            button.addView(check, LayoutHelper.createFrame(50, LayoutHelper.MATCH_PARENT));
//        }
//
//        return button;
//    }

//    private void showBrushSettings() {
//        showPopup(() -> {
//            View radial = buttonForBrush(0, R.drawable.paint_radial_preview, currentBrush == 0);
//            popupLayout.addView(radial);
//
//            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) radial.getLayoutParams();
//            layoutParams.width = LayoutHelper.MATCH_PARENT;
//            layoutParams.height = AndroidUtilities.dp(52);
//            radial.setLayoutParams(layoutParams);
//
//            View elliptical = buttonForBrush(1, R.drawable.paint_elliptical_preview, currentBrush == 1);
//            popupLayout.addView(elliptical);
//
//            layoutParams = (LinearLayout.LayoutParams) elliptical.getLayoutParams();
//            layoutParams.width = LayoutHelper.MATCH_PARENT;
//            layoutParams.height = AndroidUtilities.dp(52);
//            elliptical.setLayoutParams(layoutParams);
//
//            View neon = buttonForBrush(2, R.drawable.paint_neon_preview, currentBrush == 2);
//            popupLayout.addView(neon);
//
//            layoutParams = (LinearLayout.LayoutParams) neon.getLayoutParams();
//            layoutParams.width = LayoutHelper.MATCH_PARENT;
//            layoutParams.height = AndroidUtilities.dp(52);
//            neon.setLayoutParams(layoutParams);
//        }, this, Gravity.RIGHT | Gravity.BOTTOM, 0, AndroidUtilities.dp(48));
//    }

//    private FrameLayout buttonForText(final boolean stroke, String text, boolean selected) {
//        FrameLayout button = new FrameLayout(getContext()) {
//            @Override
//            public boolean onInterceptTouchEvent(MotionEvent ev) {
//                return true;
//            }
//        };
//        button.setBackgroundDrawable(Theme.getSelectorDrawable(false));
//        button.setOnClickListener(v -> {
//            setStroke(stroke);
//
//            if (popupWindow != null && popupWindow.isShowing()) {
//                popupWindow.dismiss(true);
//            }
//        });
//
//        EditTextOutline textView = new EditTextOutline(getContext());
//        textView.setBackgroundColor(Color.TRANSPARENT);
//        textView.setEnabled(false);
//        textView.setStrokeWidth(AndroidUtilities.dp(3));
//        textView.setTextColor(stroke ? Color.WHITE : Color.BLACK);
//        textView.setStrokeColor(stroke ? Color.BLACK : Color.TRANSPARENT);
//        textView.setPadding(AndroidUtilities.dp(2), 0, AndroidUtilities.dp(2), 0);
//        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
//        textView.setTypeface(null, Typeface.BOLD);
//        textView.setTag(stroke);
//        textView.setText(text);
//        button.addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL, 46, 0, 16, 0));
//
//        if (selected) {
//            ImageView check = new ImageView(getContext());
//            check.setImageResource(R.drawable.ic_ab_done);
//            check.setScaleType(ImageView.ScaleType.CENTER);
//            check.setColorFilter(new PorterDuffColorFilter(0xff2f8cc9, PorterDuff.Mode.MULTIPLY));
//            button.addView(check, LayoutHelper.createFrame(50, LayoutHelper.MATCH_PARENT));
//        }
//
//        return button;
//    }

//    private void showTextSettings() {
//        showPopup(() -> {
//            View outline = buttonForText(true, LocaleController.getString("PaintOutlined", R.string.PaintOutlined), selectedStroke);
//            popupLayout.addView(outline);
//
//            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) outline.getLayoutParams();
//            layoutParams.width = LayoutHelper.MATCH_PARENT;
//            layoutParams.height = AndroidUtilities.dp(48);
//            outline.setLayoutParams(layoutParams);
//
//            View regular = buttonForText(false, LocaleController.getString("PaintRegular", R.string.PaintRegular), !selectedStroke);
//            popupLayout.addView(regular);
//
//            layoutParams = (LinearLayout.LayoutParams) regular.getLayoutParams();
//            layoutParams.width = LayoutHelper.MATCH_PARENT;
//            layoutParams.height = AndroidUtilities.dp(48);
//            regular.setLayoutParams(layoutParams);
//        }, this, Gravity.RIGHT | Gravity.BOTTOM, 0, AndroidUtilities.dp(48));
//    }

//    private void showPopup(Runnable setupRunnable, View parent, int gravity, int x, int y) {
//        if (popupWindow != null && popupWindow.isShowing()) {
//            popupWindow.dismiss();
//            return;
//        }
//
//        if (popupLayout == null) {
//            popupRect = new android.graphics.Rect();
//            popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getContext());
//            popupLayout.setAnimationEnabled(false);
//            popupLayout.setOnTouchListener((v, event) -> {
//                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
//                    if (popupWindow != null && popupWindow.isShowing()) {
//                        v.getHitRect(popupRect);
//                        if (!popupRect.contains((int) event.getX(), (int) event.getY())) {
//                            popupWindow.dismiss();
//                        }
//                    }
//                }
//                return false;
//            });
//            popupLayout.setDispatchKeyEventListener(keyEvent -> {
//                if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && popupWindow != null && popupWindow.isShowing()) {
//                    popupWindow.dismiss();
//                }
//            });
//            popupLayout.setShowedFromBotton(true);
//        }
//
//        popupLayout.removeInnerViews();
//        setupRunnable.run();
//
//        if (popupWindow == null) {
//            popupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
//            popupWindow.setAnimationEnabled(false);
//            popupWindow.setAnimationStyle(R.style.PopupAnimation);
//            popupWindow.setOutsideTouchable(true);
//            popupWindow.setClippingEnabled(true);
//            popupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
//            popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
//            popupWindow.getContentView().setFocusableInTouchMode(true);
//            popupWindow.setOnDismissListener(() -> popupLayout.removeInnerViews());
//        }
//
//        popupLayout.measure(MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), MeasureSpec.AT_MOST));
//
//        popupWindow.setFocusable(true);
//
//        popupWindow.showAtLocation(parent, gravity, x, y);
//        popupWindow.startAnimation();
//    }


    public void switchToDraw() {
        selectEntity(null);
    }

    public void switchToText() {
        createText();
    }

    private void showMenuForEntity(final EntityView entityView) {
//        int x = (int) ((entityView.getPosition().x - entitiesView.getWidth() / 2) * entitiesView.getScaleX());
//        int y = (int) ((entityView.getPosition().y - entityView.getHeight() * entityView.getScale() / 2 - entitiesView.getHeight() / 2) * entitiesView.getScaleY()) - AndroidUtilities.dp(32);

//        showPopup(() ->
//        {
//            LinearLayout parent = new LinearLayout(getContext());
//            parent.setOrientation(LinearLayout.HORIZONTAL);
//
//            TextView deleteView = new TextView(getContext());
//            deleteView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
//            deleteView.setBackgroundDrawable(Theme.getSelectorDrawable(false));
//            deleteView.setGravity(Gravity.CENTER_VERTICAL);
//            deleteView.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(14), 0);
//            deleteView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
//            deleteView.setTag(0);
//            deleteView.setText(LocaleController.getString("PaintDelete", R.string.PaintDelete));
//            deleteView.setOnClickListener(v -> {
//                removeEntity(entityView);
//
//                if (popupWindow != null && popupWindow.isShowing()) {
//                    popupWindow.dismiss(true);
//                }
//            });
//            parent.addView(deleteView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48));
//
//            if (entityView instanceof TextPaintView) {
//                TextView editView = new TextView(getContext());
//                editView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
//                editView.setBackgroundDrawable(Theme.getSelectorDrawable(false));
//                editView.setGravity(Gravity.CENTER_VERTICAL);
//                editView.setPadding(AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16), 0);
//                editView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
//                editView.setTag(1);
//                editView.setText(LocaleController.getString("PaintEdit", R.string.PaintEdit));
//                editView.setOnClickListener(v -> {
//                    editSelectedTextEntity();
//
//                    if (popupWindow != null && popupWindow.isShowing()) {
//                        popupWindow.dismiss(true);
//                    }
//                });
//                parent.addView(editView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48));
//            }
//
//            TextView duplicateView = new TextView(getContext());
//            duplicateView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
//            duplicateView.setBackgroundDrawable(Theme.getSelectorDrawable(false));
//            duplicateView.setGravity(Gravity.CENTER_VERTICAL);
//            duplicateView.setPadding(AndroidUtilities.dp(14), 0, AndroidUtilities.dp(16), 0);
//            duplicateView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
//            duplicateView.setTag(2);
//            duplicateView.setText(LocaleController.getString("PaintDuplicate", R.string.PaintDuplicate));
//            duplicateView.setOnClickListener(v -> {
//                duplicateSelectedEntity();
//
//                if (popupWindow != null && popupWindow.isShowing()) {
//                    popupWindow.dismiss(true);
//                }
//            });
//            parent.addView(duplicateView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 48));
//
//            popupLayout.addView(parent);
//
//            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) parent.getLayoutParams();
//            params.width = LayoutHelper.WRAP_CONTENT;
//            params.height = LayoutHelper.WRAP_CONTENT;
//            parent.setLayoutParams(params);
//        }, entityView, Gravity.CENTER, x, y);

        if (currentEntityView instanceof TextPaintView) {
            PopupMenu popup = new PopupMenu(getContext(), entityView, Gravity.BOTTOM);
            popup.getMenuInflater().inflate(R.menu.paint_text_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_delete)
                    removeEntity(entityView);
                else if (itemId == R.id.menu_edit)
                    editSelectedTextEntity();
                else if (itemId == R.id.menu_duplicate)
                    duplicateSelectedEntity();

                return true;
            });

            popup.show();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateSizes();
    }

    private void updateSizes() {
        float scaleX = (float) getWidth() / bitmapToEdit.getWidth();
        float scaleY = (float) getHeight() / bitmapToEdit.getHeight();

        int height, width;

        if (scaleX > scaleY) {
            height = getHeight();
            width = (int) (bitmapToEdit.getWidth() * scaleY);
        } else {
            width = getWidth();
            height = (int) (bitmapToEdit.getHeight() * scaleX);
        }

        post(() -> {
            LayoutParams lp = new FrameLayout.LayoutParams(width, height, Gravity.CENTER);
            renderView.setLayoutParams(lp);
            entitiesView.setLayoutParams(new LayoutParams(lp));
            selectionContainerView.setLayoutParams(new LayoutParams(lp));
        });
    }
}
