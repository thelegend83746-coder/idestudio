package com.idestudio.app.ui.editor;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

/**
 * Handles smooth two-finger pinch-to-zoom for the code editor and line numbers gutter.
 * Includes visual HUD badge and font size persistence.
 */
public class EditorPinchZoomController {

    private static final String PREF_NAME = "ide_editor_zoom_prefs";
    private static final String KEY_FONT_SIZE = "editor_font_size_sp";

    public static final float MIN_FONT_SIZE_SP = 10.0f;
    public static final float MAX_FONT_SIZE_SP = 36.0f;
    public static final float DEFAULT_FONT_SIZE_SP = 13.0f;

    private final Context context;
    private final EditText codeEditor;
    private final TextView lineNumbers;
    private final TextView zoomBadge;
    private final TextView statusZoom;
    private final ScrollView verticalScroll;
    private final HorizontalScrollView horizontalScroll;
    private final SharedPreferences prefs;

    private float currentFontSizeSp;
    private final ScaleGestureDetector scaleDetector;
    private final Handler hideBadgeHandler = new Handler(Looper.getMainLooper());
    private final Runnable hideBadgeRunnable;

    public EditorPinchZoomController(
            Context context,
            EditText codeEditor,
            TextView lineNumbers,
            TextView zoomBadge,
            TextView statusZoom,
            ScrollView verticalScroll,
            HorizontalScrollView horizontalScroll
    ) {
        this.context = context;
        this.codeEditor = codeEditor;
        this.lineNumbers = lineNumbers;
        this.zoomBadge = zoomBadge;
        this.statusZoom = statusZoom;
        this.verticalScroll = verticalScroll;
        this.horizontalScroll = horizontalScroll;

        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.currentFontSizeSp = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE_SP);

        this.hideBadgeRunnable = () -> {
            if (zoomBadge != null) {
                zoomBadge.setVisibility(View.GONE);
            }
        };

        this.scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                if (Float.isNaN(factor) || Float.isInfinite(factor) || factor <= 0) {
                    return false;
                }

                float newSize = currentFontSizeSp * factor;
                newSize = Math.max(MIN_FONT_SIZE_SP, Math.min(MAX_FONT_SIZE_SP, newSize));

                if (Math.abs(newSize - currentFontSizeSp) >= 0.1f) {
                    applyFontSize(newSize, true);
                }
                return true;
            }
        });

        // Initialize saved font size
        applyFontSize(currentFontSizeSp, false);
        attachTouchListeners();
    }

    private void attachTouchListeners() {
        View.OnTouchListener touchListener = (v, event) -> {
            int pointerCount = event.getPointerCount();

            // When two or more fingers touch, disallow scrollviews from stealing gesture
            if (pointerCount >= 2) {
                if (verticalScroll != null) verticalScroll.requestDisallowInterceptTouchEvent(true);
                if (horizontalScroll != null) horizontalScroll.requestDisallowInterceptTouchEvent(true);
                return scaleDetector.onTouchEvent(event);
            } else {
                if (event.getActionMasked() == MotionEvent.ACTION_UP ||
                    event.getActionMasked() == MotionEvent.ACTION_CANCEL) {
                    if (verticalScroll != null) verticalScroll.requestDisallowInterceptTouchEvent(false);
                    if (horizontalScroll != null) horizontalScroll.requestDisallowInterceptTouchEvent(false);
                }
            }
            return false;
        };

        codeEditor.setOnTouchListener(touchListener);
    }

    public void applyFontSize(float sizeSp, boolean showBadge) {
        currentFontSizeSp = sizeSp;

        // Apply to editor and line numbers gutter in perfect sync
        codeEditor.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSizeSp);
        if (lineNumbers != null) {
            lineNumbers.setTextSize(TypedValue.COMPLEX_UNIT_SP, currentFontSizeSp);
        }

        // Update status bar
        if (statusZoom != null) {
            statusZoom.setText(String.format(Locale.US, "%.0fsp", currentFontSizeSp));
        }

        // Show floating HUD badge
        if (showBadge && zoomBadge != null) {
            int percent = Math.round((currentFontSizeSp / DEFAULT_FONT_SIZE_SP) * 100);
            zoomBadge.setText(String.format(Locale.US, "%.1f sp (%d%%)", currentFontSizeSp, percent));
            zoomBadge.setVisibility(View.VISIBLE);

            hideBadgeHandler.removeCallbacks(hideBadgeRunnable);
            hideBadgeHandler.postDelayed(hideBadgeRunnable, 1500);
        }

        // Persist preference
        prefs.edit().putFloat(KEY_FONT_SIZE, currentFontSizeSp).apply();
    }

    public void zoomIn() {
        applyFontSize(Math.min(MAX_FONT_SIZE_SP, currentFontSizeSp + 2.0f), true);
    }

    public void zoomOut() {
        applyFontSize(Math.max(MIN_FONT_SIZE_SP, currentFontSizeSp - 2.0f), true);
    }

    public void resetZoom() {
        applyFontSize(DEFAULT_FONT_SIZE_SP, true);
    }

    public float getCurrentFontSizeSp() {
        return currentFontSizeSp;
    }
}
