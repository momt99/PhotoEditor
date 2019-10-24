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
import android.util.DisplayMetrics;
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
    private RenderView renderView;
    private EntitiesContainerView entitiesView;
    private FrameLayout textDimView;
    private FrameLayout selectionContainerView;

    private EntityView currentEntityView;

    private boolean editingText;
    private Point2 editedTextPosition;
    private float editedTextRotation;
    private float editedTextScale;
    private String initialText;

    private SizeFX paintingSize;

    private boolean selectedStroke = true;


    private DispatchQueue queue;


    public PhotoPaintRenderView(Context context, Bitmap bitmap, int rotation) {
        super(context);

        AndroidUtilities.density = context.getResources().getDisplayMetrics().density;

        queue = new DispatchQueue("Paint");

        bitmapToEdit = bitmap;
        orientation = rotation;
        undoStore = new UndoStore();

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
                    canvas.rotate(v.getRotation());
                    canvas.translate(-entity.getWidth() * entity.getScaleX() / 2, -entity.getHeight() * entity.getScaleY() / 2);

                    if (v instanceof TextPaintView) {
                        Bitmap b = createBitmap(getResources().getDisplayMetrics(), (int) (v.getWidth() * v.getScaleX()), (int) (v.getHeight() * v.getScaleY()), Bitmap.Config.ARGB_8888);
                        Canvas c = new Canvas(b);
                        c.scale(v.getScaleX(), v.getScaleY());
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
                canvas.restore();
            }
        }
        return bitmap;
    }

    public static Bitmap createBitmap(DisplayMetrics metrics, int width, int height, Bitmap.Config config) {
        Bitmap bitmap;
        if (Build.VERSION.SDK_INT < 21) {
            bitmap = Bitmap.createBitmap(metrics, width, height, config);
        } else {
            bitmap = Bitmap.createBitmap(metrics, width, height, config);
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

        final TextPaintView textPaintView = (TextPaintView) currentEntityView;
        initialText = textPaintView.getText();
        editingText = true;

        editedTextPosition = textPaintView.getPosition();
        editedTextRotation = textPaintView.getRotation();
        editedTextScale = textPaintView.getScale();

        textPaintView.setPosition(centerPositionForEntity());
        textPaintView.setRotation(0.0f);
        textPaintView.setScale(1.0f);

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

    public void switchToDraw() {
        selectEntity(null);
    }

    public void switchToText() {
        createText();
    }

    private void showMenuForEntity(final EntityView entityView) {
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
