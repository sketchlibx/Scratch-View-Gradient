# ScratchView 🚀

A highly customizable, buttery-smooth, and premium Scratch Card library for Android. Built perfectly for modern reward zones, gaming, and earning apps (think Google Pay, MPL, or WinZO style scratch cards).

Unlike old libraries that only let you reveal a static image, this library extends `FrameLayout`. This means **you can put ANY custom XML layout inside it!** Until the user scratches the card, all touches are blocked. Once revealed, your inner layout (buttons, animations, etc.) becomes fully interactive.

## ✨ Why use this?
* **Full Layout Reveal:** Put complex UI, Lottie animations, or buttons inside the scratch card.
* **Built-in Particle Engine:** Beautiful spark effects while scratching and a confetti blast on full reveal! 🎉
* **Glassmorphism Borders:** Premium neon glowing borders with blur effects (API 31+).
* **Haptic Feedback:** Gives a physical, satisfying vibration when the user hits the jackpot.
* **Smart Auto-Reveal:** Automatically clears the card when a specific percentage (e.g., 40%) is scratched.
* **Cinematic & Minimalist:** Designed to look highly professional, fitting perfectly into modern dark UI themes.

---

## 📦 Installation

**Step 1:** Add the JitPack repository to your `settings.gradle` (or project-level `build.gradle`):

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url '[https://jitpack.io](https://jitpack.io)' } // <-- Add this line
    }
}

```
**Step 2:** Add the dependency to your app-level build.gradle file:
```groovy
dependencies {
    implementation 'com.github.sketchlibx:Scratch-View-Gradient:beta-1.0.4'
}

```
## 💻 How to Use (It's Super Simple)
### 1. XML Layout
Just wrap your custom reward layout inside the ScratchView tag.
```xml
<sketchlib.scratchview.ScratchView
    android:id="@+id/myScratchView"
    android:layout_width="300dp"
    android:layout_height="300dp"
    android:layout_centerInParent="true"
    android:elevation="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:background="#0F172A"
        android:gravity="center"
        android:orientation="vertical">

        <ImageView
            android:layout_width="80dp"
            android:layout_height="80dp"
            android:src="@drawable/ic_trophy" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="You Won ₹500!"
            android:textColor="#10B981"
            android:textSize="24sp"
            android:textStyle="bold"
            android:layout_marginTop="16dp"/>

        <Button
            android:id="@+id/btnClaim"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Claim Now"
            android:layout_marginTop="20dp"/>

    </LinearLayout>

</sketchlib.scratchview.ScratchView>

```
### 2. Java / Kotlin Setup
Initialize it in your Activity or Fragment and set up the listener to handle the logic.
```java
import sketchlib.scratchview.ScratchView;

public class RewardActivity extends AppCompatActivity {

    private ScratchView scratchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reward);

        scratchView = findViewById(R.id.myScratchView);

        // Optional: Set the exact reward string/data you want to pass when revealed
        // scratchView.setRewardText("500_COINS"); 

        scratchView.setListener(new ScratchView.ScratchListener() {
            @Override
            public void onScratchStart() {
                // Triggered the moment user touches the card
                // Perfect for playing a scratching sound effect!
            }

            @Override
            public void onScratchProgress(float percent) {
                // Returns value between 0.0 to 1.0
                // Example: if (percent > 0.3f) { ... }
            }

            @Override
            public void onScratchEnd() {
                // Triggered when user lifts their finger
            }

            @Override
            public void onRevealed(ScratchView.Reward reward) {
                // Boom! Card is fully revealed. 
                // Now your inner layout (like the Claim button) is clickable.
                Toast.makeText(RewardActivity.this, "Jackpot Unlocked!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

```
## 🛠️ Pro Tips
 * **Resetting the Card:** Want to use the same view again without reloading the activity? Just call scratchView.reset();.
 * **Overlay Image:** Don't want the default metallic gradient? You can set a custom scratch image using scratchView.setOverlayBitmap(yourBitmap);.
## 🛡️ License
This project is licensed under the MIT License - feel free to use it in your personal and commercial projects!