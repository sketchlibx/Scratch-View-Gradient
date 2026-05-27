package sketchlib.scratchview;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleEngine {
    private final View parentView;
    private final List<Particle> particles = new ArrayList<>();
    private final Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private ValueAnimator animator;
    private boolean isEnabled = false;

    public ParticleEngine(View parentView) {
        this.parentView = parentView;
        particlePaint.setStyle(Paint.Style.FILL);
    }

    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
    }

    public void emitSparks(float x, float y, int count) {
        if (!isEnabled) return;
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, random));
        }
        startAnimationIfNeeded();
    }

    public void emitConfetti(int width, int height) {
        if (!isEnabled) return;
        for (int i = 0; i < 50; i++) {
            particles.add(new Particle(width / 2f, height / 2f, random, true));
        }
        startAnimationIfNeeded();
    }

    public void draw(Canvas canvas) {
        if (!isEnabled || particles.isEmpty()) return;
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            particlePaint.setColor(p.color);
            particlePaint.setAlpha(p.alpha);
            canvas.drawCircle(p.x, p.y, p.size, particlePaint);
            
            p.update();
            if (p.alpha <= 0 || p.size <= 0) {
                iterator.remove();
            }
        }
    }

    private void startAnimationIfNeeded() {
        if (animator == null || !animator.isRunning()) {
            animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(1000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(a -> parentView.invalidate());
            animator.start();
        }
    }

    public void stop() {
        if (animator != null) animator.cancel();
        particles.clear();
    }

    private static class Particle {
        float x, y, vx, vy, size;
        int alpha = 255;
        int color;

        Particle(float x, float y, Random random) {
            this(x, y, random, false);
        }

        Particle(float x, float y, Random random, boolean isConfetti) {
            this.x = x;
            this.y = y;
            double angle = random.nextDouble() * 2 * Math.PI;
            double speed = random.nextDouble() * (isConfetti ? 15 : 5);
            this.vx = (float) (Math.cos(angle) * speed);
            this.vy = (float) (Math.sin(angle) * speed);
            this.size = random.nextFloat() * (isConfetti ? 12f : 6f) + 2f;
            this.color = Color.HSVToColor(new float[]{random.nextInt(360), 1f, 1f});
        }

        void update() {
            x += vx;
            y += vy;
            vy += 0.2f; // Gravity
            alpha -= 5;
            size -= 0.1f;
        }
    }
}
