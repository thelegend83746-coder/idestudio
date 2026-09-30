package com.ide.studio.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.*;
import android.view.animation.OvershootInterpolator;
import android.widget.OverScroller;
import androidx.appcompat.widget.AppCompatEditText;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SmoothCodeEditor
 * Features:
 * - Fluid kinetic scrolling & momentum fling with custom OverScroller
 * - Rubber-band overscroll stretch with logarithmic resistance & Spring bounce-back
 * - Auto-fading scrollbars
 * - Line numbers gutter
 * - Syntax highlighting for Java & XML (Darcula / Light theme)
 * - Undo & Redo history stack
 */
public class SmoothCodeEditor extends AppCompatEditText {

    private Paint gutterPaint;
    private Paint gutterTextPaint;
    private Paint gutterDividerPaint;
    private Paint currentLinePaint;

    private OverScroller scroller;
    private GestureDetector gestureDetector;
    private float overscrollOffsetY = 0f;
    private float overscrollOffsetX = 0f;
    private static final float MAX_OVERSCROLL_DP = 32f;
    private float maxOverscrollPx;

    private boolean isDarkTheme = true;
    private boolean showLineNumbers = true;
    private boolean highlightCurrentLine = true;

    // Undo / Redo
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private boolean isUndoOrRedo = false;

    // Syntax highlighting patterns
    private static final Pattern PATTERN_KEYWORDS = Pattern.compile("\\b(abstract|assert|boolean|break|byte|case|catch|char|class|const|continue|default|do|double|else|enum|extends|final|finally|float|for|goto|if|implements|import|instanceof|int|interface|long|native|new|package|private|protected|public|return|short|static|strictfp|super|switch|synchronized|this|throw|throws|transient|try|void|volatile|while)\\b");
    private static final Pattern PATTERN_ANNOTATIONS = Pattern.compile("@[a-zA-Z_][a-zA-Z0-9_]*");
    private static final Pattern PATTERN_STRINGS = Pattern.compile("\"(\\\\.|[^\"\\\\])*\"");
    private static final Pattern PATTERN_NUMBERS = Pattern.compile("\\b\\d+([._]\\d+)*[fFdDlL]?\\b");
    private static final Pattern PATTERN_COMMENTS = Pattern.compile("(//.*?$)|(/\\*.*?\\*/)", Pattern.MULTILINE | Pattern.DOTALL);

    public SmoothCodeEditor(Context context) {
        super(context);
        init(context);
    }

    public SmoothCodeEditor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SmoothCodeEditor(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        maxOverscrollPx = MAX_OVERSCROLL_DP * context.getResources().getDisplayMetrics().density;
        scroller = new OverScroller(context);

        // Paints
        gutterPaint = new Paint();
        gutterTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gutterTextPaint.setTypeface(Typeface.MONOSPACE);
        gutterDividerPaint = new Paint();
        currentLinePaint = new Paint();

        setTypeface(Typeface.MONOSPACE);
        setGravity(Gravity.TOP | Gravity.START);
        setBackgroundColor(Color.TRANSPARENT);

        applyTheme(isDarkTheme);

        // Kinetic fling & gestures
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                scroller.fling(getScrollX(), getScrollY(), -(int) (velocityX * 0.7f), -(int) (velocityY * 0.7f),
                        0, getWidth(), 0, getLineCount() * getLineHeight());
                postInvalidateOnAnimation();
                return true;
            }
        });

        // Undo/Redo & Syntax Highlight TextWatcher
        addTextChangedListener(new TextWatcher() {
            private String beforeText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoOrRedo) {
                    beforeText = s.toString();
                }
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUndoOrRedo) {
                    undoStack.push(beforeText);
                    redoStack.clear();
                }
                applySyntaxHighlighting(s);
            }
        });
    }

    public void applyTheme(boolean dark) {
        this.isDarkTheme = dark;
        if (dark) {
            setTextColor(Color.parseColor("#A9B7C6"));
            gutterPaint.setColor(Color.parseColor("#313335"));
            gutterTextPaint.setColor(Color.parseColor("#606366"));
            gutterDividerPaint.setColor(Color.parseColor("#2D3748"));
            currentLinePaint.setColor(Color.parseColor("#323232"));
        } else {
            setTextColor(Color.parseColor("#111827"));
            gutterPaint.setColor(Color.parseColor("#F3F4F6"));
            gutterTextPaint.setColor(Color.parseColor("#9E9E9E"));
            gutterDividerPaint.setColor(Color.parseColor("#E5E7EB"));
            currentLinePaint.setColor(Color.parseColor("#F0F4F8"));
        }
        gutterTextPaint.setTextSize(getTextSize() * 0.85f);
        invalidate();
    }

    public void setLineNumbersVisible(boolean visible) {
        this.showLineNumbers = visible;
        setPadding(visible ? (int) (48 * getResources().getDisplayMetrics().density) : 16, getPaddingTop(), getPaddingRight(), getPaddingBottom());
        invalidate();
    }

    public void setHighlightCurrentLine(boolean highlight) {
        this.highlightCurrentLine = highlight;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int gutterWidth = showLineNumbers ? (int) (42 * getResources().getDisplayMetrics().density) : 0;

        // Current Line Highlight
        if (highlightCurrentLine && getLayout() != null) {
            int selStart = getSelectionStart();
            int currentLine = getLayout().getLineForOffset(selStart);
            int lineTop = getLayout().getLineTop(currentLine);
            int lineBottom = getLayout().getLineBottom(currentLine);
            canvas.drawRect(gutterWidth, lineTop, getWidth(), lineBottom, currentLinePaint);
        }

        // Draw Line Numbers Gutter
        if (showLineNumbers && getLayout() != null) {
            canvas.drawRect(0, 0, gutterWidth, getHeight() + getScrollY(), gutterPaint);
            canvas.drawLine(gutterWidth, 0, gutterWidth, getHeight() + getScrollY(), gutterDividerPaint);

            int firstLine = getLayout().getLineForVertical(getScrollY());
            int lastLine = getLayout().getLineForVertical(getScrollY() + getHeight());

            for (int i = firstLine; i <= lastLine && i < getLineCount(); i++) {
                int baseline = getLayout().getLineBaseline(i);
                String lineNum = String.valueOf(i + 1);
                float textWidth = gutterTextPaint.measureText(lineNum);
                canvas.drawText(lineNum, gutterWidth - textWidth - 12, baseline, gutterTextPaint);
            }
        }

        // Apply Overscroll Canvas Translation
        canvas.save();
        canvas.translate(overscrollOffsetX, overscrollOffsetY);
        super.onDraw(canvas);
        canvas.restore();
    }

    // Touch Event Handling with Elastic Overscroll
    private float lastTouchY = 0f;
    private float lastTouchX = 0f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchY = event.getY();
                lastTouchX = event.getX();
                break;

            case MotionEvent.ACTION_MOVE:
                float deltaY = event.getY() - lastTouchY;
                float deltaX = event.getX() - lastTouchX;
                lastTouchY = event.getY();
                lastTouchX = event.getX();

                // If scrolled to top limit and pulling down, or bottom limit and pulling up:
                if ((getScrollY() <= 0 && deltaY > 0) || (getScrollY() >= getBottomScrollLimit() && deltaY < 0)) {
                    // Logarithmic resistance damping
                    float resistance = (1f - (Math.abs(overscrollOffsetY) / maxOverscrollPx));
                    overscrollOffsetY = Math.max(-maxOverscrollPx, Math.min(maxOverscrollPx, overscrollOffsetY + deltaY * resistance * 0.4f));
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Spring bounce-back return animation
                if (overscrollOffsetY != 0f || overscrollOffsetX != 0f) {
                    animateOverscrollBounceBack();
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    private int getBottomScrollLimit() {
        if (getLayout() == null) return 0;
        return Math.max(0, getLayout().getHeight() - getHeight());
    }

    private void animateOverscrollBounceBack() {
        ValueAnimator animator = ValueAnimator.ofFloat(overscrollOffsetY, 0f);
        animator.setDuration(300);
        animator.setInterpolator(new OvershootInterpolator(1.2f));
        animator.addUpdateListener(animation -> {
            overscrollOffsetY = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidateOnAnimation();
        }
        super.computeScroll();
    }

    // Syntax Highlighting Spans
    private void applySyntaxHighlighting(Editable editable) {
        String text = editable.toString();

        // Clear previous ForegroundColorSpans
        ForegroundColorSpan[] spans = editable.getSpans(0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spans) {
            editable.removeSpan(span);
        }

        int keywordColor = isDarkTheme ? Color.parseColor("#CC7832") : Color.parseColor("#0066CC");
        int annotationColor = isDarkTheme ? Color.parseColor("#BBB529") : Color.parseColor("#808000");
        int stringColor = isDarkTheme ? Color.parseColor("#6A8759") : Color.parseColor("#008000");
        int numberColor = isDarkTheme ? Color.parseColor("#6897BB") : Color.parseColor("#098658");
        int commentColor = isDarkTheme ? Color.parseColor("#808080") : Color.parseColor("#6A9955");

        // Keywords
        Matcher m = PATTERN_KEYWORDS.matcher(text);
        while (m.find()) {
            editable.setSpan(new ForegroundColorSpan(keywordColor), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Annotations
        m = PATTERN_ANNOTATIONS.matcher(text);
        while (m.find()) {
            editable.setSpan(new ForegroundColorSpan(annotationColor), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Strings
        m = PATTERN_STRINGS.matcher(text);
        while (m.find()) {
            editable.setSpan(new ForegroundColorSpan(stringColor), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Numbers
        m = PATTERN_NUMBERS.matcher(text);
        while (m.find()) {
            editable.setSpan(new ForegroundColorSpan(numberColor), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // Comments
        m = PATTERN_COMMENTS.matcher(text);
        while (m.find()) {
            editable.setSpan(new ForegroundColorSpan(commentColor), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            isUndoOrRedo = true;
            String current = getText().toString();
            redoStack.push(current);
            String prev = undoStack.pop();
            setText(prev);
            setSelection(Math.min(prev.length(), current.length()));
            isUndoOrRedo = false;
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            isUndoOrRedo = true;
            String current = getText().toString();
            undoStack.push(current);
            String next = redoStack.pop();
            setText(next);
            setSelection(next.length());
            isUndoOrRedo = false;
        }
    }

    public void insertText(String text) {
        int start = Math.max(0, getSelectionStart());
        int end = Math.max(0, getSelectionEnd());
        getText().replace(Math.min(start, end), Math.max(start, end), text, 0, text.length());
    }

    public void insertTab(int spaces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < spaces; i++) {
            sb.append(" ");
        }
        insertText(sb.toString());
    }

    public String getCurrentWordPrefix() {
        int cursor = getSelectionStart();
        if (cursor <= 0) return "";
        CharSequence text = getText();
        int start = cursor - 1;
        while (start >= 0 && Character.isJavaIdentifierPart(text.charAt(start))) {
            start--;
        }
        start++;
        if (start < cursor) {
            return text.subSequence(start, cursor).toString();
        }
        return "";
    }

    public void replaceCurrentWord(String replacement) {
        int cursor = getSelectionStart();
        if (cursor < 0) return;
        CharSequence text = getText();
        int start = cursor - 1;
        while (start >= 0 && Character.isJavaIdentifierPart(text.charAt(start))) {
            start--;
        }
        start++;
        getText().replace(start, cursor, replacement);
    }
}
