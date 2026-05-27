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
import android.view.View;

import androidx.annotation.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdvancedScratchView extends View {

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
    private Bitmap revealImage;

    // 🔴 FIX: Clipping tools to hide background outside the outline
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

    public AdvancedScratchView(Context context) {
        super(context);
        init(context, null);
    }

    public AdvancedScratchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
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

        if (attrs != null) {
            // Uncomment if using attrs.xml
            /*
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.AdvancedScratchView, 0, 0);
            try {
                brushSize = a.getDimension(R.styleable.AdvancedScratchView_sv_brushSize, brushSize);
                cornerRadius = a.getDimension(R.styleable.AdvancedScratchView_sv_cornerRadius, cornerRadius);
                revealThreshold = a.getFloat(R.styleable.AdvancedScratchView_sv_revealThreshold, revealThreshold);
                autoReveal = a.getBoolean(R.styleable.AdvancedScratchView_sv_autoRevealEnabled, autoReveal);
                glassEffectEnabled = a.getBoolean(R.styleable.AdvancedScratchView_sv_glassEffectEnabled, glassEffectEnabled);
                particlesEnabled = a.getBoolean(R.styleable.AdvancedScratchView_sv_particlesEnabled, particlesEnabled);
            } finally {
                a.recycle();
            }
            */
        }
        
        scratchPaint.setStrokeWidth(brushSize);
        particleEngine.setEnabled(particlesEnabled);
    }

    // ==========================================
    // PUBLIC SETTERS
    // ==========================================

    public void setRevealBitmap(Bitmap bitmap) {
        this.revealImage = bitmap;
        invalidate(); 
    }

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
        
        // 🔴 FIX: Setup Clipping Path properly when size changes
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
            
            // RenderEffect for API 31+ Glassmorphism
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
            // Default Metallic Foil Gradient
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

    @Override
    protected void onDraw(Canvas canvas) {
        // 🔴 FIX: Clip the entire canvas to the rounded rectangle
        // This ensures NOTHING draws outside the corners
        canvas.clipPath(clipPath);
        
        super.onDraw(canvas);

        // Draw Reward Layer
        if (revealImage != null) {
            Rect dest = new Rect(0, 0, getWidth(), getHeight());
            canvas.drawBitmap(revealImage, null, dest, null);
        }

        // Draw Scratch Layer
        if (mScratchBitmap != null && !isRevealed) {
            canvas.drawBitmap(mScratchBitmap, 0, 0, overlayPaint);
        }

        // Draw Particles
        particleEngine.draw(canvas);

        // Draw Glass Border
        if (glassEffectEnabled) {
            // We inset the border by half its stroke width so it draws perfectly inside the clipped area
            float inset = glassBorderPaint.getStrokeWidth() / 2f;
            RectF borderRect = new RectF(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, glassBorderPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isScratchable || isRevealed) return true;

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
            int step = 5; // Performance optimization: check every 5th pixel

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

        // Haptic Feedback for Jackpot
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

        // Circular Expand / Fade Out Animation
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

    // ==========================================
    // BUILDER PATTERN
    // ==========================================
    public static class Builder {
        private final AdvancedScratchView view;

        public Builder(Context context) {
            view = new AdvancedScratchView(context);
        }

        public Builder setGlassMode(boolean enabled) {
            view.glassEffectEnabled = enabled;
            return this;
        }

        public Builder setBrushSize(float sizeDp) {
            view.brushSize = sizeDp * view.getResources().getDisplayMetrics().density;
            view.scratchPaint.setStrokeWidth(view.brushSize);
            return this;
        }

        public Builder setOverlayBitmap(Bitmap bitmap) {
            view.setOverlayBitmap(bitmap);
            return this;
        }

        public Builder setRevealBitmap(Bitmap bitmap) {
            view.setRevealBitmap(bitmap);
            return this;
        }

        public Builder setReward(Reward reward) {
            view.setReward(reward);
            return this;
        }

        public Builder setListener(ScratchListener listener) {
            view.setListener(listener);
            return this;
        }

        public AdvancedScratchView build() {
            return view;
        }
    }
}
