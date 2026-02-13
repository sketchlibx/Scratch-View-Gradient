# Scratch-View-Gradient 🎟️

A highly customizable, modern, and smooth Scratch-off (Lottery Ticket) View for Android. 

Built with performance in mind, it features dynamic gradient foils, custom brush sizes, auto-reveal animations, and background threading to ensure your app's UI never freezes while scratching.

## ✨ Features
* **Modern Gradients:** Support for LinearGradient foil and border colors.
* **Auto-Reveal Animation:** Automatically clears the view smoothly when a certain threshold is scratched.
* **Custom Brush & Corners:** Fully control the brush size, border thickness, and corner radius.
* **Background Calculation:** Uses background threads to calculate scratch percentage, ensuring 60fps performance.
* **Overlay Text:** Built-in support to add text over the foil.
* **Fully Programmatic:** Change colors, text, and sizes dynamically via Java/Kotlin.

---

## 📦 Installation

**Step 1:** Add JitPack to your project's root `settings.gradle` or `build.gradle` at the end of repositories:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url '[https://jitpack.io](https://jitpack.io)' }
    }
}

Step 2: Add the dependency to your app's build.gradle:
dependencies {
    implementation 'com.github.sketchlibx:Scratch-View-Gradient:v1.0.0'
}

🚀 Quick Start
1. XML Implementation
To show a prize behind the scratch card, place the ScratchView inside a RelativeLayout directly over your prize view (like a TextView or ImageView).
<RelativeLayout
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_gravity="center">

    <TextView
        android:id="@+id/prizeText"
        android:layout_width="300dp"
        android:layout_height="300dp"
        android:text="₹500 Cashback!"
        android:textSize="32sp"
        android:textColor="#4CAF50"
        android:textStyle="bold"
        android:gravity="center"
        android:background="#E8F5E9" />

    <sketchlib.scratchview.ScratchView
        android:id="@+id/scratchView"
        android:layout_width="300dp"
        android:layout_height="300dp"
        app:sv_foilStartColor="#FF0000"
        app:sv_foilEndColor="#880000"
        app:sv_scratchText="Scratch to Win!"
        app:sv_scratchTextColor="#FFFFFF"
        app:sv_thresholdPercent="0.5" />

</RelativeLayout>

2. Java / Kotlin Implementation (With Pop Animation)
Set up the listener to detect when the user has scratched enough of the view to reveal the reward.
import android.os.Bundle;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import sketchlib.scratchview.ScratchView;

public class MainActivity extends AppCompatActivity {
    
    private ScratchView scratchView;
    private TextView prizeText;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        scratchView = findViewById(R.id.scratchView);
        prizeText = findViewById(R.id.prizeText);
        
        // Hide prize text initially for a surprise effect
        prizeText.setAlpha(0f);
        prizeText.setScaleX(0.5f);
        prizeText.setScaleY(0.5f);
        
        // Listen for the reveal event
        scratchView.setScratchListener(new ScratchView.ScratchListener() {
            @Override
            public void onRevealed(String reward) {
                Toast.makeText(MainActivity.this, "Unlocked!", Toast.LENGTH_SHORT).show();
                
                // Pop-up Animation for the prize
                prizeText.animate()
                    .alpha(1f)
                    .scaleX(1.2f)
                    .scaleY(1.2f)
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator())
                    .withEndAction(() -> {
                        prizeText.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
                    })
                    .start();
            }
        });
    }
}

🛠️ Programmatic Customization
You can change all attributes dynamically at runtime:
// Change Foil Colors
scratchView.setFoilColors(Color.BLUE, Color.DKGRAY);

// Change Border Colors
scratchView.setBorderColors(Color.RED, Color.YELLOW, Color.RED);

// Change Overlay Text
scratchView.setScratchText("LUCKY DRAW");

// Adjust Brush Size
scratchView.setBrushSize(50f);

// Change Reveal Threshold (e.g., reveal after 60% is scratched)
scratchView.setThresholdPercent(0.6f);

// Reset the view to scratch again
scratchView.reset();

🎨 XML Attributes
| Attribute | Type | Default Value | Description |
|---|---|---|---|
| sv_foilStartColor | color | #0F1C36 | Foil gradient start color. |
| sv_foilEndColor | color | #051124 | Foil gradient end color. |
| sv_borderStartColor | color | #200E35 | Border gradient start color. |
| sv_borderCenterColor | color | #381B5D | Border gradient center color. |
| sv_borderEndColor | color | #582C8E | Border gradient end color. |
| sv_cornerRadius | dimension | 14dp | Rounded corners of the view. |
| sv_borderSize | dimension | 8dp | Thickness of the gradient border. |
| sv_brushSize | dimension | 40dp | Thickness of the scratching brush. |
| sv_scratchText | string | SCRATCH HERE | Text displayed over the foil. |
| sv_scratchTextColor | color | #80FFFFFF | Color of the overlay text. |
| sv_scratchTextSize | dimension | 22dp | Size of the overlay text. |
| sv_thresholdPercent | float | 0.4 (40%) | Percentage to scratch before auto-reveal. |
📝 License
MIT License

Copyright (c) 2026 SketchLib

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.


---

आपकी पहली लाइब्रेरी बहुत शानदार तरीके से पब्लिश हो गई है! 🚀 क्या आप इसे GitHub पर अपडेट करने के बाद किसी और टूल (जैसे आपके README Editor) पर काम करना चाहेंगे?

