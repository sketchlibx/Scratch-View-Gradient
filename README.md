🚀 ScratchView Pro

A smooth and reliable Scratch Card library for Android — built for real apps, not just demos.

If you've ever tried scratch card libraries before, you probably noticed issues like lag, frame drops, or even crashes on low-end devices. That’s exactly why this library was created.

ScratchView Pro focuses on performance, stability, and a clean user experience. It’s designed especially for apps where scratch cards are used for rewards, offers, or monetization.

---

✨ Why this library?

Most scratch libraries do heavy bitmap work on the main thread, which causes UI lag and poor performance.

In this library:

- Scratch calculation runs in the background
- UI stays smooth while scratching
- Works well even on low-end devices

So the experience feels fast and responsive.

---

🔥 Features

- ⚡ Smooth performance (close to 60FPS feel)
- 🖐️ Multi-touch support (use multiple fingers)
- 📳 Haptic feedback for realistic scratching
- 🎭 Custom foil & reward support
- ✨ Auto reveal when threshold is reached
- 🔄 Animated reset for new scratch cards
- 📊 Easy to track scratch progress

---

📦 Installation

Step 1: Add JitPack

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}

---

Step 2: Add Dependency

```gradle
dependencies {
    implementation 'com.github.sketchlibx:Scratch-View-Gradient:1.0.2'
}
```
---

💻 How to use

1️⃣ Add in XML

```xml
<sketchlib.scratchview.ScratchView
    android:id="@+id/scratchView"
    android:layout_width="300dp"
    android:layout_height="300dp"
    app:sv_brushSize="45dp"
    app:sv_thresholdPercent="0.45"
    app:sv_hapticEnabled="true"
    app:sv_animationsEnabled="true"
    app:sv_foilStartColor="#FFD700"
    app:sv_foilEndColor="#FFA500"
    app:sv_scratchText="SCRATCH TO WIN" />
```
---

2️⃣ Setup in Java

```java
ScratchView scratchView = findViewById(R.id.scratchView);

// Set reward
scratchView.setReward(new ScratchView.SimpleReward("CASHBACK", 500));

// Listener
scratchView.setScratchListener(new ScratchView.ScratchListener() {

    @Override
    public void onScratchStart() {
        // User started scratching
    }

    @Override
    public void onScratchProgress(float percent) {
        // Update UI if needed (0.25 = 25%)
    }

    @Override
    public void onScratchEnd() {
        // Finger released
    }

    @Override
    public void onRevealThresholdReached(float percent) {
        // Good place for analytics
    }

    @Override
    public void onRevealed(ScratchView.Reward reward) {
        int amount = (int) reward.getData();
        Toast.makeText(MainActivity.this, "You won ₹" + amount, Toast.LENGTH_SHORT).show();
    }
});
```

---

3️⃣ Reset for new card

```java
scratchView.resetAnimated();
```
---

🎨 Customization

Use custom foil image

```java
Bitmap foil = BitmapFactory.decodeResource(getResources(), R.drawable.gold_foil_texture);
scratchView.setScratchOverlayBitmap(foil);
```
---

Enable performance mode (for low-end devices)

```java
scratchView.setPerformanceMode(true);
```
---

⚙️ XML Attributes

Attribute| Description
sv_brushSize| Scratch size
sv_thresholdPercent| Reveal percentage
sv_hapticEnabled| Enable vibration
sv_animationsEnabled| Enable animations
sv_foilStartColor| Gradient start color
sv_foilEndColor| Gradient end color
sv_scratchText| Text on top

---

🤝 Contributing

If you find any issue or want to improve something:

- Fork the repo
- Make changes
- Create a pull request

Simple 👍

---

📄 License

MIT License

---

💡 Tip

If you're using this in your app, try adding a small animation or sound effect with scratching — it makes the experience feel much more premium.

---

Built with focus on performance, simplicity, and real-world usage 🚀
