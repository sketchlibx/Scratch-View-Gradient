package sketchlib.scratchview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 🔴 FIX: Extends FrameLayout instead of View so it can hold child layouts
public class ScratchView extends FrameLayout {

    // Interfaces
    public interface Reward {
        String getType();
        Object getData();
    }

    public interface ScratchListener {
        void onScratchStart();
        void onScratchProgress(float percent);
        void onScratchEnd();
        void onRevealed(Reward reward);
    }

    // Engines
    private BezierPathManager pathManager;
    private ParticleEngine particleEngine;
    private ExecutorService asyncExecutor;
    private Handler mainHandler;

    // Paints
    private Paint scratchPaint;
    private Paint overlayPaint;
    private Paint glassBorderPaint;
    private Paint textPaint;

    // Bitmaps & Canvas
    private Bitmap mScratchBitmap;
    private Canvas mScratchCanvas;
    private Bitmap overlayImage;

    // Clipping tools to hide background outside the outline
    private Path clipPath;
    private RectF clipRect;

    // Configurations
    private float brushSize = 50f;
    private float cornerRadius = 30f;
    private float revealThreshold = 0.4f; // 40%
    private boolean autoReveal = true;
    private boolean isRevealed = false;
    private boolean glassEffectEnabled = true;
    private boolean particlesEnabled = true;
    private boolean isScratchable = true;
    private String scratchText = "SCRATCH HERE";
    private int foilColor = Color.parseColor("#1E293B");

    // State
    private boolean isCalculating = false;
    private Reward currentReward;
    private ScratchListener listener;

    public ScratchView(Context context) {
        super(context);
        init(context, null);
    }

    public ScratchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // 🔴 Ensure FrameLayout calls its draw methods
        setWillNotDraw(false);

        pathManager = new BezierPathManager();
        particleEngine = new ParticleEngine(this);
        asyncExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        clipPath = new Path();
        clipRect = new RectF();

        setLayerType(LAYER_TYPE_HARDWARE, null);

        // Paints Setup
        scratchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scratchPaint.setStyle(Paint.Style.STROKE);
        scratchPaint.setStrokeCap(Paint.Cap.ROUND);
        scratchPaint.setStrokeJoin(Paint.Join.ROUND);
        scratchPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        overlayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        glassBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassBorderPaint.setStyle(Paint.Style.STROKE);
        glassBorderPaint.setStrokeWidth(5f);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        
        scratchPaint.setStrokeWidth(brushSize);
        particleEngine.setEnabled(particlesEnabled);
    }

    // ==========================================
    // PUBLIC SETTERS
    // ==========================================

    public void setOverlayBitmap(Bitmap bitmap) {
        this.overlayImage = bitmap;
        if (getWidth() > 0 && getHeight() > 0) {
            resetScratchLayer(getWidth(), getHeight());
        }
    }

    public void setListener(ScratchListener listener) {
        this.listener = listener;
    }

    public void setReward(Reward reward) {
        this.currentReward = reward;
    }

    public void reset() {
        if (getWidth() > 0 && getHeight() > 0) {
            resetScratchLayer(getWidth(), getHeight());
        }
        isScratchable = true;
    }

    // ==========================================

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        
        clipRect.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(clipRect, cornerRadius, cornerRadius, Path.Direction.CW);

        setupGlassBorder(w, h);
        resetScratchLayer(w, h);
    }

    private void setupGlassBorder(int w, int h) {
        if (glassEffectEnabled) {
            LinearGradient neonGradient = new LinearGradient(0, 0, w, h, 
                new int[]{Color.parseColor("#4F46E5"), Color.parseColor("#EC4899"), Color.parseColor("#8B5CF6")}, 
                null, Shader.TileMode.CLAMP);
            glassBorderPaint.setShader(neonGradient);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRenderEffect(RenderEffect.createBlurEffect(2f, 2f, Shader.TileMode.CLAMP));
            }
        }
    }

    private void resetScratchLayer(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (mScratchBitmap != null && !mScratchBitmap.isRecycled()) mScratchBitmap.recycle();

        mScratchBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mScratchCanvas = new Canvas(mScratchBitmap);

        RectF rect = new RectF(0, 0, w, h);
        
        if (overlayImage != null) {
            Bitmap scaled = Bitmap.createScaledBitmap(overlayImage, w, h, true);
            mScratchCanvas.drawBitmap(scaled, 0, 0, null);
        } else {
            LinearGradient foilGradient = new LinearGradient(0, 0, w, h, 
                new int[]{Color.parseColor("#1E293B"), Color.parseColor("#334155"), Color.parseColor("#0F172A")}, 
                null, Shader.TileMode.MIRROR);
            Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setShader(foilGradient);
            mScratchCanvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint);
            
            textPaint.setColor(Color.WHITE);
            textPaint.setTextSize(60f);
            mScratchCanvas.drawText(scratchText, w / 2f, h / 2f + 20f, textPaint);
        }

        isRevealed = false;
        overlayPaint.setAlpha(255);
        invalidate();
    }

    // 🔴 FIX: dispatchDraw instead of onDraw so children (custom layouts) render properly underneath
    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Clip the entire canvas to the rounded rectangle
        canvas.clipPath(clipPath);
        
        // Draws the custom child views FIRST (Your reward XML layout)
        super.dispatchDraw(canvas);

        // Draw Scratch Layer OVER the children
        if (mScratchBitmap != null && !isRevealed) {
            canvas.drawBitmap(mScratchBitmap, 0, 0, overlayPaint);
        }

        // Draw Particles OVER the scratch layer
        particleEngine.draw(canvas);

        // Draw Glass Border
        if (glassEffectEnabled) {
            float inset = glassBorderPaint.getStrokeWidth() / 2f;
            RectF borderRect = new RectF(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, glassBorderPaint);
        }
    }

    // 🔴 FIX: Intercept touches. If not revealed, STEAL the touch so child buttons aren't clicked
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isScratchable || isRevealed) {
            return super.onInterceptTouchEvent(ev); // Pass touch down to child layouts
        }
        return true; // Block children, redirect to our onTouchEvent for scratching
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isScratchable || isRevealed) {
            return super.onTouchEvent(event); // Let child layouts handle clicks
        }

        int pointerIndex = event.getActionIndex();
        int pointerId = event.getPointerId(pointerIndex);
        float x = event.getX(pointerIndex);
        float y = event.getY(pointerIndex);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                Path startPath = pathManager.startPath(pointerId, x, y);
                mScratchCanvas.drawPath(startPath, scratchPaint);
                if (listener != null) listener.onScratchStart();
                break;

            case MotionEvent.ACTION_MOVE:
                boolean needsInvalidate = false;
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int id = event.getPointerId(i);
                    Path path = pathManager.updatePath(id, event.getX(i), event.getY(i));
                    if (path != null) {
                        mScratchCanvas.drawPath(path, scratchPaint);
                        needsInvalidate = true;
                        if (particlesEnabled && Math.random() > 0.6) {
                            particleEngine.emitSparks(event.getX(i), event.getY(i), 2);
                        }
                    }
                }
                if (needsInvalidate) {
                    invalidate();
                    calculateRevealPercentAsync();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                pathManager.endPath(pointerId);
                if (event.getPointerCount() == 1 && listener != null) {
                    listener.onScratchEnd();
                }
                break;
        }
        return true;
    }

    private void calculateRevealPercentAsync() {
        if (isCalculating || isRevealed || mScratchBitmap == null) return;
        isCalculating = true;

        final Bitmap bitmapCopy = mScratchBitmap.copy(Bitmap.Config.ARGB_8888, false);
        
        asyncExecutor.execute(() -> {
            int w = bitmapCopy.getWidth();
            int h = bitmapCopy.getHeight();
            int[] pixels = new int[w * h];
            bitmapCopy.getPixels(pixels, 0, w, 0, 0, w, h);

            int transparentCount = 0;
            int step = 5; 

            for (int i = 0; i < pixels.length; i += step) {
                if (Color.alpha(pixels[i]) == 0) transparentCount++;
            }

            float percent = (float) transparentCount / (pixels.length / step);
            bitmapCopy.recycle();

            mainHandler.post(() -> {
                isCalculating = false;
                if (listener != null) listener.onScratchProgress(percent);

                if (autoReveal && percent >= revealThreshold && !isRevealed) {
                    triggerRevealAnimation();
                }
            });
        });
    }

    public void triggerRevealAnimation() {
        if (isRevealed) return;
        isRevealed = true;
        isScratchable = false;

        if (particlesEnabled) {
            particleEngine.emitConfetti(getWidth(), getHeight());
        }

        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        ValueAnimator anim = ValueAnimator.ofInt(255, 0);
        anim.setDuration(800);
        anim.addUpdateListener(a -> {
            overlayPaint.setAlpha((int) a.getAnimatedValue());
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (listener != null) listener.onRevealed(currentReward);
            }
        });
        anim.start();
    }
}
