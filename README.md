# 🚀 ScratchView Pro

A smooth and reliable Scratch Card library for Android — built for real apps, not just demos.

If you've ever used a scratch card library before, you’ve probably faced issues like lag, frame drops, or even crashes on low-end devices. That’s exactly the problem this library tries to solve.

ScratchView Pro is focused on performance, stability, and a clean user experience. It’s made for real-world use cases like reward apps, offers, and monetization systems.

---

## ✨ Why this library?

Most scratch libraries do heavy bitmap work on the main thread, which leads to UI lag and poor performance.

In this library:

- Scratch calculations run in the background
- UI stays smooth while the user is scratching
- Works properly even on low-end devices

So overall, the experience feels fast and responsive.

---

## 🔥 Features

- ⚡ Smooth performance (almost 60FPS feel)
- 🖐️ Multi-touch support (scratch with multiple fingers)
- 📳 Haptic feedback for a realistic feel
- 🎭 Support for custom foil and reward images
- ✨ Auto reveal when a threshold is reached
- 🔄 Animated reset for new scratch cards
- 📊 Easy tracking of scratch progress

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
```
---

Step 2: Add Dependency

```gradle
dependencies {
    implementation 'com.github.sketchlibx:Scratch-View-Gradient:1.0.2'
}
```
---

## 💻 How to use

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
        // Example: 0.25 = 25%
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

3️⃣ Reset for new card

```java
scratchView.resetAnimated();
```

# 🎨 Customization

Custom foil image

```java
Bitmap foil = BitmapFactory.decodeResource(getResources(), R.drawable.gold_foil_texture);
scratchView.setScratchOverlayBitmap(foil);
```


Performance mode (for low-end devices)

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

- Fork the project
- Make your changes
- Open a pull request

---

📄 License

MIT License

---

💡 Tip

If you're using this in your app, try adding small sound effects or animations during scratching — it really improves the overall feel.

---

🔥 Example Usage (Advanced)

```java
ScratchView.SimpleReward reward = new ScratchView.SimpleReward("CASHBACK", 500);
binding.scratchView.setReward(reward);

binding.scratchView.setPerformanceMode(true);

// Control reveal behavior
binding.scratchView.setThresholdPercent(0.25f);
binding.scratchView.setAutoRevealEnabled(true);

binding.scratchView.setScratchListener(new ScratchView.ScratchListener() {

    @Override
    public void onScratchStart() {
        binding.tvProgress.setText("Keep scratching...");
    }

    @Override
    public void onScratchProgress(float percent) {
        int progressInt = (int) (percent * 100);
        binding.tvProgress.setText("Revealed: " + progressInt + "%");
    }

    @Override
    public void onScratchEnd() {}

    @Override
    public void onRevealThresholdReached(float percent) {}

    @Override
    public void onRevealed(ScratchView.Reward reward) {
        binding.tvProgress.setText("Card Fully Revealed!");

        if (reward != null) {
            int amount = (int) reward.getData();
            Toast.makeText(MainActivity.this, "You won ₹" + amount + "!", Toast.LENGTH_SHORT).show();
        }
    }
});

binding.btnReset.setOnClickListener(v -> {
    binding.scratchView.resetAnimated();
    binding.tvProgress.setText("Scratch the card to win!");
});
```
---

Built with focus on performance, simplicity, and real-world usage 🚀
