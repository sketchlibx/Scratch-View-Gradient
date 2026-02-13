package sketchlib.scratchview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import androidx.annotation.Nullable;

public class ScratchView extends View {
	
	// --- Dynamic Colors (With Defaults) ---
	private int foilStartColor = Color.parseColor("#0F1C36");
	private int foilEndColor = Color.parseColor("#051124");
	
	private int borderStartColor = Color.parseColor("#200E35");
	private int borderCenterColor = Color.parseColor("#381B5D");
	private int borderEndColor = Color.parseColor("#582C8E");
	
	private float cornerRadius;
	private float borderSize;
	private float brushSize;
	
	// Text Properties
	private String scratchText = "SCRATCH HERE";
	private int scratchTextColor = Color.parseColor("#80FFFFFF");
	private float scratchTextSize;
	
	// Paints
	private Paint foilPaint;
	private Paint borderPaint;
	private Paint scratchPathPaint;
	private Paint textPaint;
	
	// Scratch Logic
	private Bitmap mScratchBitmap;
	private Canvas mScratchCanvas;
	private Path mPath;
	private float lastX, lastY;
	private boolean isRevealed = false;
	private boolean isCalculating = false;
	
	// Config
	private float thresholdPercent = 0.4f; 
	private ScratchListener listener;
	private String hiddenText = "Reward";
	
	public interface ScratchListener {
		void onRevealed(String reward);
	}
	
	public ScratchView(Context context) {
		super(context);
		init(context, null);
	}
	
	public ScratchView(Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}
	
	public ScratchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init(context, attrs);
	}
	
	private void init(Context context, @Nullable AttributeSet attrs) {
		// Default Sizes
		cornerRadius = dpToPx(14);
		borderSize = dpToPx(8);
		brushSize = dpToPx(40);
		scratchTextSize = dpToPx(22);
		
		// --- Read XML Attributes ---
		if (attrs != null) {
			TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ScratchView, 0, 0);
			try {
				foilStartColor = a.getColor(R.styleable.ScratchView_sv_foilStartColor, foilStartColor);
				foilEndColor = a.getColor(R.styleable.ScratchView_sv_foilEndColor, foilEndColor);
				borderStartColor = a.getColor(R.styleable.ScratchView_sv_borderStartColor, borderStartColor);
				borderCenterColor = a.getColor(R.styleable.ScratchView_sv_borderCenterColor, borderCenterColor);
				borderEndColor = a.getColor(R.styleable.ScratchView_sv_borderEndColor, borderEndColor);
				
				cornerRadius = a.getDimension(R.styleable.ScratchView_sv_cornerRadius, cornerRadius);
				borderSize = a.getDimension(R.styleable.ScratchView_sv_borderSize, borderSize);
				brushSize = a.getDimension(R.styleable.ScratchView_sv_brushSize, brushSize);
				thresholdPercent = a.getFloat(R.styleable.ScratchView_sv_thresholdPercent, thresholdPercent);
				
				if (a.hasValue(R.styleable.ScratchView_sv_scratchText)) {
					scratchText = a.getString(R.styleable.ScratchView_sv_scratchText);
				}
				scratchTextColor = a.getColor(R.styleable.ScratchView_sv_scratchTextColor, scratchTextColor);
				scratchTextSize = a.getDimension(R.styleable.ScratchView_sv_scratchTextSize, scratchTextSize);
			} finally {
				a.recycle();
			}
		}
		
		mPath = new Path();
		
		// Setup Paints
		scratchPathPaint = new Paint();
		scratchPathPaint.setAntiAlias(true);
		scratchPathPaint.setDither(true);
		scratchPathPaint.setStyle(Paint.Style.STROKE);
		scratchPathPaint.setStrokeJoin(Paint.Join.ROUND);
		scratchPathPaint.setStrokeCap(Paint.Cap.ROUND);
		scratchPathPaint.setStrokeWidth(brushSize);
		scratchPathPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		
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
		
		setLayerType(LAYER_TYPE_SOFTWARE, null);
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		updateGradients(w, h);
		resetScratchBitmap(w, h);
	}
	
	private void updateGradients(int w, int h) {
		if (w <= 0 || h <= 0) return;
		
		Shader foilShader = new LinearGradient(0, 0, 0, h,
		new int[]{foilStartColor, foilEndColor},
		null, Shader.TileMode.CLAMP);
		foilPaint.setShader(foilShader);
		
		Shader borderShader = new LinearGradient(0, h, w, 0,
		new int[]{borderStartColor, borderCenterColor, borderEndColor},
		new float[]{0f, 0.5f, 1f},
		Shader.TileMode.CLAMP);
		borderPaint.setShader(borderShader);
	}
	
	private void resetScratchBitmap(int w, int h) {
		if (w <= 0 || h <= 0) return;
		
		mScratchBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		mScratchCanvas = new Canvas(mScratchBitmap);
		
		RectF rect = new RectF(0, 0, w, h);
		mScratchCanvas.drawRoundRect(rect, cornerRadius, cornerRadius, foilPaint);
		
		Paint.FontMetrics fm = textPaint.getFontMetrics();
		float textY = (h / 2f) - ((fm.descent + fm.ascent) / 2);
		mScratchCanvas.drawText(scratchText, w / 2f, textY, textPaint);
		
		isRevealed = false;
		mPath.reset();
		invalidate();
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (mScratchBitmap != null && !mScratchBitmap.isRecycled()) {
			canvas.drawBitmap(mScratchBitmap, 0, 0, null);
		}
		
		float inset = borderSize / 2f;
		RectF borderRect = new RectF(inset, inset, getWidth() - inset, getHeight() - inset);
		canvas.drawRoundRect(borderRect, cornerRadius, cornerRadius, borderPaint);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (isRevealed) return true;
		
		float x = event.getX();
		float y = event.getY();
		
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
			mPath.reset();
			mPath.moveTo(x, y);
			lastX = x; lastY = y;
			break;
			case MotionEvent.ACTION_MOVE:
			float dx = Math.abs(x - lastX);
			float dy = Math.abs(y - lastY);
			if (dx >= 4 || dy >= 4) {
				mPath.quadTo(lastX, lastY, (x + lastX) / 2, (y + lastY) / 2);
				lastX = x; lastY = y;
				mScratchCanvas.drawPath(mPath, scratchPathPaint);
				invalidate();
				checkReveal();
			}
			break;
			case MotionEvent.ACTION_UP:
			checkReveal();
			break;
		}
		return true;
	}
	
	private void checkReveal() {
		if (isCalculating || isRevealed) return;
		isCalculating = true;
		
		// Modern threading instead of AsyncTask
		new Thread(() -> {
			if (mScratchBitmap == null) {
				isCalculating = false;
				return;
			}
			int w = mScratchBitmap.getWidth();
			int h = mScratchBitmap.getHeight();
			int revealed = 0;
			int step = 15;
			for (int x = 0; x < w; x += step) {
				for (int y = 0; y < h; y += step) {
					if (mScratchBitmap.getPixel(x, y) == 0) revealed++;
				}
			}
			final float percent = (float) revealed / ((w / step) * (h / step));
			
			// Post result back to main thread
			post(() -> {
				isCalculating = false;
				if (percent >= thresholdPercent) {
					isRevealed = true;
					animateReveal();
				}
			});
		}).start();
	}
	
	private void animateReveal() {
		ValueAnimator anim = ValueAnimator.ofInt(255, 0);
		anim.setDuration(600);
		anim.addUpdateListener(animation -> {
			if ((int) animation.getAnimatedValue() < 50 && mScratchCanvas != null) {
				mScratchCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
			}
			invalidate();
		});
		anim.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				if (mScratchCanvas != null) {
					mScratchCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
					invalidate();
				}
				if (listener != null) listener.onRevealed(hiddenText);
			}
		});
		anim.start();
	}
	
	// --- JAVA SETTERS FOR PROGRAMMATIC CUSTOMIZATION ---
	
	public void setFoilColors(int startColor, int endColor) {
		this.foilStartColor = startColor;
		this.foilEndColor = endColor;
		refreshView();
	}
	
	public void setBorderColors(int startColor, int centerColor, int endColor) {
		this.borderStartColor = startColor;
		this.borderCenterColor = centerColor;
		this.borderEndColor = endColor;
		refreshView();
	}
	
	public void setScratchText(String text) {
		this.scratchText = text;
		refreshView();
	}
	
	public void setBrushSize(float dp) {
		this.brushSize = dpToPx(dp);
		scratchPathPaint.setStrokeWidth(this.brushSize);
	}
	
	public void setThresholdPercent(float percent) {
		this.thresholdPercent = percent;
	}
	
	public void setRewardText(String reward) {
		this.hiddenText = reward;
	}
	
	public void setScratchListener(ScratchListener listener) {
		this.listener = listener;
	}
	
	public void reset() {
		if (getWidth() > 0) resetScratchBitmap(getWidth(), getHeight());
	}
	
	private void refreshView() {
		if (getWidth() > 0 && getHeight() > 0) {
			updateGradients(getWidth(), getHeight());
			resetScratchBitmap(getWidth(), getHeight());
		}
	}
	
	private float dpToPx(float dp) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
	}
}
