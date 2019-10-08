/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package com.felan.photoeditor.widgets.filters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import com.felan.photoeditor.utils.AndroidUtilities;
import com.felan.photoeditor.utils.RectX;

import java.util.Locale;

public class CurveView extends View {

    public interface PhotoFilterCurvesControlDelegate {
        void valueChanged();
    }

    private final static int CurvesSegmentNone = 0;
    private final static int CurvesSegmentBlacks = 1;
    private final static int CurvesSegmentShadows = 2;
    private final static int CurvesSegmentMidtones = 3;
    private final static int CurvesSegmentHighlights = 4;
    private final static int CurvesSegmentWhites = 5;

    private final static int GestureStateBegan = 1;
    private final static int GestureStateChanged = 2;
    private final static int GestureStateEnded = 3;
    private final static int GestureStateCancelled = 4;
    private final static int GestureStateFailed = 5;

    private int activeSegment = CurvesSegmentNone;

    private boolean isMoving;
    private boolean checkForMoving = true;

    private float lastX;
    private float lastY;

    private RectX actualArea = new RectX();

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintDash = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintCurve = new Paint(Paint.ANTI_ALIAS_FLAG);
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private Path path = new Path();

    private PhotoFilterCurvesControlDelegate delegate;

    private CurvesToolValue curveValue;

    public CurveView(Context context, CurvesToolValue value) {
        super(context);
        AndroidUtilities.density = context.getResources().getDisplayMetrics().density;
        setWillNotDraw(false);

        curveValue = value;

        paint.setColor(0x99ffffff);
        paint.setStrokeWidth(AndroidUtilities.dp(1));
        paint.setStyle(Paint.Style.STROKE);

        paintDash.setColor(0x99ffffff);
        paintDash.setStrokeWidth(AndroidUtilities.dp(2));
        paintDash.setStyle(Paint.Style.STROKE);

        paintCurve.setColor(0xffffffff);
        paintCurve.setStrokeWidth(AndroidUtilities.dp(2));
        paintCurve.setStyle(Paint.Style.STROKE);

        textPaint.setColor(0xffbfbfbf);
        textPaint.setTextSize(AndroidUtilities.dp(13));
    }

    public void setDelegate(PhotoFilterCurvesControlDelegate photoFilterCurvesControlDelegate) {
        delegate = photoFilterCurvesControlDelegate;
    }

    public void setActualArea(float x, float y, float width, float height) {
        actualArea.x = x;
        actualArea.y = y;
        actualArea.width = width;
        actualArea.height = height;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN: {
                if (event.getPointerCount() == 1) {
                    if (checkForMoving && !isMoving) {
                        float locationX = event.getX();
                        float locationY = event.getY();
                        lastX = locationX;
                        lastY = locationY;
                        if (locationX >= actualArea.x && locationX <= actualArea.x + actualArea.width && locationY >= actualArea.y && locationY <= actualArea.y + actualArea.height) {
                            isMoving = true;
                        }
                        checkForMoving = false;
                        if (isMoving) {
                            handlePan(GestureStateBegan, event);
                        }
                    }
                } else {
                    if (isMoving) {
                        handlePan(GestureStateEnded, event);
                        checkForMoving = true;
                        isMoving = false;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                if (isMoving) {
                    handlePan(GestureStateEnded, event);
                    isMoving = false;
                }
                checkForMoving = true;
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (isMoving) {
                    handlePan(GestureStateChanged, event);
                }
            }
        }
        return true;
    }

    private void handlePan(int state, MotionEvent event) {
        float locationX = event.getX();
        float locationY = event.getY();

        switch (state) {
            case GestureStateBegan: {
                selectSegmentWithPoint(locationX);
                break;
            }

            case GestureStateChanged: {
                float delta = Math.min(2, (lastY - locationY) / 8.0f);

                CurvesValue curveValue;
                switch (this.curveValue.getActiveType()) {
                    case LUMINANCE:
                        curveValue = this.curveValue.getLuminanceCurve();
                        break;

                    case RED:
                        curveValue = this.curveValue.getRedCurve();
                        break;

                    case GREEN:
                        curveValue = this.curveValue.getGreenCurve();
                        break;

                    case BLUE:
                        curveValue = this.curveValue.getBlueCurve();
                        break;

                    default:
                        return;
                }

                switch (activeSegment) {
                    case CurvesSegmentBlacks:
                        curveValue.setBlacksLevel(Math.max(0, Math.min(100, curveValue.getBlacksLevel() + delta)));
                        break;

                    case CurvesSegmentShadows:
                        curveValue.setShadowsLevel(Math.max(0, Math.min(100, curveValue.getShadowsLevel() + delta)));
                        break;

                    case CurvesSegmentMidtones:
                        curveValue.setMidtonesLevel(Math.max(0, Math.min(100, curveValue.getMidtonesLevel() + delta)));
                        break;

                    case CurvesSegmentHighlights:
                        curveValue.setHighlightsLevel(Math.max(0, Math.min(100, curveValue.getHighlightsLevel() + delta)));
                        break;

                    case CurvesSegmentWhites:
                        curveValue.setWhitesLevel(Math.max(0, Math.min(100, curveValue.getWhitesLevel() + delta)));
                        break;

                    default:
                        break;
                }

                invalidate();

                if (delegate != null) {
                    delegate.valueChanged();
                }

                lastX = locationX;
                lastY = locationY;
            }
            break;

            case GestureStateEnded:
            case GestureStateCancelled:
            case GestureStateFailed: {
                unselectSegments();
            }
            break;

            default:
                break;
        }
    }

    private void selectSegmentWithPoint(float pointx) {
        if (activeSegment != CurvesSegmentNone) {
            return;
        }
        float segmentWidth = actualArea.width / 5.0f;
        pointx -= actualArea.x;
        activeSegment = (int) Math.floor((pointx / segmentWidth) + 1);
    }

    private void unselectSegments() {
        if (activeSegment == CurvesSegmentNone) {
            return;
        }
        activeSegment = CurvesSegmentNone;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float segmentWidth = actualArea.width / 5.0f;

        for (int i = 0; i < 4; i++) {
            canvas.drawLine(actualArea.x + segmentWidth + i * segmentWidth, actualArea.y, actualArea.x + segmentWidth + i * segmentWidth, actualArea.y + actualArea.height, paint);
        }

        canvas.drawLine(actualArea.x, actualArea.y + actualArea.height, actualArea.x + actualArea.width, actualArea.y, paintDash);

        CurvesValue curvesValue;
        switch (curveValue.getActiveType()) {
            case LUMINANCE:
                paintCurve.setColor(0xffffffff);
                curvesValue = curveValue.getLuminanceCurve();
                break;

            case RED:
                paintCurve.setColor(0xffed3d4c);
                curvesValue = curveValue.getRedCurve();
                break;

            case GREEN:
                paintCurve.setColor(0xff10ee9d);
                curvesValue = curveValue.getGreenCurve();
                break;

            case BLUE:
                paintCurve.setColor(0xff3377fb);
                curvesValue = curveValue.getBlueCurve();
                break;

            default:
                return;
        }

        for (int a = 0; a < 5; a++) {
            String str;
            switch (a) {
                case 0:
                    str = String.format(Locale.US, "%.2f", curvesValue.getBlacksLevel() / 100.0f);
                    break;
                case 1:
                    str = String.format(Locale.US, "%.2f", curvesValue.getShadowsLevel() / 100.0f);
                    break;
                case 2:
                    str = String.format(Locale.US, "%.2f", curvesValue.getMidtonesLevel() / 100.0f);
                    break;
                case 3:
                    str = String.format(Locale.US, "%.2f", curvesValue.getHighlightsLevel() / 100.0f);
                    break;
                case 4:
                    str = String.format(Locale.US, "%.2f", curvesValue.getWhitesLevel() / 100.0f);
                    break;
                default:
                    str = "";
                    break;
            }
            float width = textPaint.measureText(str);
            canvas.drawText(str, actualArea.x + (segmentWidth - width) / 2 + segmentWidth * a, actualArea.y + actualArea.height - AndroidUtilities.dp(4), textPaint);
        }

        float[] points = curvesValue.interpolateCurve();
        invalidate();
        path.reset();
        for (int a = 0; a < points.length / 2; a++) {
            if (a == 0) {
                path.moveTo(actualArea.x + points[a * 2] * actualArea.width, actualArea.y + (1.0f - points[a * 2 + 1]) * actualArea.height);
            } else {
                path.lineTo(actualArea.x + points[a * 2] * actualArea.width, actualArea.y + (1.0f - points[a * 2 + 1]) * actualArea.height);
            }
        }

        canvas.drawPath(path, paintCurve);
    }

    public void setActiveType(CurvesToolValue.CurveType type) {
        curveValue.setActiveType(type);
        invalidate();
    }
}
