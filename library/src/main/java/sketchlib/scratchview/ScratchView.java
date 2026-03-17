package sketchlib.scratchview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

public class ScratchView extends View {

    public enum RevealType { FADE, EXPAND_CIRCLE }
    public enum ScratchDirection { ANY, HORIZONTAL, VERTICAL }

    public interface Reward {
        String getType();
        Object getData();
    }

    public interface ScratchListener {
        void onScratchStart();
        void onScratchProgress(float percent);
        void onScratchEnd();
        void onRevealed(Reward reward);
        void onRevealThresholdReached(float percent); 
    }

    private ScratchListener listener = new ScratchListener() {
        @Override public void onScratchStart() {}
        @Override public void onScratchProgress(float percent) {}
        @Override public void onScratchEnd() {}
        @Override public void onRevealed(Reward reward) {}
        @Override public void onRevealThresholdReached(float percent) {}
    };

    private int foilStartColor = Color.parseColor("#0F1C36");
    private int foilEndColor = Color.parseColor("#051124");
    private int borderStartColor = Color.parseColor("#200E35");
    private int borderCenterColor = Color.parseColor("#381B5D");
    private int borderEndColor = Color.parseColor("#582C8E");

    private float cornerRadius, borderSize, brushSize, scratchTextSize;
    private String scratchText = "SCRATCH HERE";
    private int scratchTextColor = Color.parseColor("#80FFFFFF");

    private Paint foilPaint, borderPaint, scratchPathPaint, textPaint;
    private Paint bitmapPaint; 

    private Bitmap mScratchBitmap;
    private Canvas mScratchCanvas;
    
    private Bitmap overlayBitmap, scaledOverlayBitmap; 
    private Bitmap revealBitmap, scaledRevealBitmap;  

    private SparseArray<Path> mPaths = new SparseArray<>();
    private SparseArray<PointF> mLastPoints = new SparseArray<>();

    private boolean isRevealed = false;
    private boolean isCalculating = false;
    private boolean isScratchable = true;
    private boolean isScratching = false;

    private float thresholdPercent = 0.4f;
    private boolean autoRevealEnabled = true;
    private boolean hapticEnabled = true;
    private boolean performanceMode = false;
    private boolean animationsEnabled = true;
    private RevealType revealType = RevealType.FADE;
    private ScratchDirection scratchDirection = ScratchDirection.ANY;
    
    private Reward hiddenReward = new SimpleReward("Text", "Reward");

    private long lastHapticTime = 0;
    private long lastCheckTime = 0;

    public ScratchView(Context context) { super(context); init(context, null); }
    public ScratchView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(context, attrs); }
    public ScratchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) { super(context, attrs, defStyleAttr); init(context, attrs); }

    private void init(Context context, @Nullable AttributeSet attrs) {
        cornerRadius = dpToPx(14); borderSize = dpToPx(8);
        brushSize = dpToPx(40); scratchTextSize = dpToPx(22);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ScratchView, 0, 0);
            try {
                foilStartColor = a.getColor(R.styleable.ScratchView_sv_foilStartColor, foilStartColor);
                foilEndColor = a.getColor(R.styleable.ScratchView_sv_foilEndColor, foilEndColor);
                brushSize = a.getDimension(R.styleable.ScratchView_sv_brushSize, brushSize);
                thresholdPercent = a.getFloat(R.styleable.ScratchView_sv_thresholdPercent, thresholdPercent);
                hapticEnabled = a.getBoolean(R.styleable.ScratchView_sv_hapticEnabled, hapticEnabled);
            } finally {
                a.recycle();
            }
        }

        scratchPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scratchPathPaint.setDither(true);
        scratchPathPaint.setStyle(Paint.Style.STROKE);
        scratchPathPaint.setStrokeJoin(Paint.Join.ROUND);
        scratchPathPaint.setStrokeCap(Paint.Cap.ROUND);
        scratchPathPaint.setStrokeWidth(brushSize);
        scratchPathPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT)); 

        foilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        foilPaint.setStyle(Paint.Style.FILL);

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(borderSize);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(scratchTextColor);
        textPaint.setTextSize(scratchTextSize);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG); 
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateGradients(w, h);
        cacheScaledBitmaps(w, h);
        resetScratchBitmap(w, h);
    }

    private void cacheScaledBitmaps(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (overlayBitmap != null) scaledOverlayBitmap = Bitmap.createScaledBitmap(overlayBitmap, w, h, true);
        if (revealBitmap != null) scaledRevealBitmap = Bitmap.createScaledBitmap(revealBitmap, w, h, true);
    }

    private void updateGradients(int w, int h) {
        if (w <= 0 || h <= 0) return;
        foilPaint.setShader(new LinearGradient(0, 0, 0, h, new int[]{foilStartColor, foilEndColor}, null, Shader.TileMode.CLAMP));
        borderPaint.setShader(new LinearGradient(0, h, w, 0, new int[]{borderStartColor, borderCenterColor, borderEndColor}, new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
    }

    private void resetScratchBitmap(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (mScratchBitmap != null && !mScratchBitmap.isRecycled()) mScratchBitmap.recycle();
        
        mScratchBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mScratchCanvas = new Canvas(mScratchBitmap);

        RectF rect = new RectF(0, 0, w, h);
        
        if (scaledOverlayBitmap != null) {
            mScratchCanvas.drawBitmap(scaledOverlayBitmap, 0, 0, null);
        } else {
            mScratchCanvas.drawRoundRect(rect, cornerRadius, cornerRadius, foilPaint);
            Paint.FontMetrics fm = textPaint.getFontMetrics();
            float textY = (h / 2f) - ((fm.descent + fm.ascent) / 2);
            mScratchCanvas.drawText(scratchText, w / 2f, textY, textPaint);
        }

        isRevealed = false;
        bitmapPaint.setAlpha(255); 
        mPaths.clear();
        mLastPoints.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (scaledRevealBitmap != null) {
            canvas.drawBitmap(scaledRevealBitmap, 0, 0, null);
        }

        if (mScratchBitmap != null && !mScratchBitmap.isRecycled()) {
            canvas.drawBitmap(mScratchBitmap, 0, 0, bitmapPaint); 
        }

        float inset = borderSize / 2f;
        RectF borderRect = new RectF(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isScratchable || isRevealed) return true;

        int action = event.getActionMasked();
        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                if (!isScratching) {
                    isScratching = true;
                    listener.onScratchStart();
                }
                Path path = new Path();
                path.moveTo(x, y);
                mPaths.put(pointerId, path);
                mLastPoints.put(pointerId, new PointF(x, y));
                break;

            case MotionEvent.ACTION_MOVE:
                boolean needsInvalidate = false;
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    float px = event.getX(i);
                    float py = event.getY(i);
                    
                    Path p = mPaths.get(id);
                    PointF lastPoint = mLastPoints.get(id);
                    
                    if (p != null && lastPoint != null) {
                        float dx = Math.abs(px - lastPoint.x);
                        float dy = Math.abs(py - lastPoint.y);

                        if (scratchDirection == ScratchDirection.HORIZONTAL && dy > dx * 1.5) continue;
                        if (scratchDirection == ScratchDirection.VERTICAL && dx > dy * 1.5) continue;

                        if (dx >= 4 || dy >= 4) {
                            p.quadTo(lastPoint.x, lastPoint.y, (px + lastPoint.x) / 2, (py + lastPoint.y) / 2);
                            lastPoint.set(px, py);
                            mScratchCanvas.drawPath(p, scratchPathPaint);
                            needsInvalidate = true;
                        }
                    }
                }
                
                if (needsInvalidate) {
                    invalidate();
                    if (hapticEnabled && System.currentTimeMillis() - lastHapticTime > 50) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        lastHapticTime = System.currentTimeMillis();
                    }
                    triggerRevealCheck();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                mPaths.remove(pointerId);
                mLastPoints.remove(pointerId);
                
                if (mPaths.size() == 0) {
                    isScratching = false;
                    listener.onScratchEnd();
                    calculateRevealPercentSafe(); 
                }
                break;
        }
        return true;
    }

    private void triggerRevealCheck() {
        if (System.currentTimeMillis() - lastCheckTime > 150 && !isCalculating) {
            lastCheckTime = System.currentTimeMillis();
            calculateRevealPercentSafe();
        }
    }

    private void calculateRevealPercentSafe() {
        if (isCalculating || isRevealed || mScratchBitmap == null || mScratchBitmap.isRecycled()) return;
        isCalculating = true;

        int w = mScratchBitmap.getWidth();
        int h = mScratchBitmap.getHeight();
        
        int[] pixels = new int[w * h];
        mScratchBitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        new Thread(() -> {
            int revealed = 0;
            int totalPixels = w * h;
            int step = performanceMode ? 10 : 3; 

            for (int i = 0; i < totalPixels; i += step) {
                if ((pixels[i] >>> 24) == 0) revealed++;
            }
            
            final float percent = (float) revealed / (totalPixels / step);

            post(() -> {
                isCalculating = false;
                listener.onScratchProgress(percent);
                
                if (autoRevealEnabled && percent >= thresholdPercent && !isRevealed) {
                    listener.onRevealThresholdReached(percent); 
                    revealAutomatically();
                }
            });
        }).start();
    }

    public void revealAutomatically() {
        if (isRevealed) return;
        isRevealed = true;
        if (animationsEnabled) {
            animateReveal();
        } else {
            clearCanvas();
        }
    }

    private void animateReveal() {
        if (revealType == RevealType.FADE) {
            ValueAnimator anim = ValueAnimator.ofInt(255, 0);
            anim.setDuration(600);
            anim.addUpdateListener(animation -> {
                bitmapPaint.setAlpha((int) animation.getAnimatedValue());
                invalidate();
            });
            anim.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator animation) { clearCanvas(); }
            });
            anim.start();
        } else {
            clearCanvas();
        }
    }

    public void resetAnimated() {
        if (getWidth() <= 0) return;
        resetScratchBitmap(getWidth(), getHeight());
        bitmapPaint.setAlpha(0);
        
        ValueAnimator anim = ValueAnimator.ofInt(0, 255);
        anim.setDuration(400);
        anim.addUpdateListener(animation -> {
            bitmapPaint.setAlpha((int) animation.getAnimatedValue());
            invalidate();
        });
        anim.start();
    }

    private void clearCanvas() {
        if (mScratchCanvas != null) {
            mScratchCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            bitmapPaint.setAlpha(255);
            invalidate();
        }
        listener.onRevealed(hiddenReward);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("superState", super.onSaveInstanceState());
        bundle.putBoolean("isRevealed", isRevealed);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            isRevealed = bundle.getBoolean("isRevealed");
            state = bundle.getParcelable("superState");
            if (isRevealed) post(this::clearCanvas);
        }
        super.onRestoreInstanceState(state);
    }

    public void setThresholdPercent(float percent) { 
        this.thresholdPercent = percent; 
    }

    public void setAutoRevealEnabled(boolean enabled) {
        this.autoRevealEnabled = enabled;
    }

    public void setScratchOverlayBitmap(Bitmap bitmap) {
        this.overlayBitmap = bitmap;
        if (getWidth() > 0) { cacheScaledBitmaps(getWidth(), getHeight()); reset(); }
    }

    public void setRevealBitmap(Bitmap bitmap) {
        this.revealBitmap = bitmap;
        if (getWidth() > 0) { cacheScaledBitmaps(getWidth(), getHeight()); invalidate(); }
    }

    public void setReward(Reward reward) { this.hiddenReward = reward; }
    public void setScratchListener(ScratchListener listener) { this.listener = listener != null ? listener : this.listener; }
    public void setHapticEnabled(boolean enabled) { this.hapticEnabled = enabled; }
    public void setPerformanceMode(boolean enabled) { this.performanceMode = enabled; }
    public void setScratchDirection(ScratchDirection direction) { this.scratchDirection = direction; }
    public void setAnimationsEnabled(boolean enabled) { this.animationsEnabled = enabled; }

    public void mask() { reset(); }
    public void reset() { if (getWidth() > 0) resetScratchBitmap(getWidth(), getHeight()); }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public static class SimpleReward implements Reward {
        private String type; private Object data;
        public SimpleReward(String type, Object data) { this.type = type; this.data = data; }
        @Override public String getType() { return type; }
        @Override public Object getData() { return data; }
    }
}
