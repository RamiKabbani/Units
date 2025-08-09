# Puzzle Drop (Android)

A modern, production-quality Match-3 puzzle game built with Kotlin and Jetpack Compose. Smooth descent animations when gems fall, swap interactions, cascades, scoring, and clean MVVM architecture.

## Features

- Decent-looking, smooth descent and swap animations using Compose springs
- Cascading matches with score multipliers
- MVVM with a clean `GameBoard` logic core
- Material 3 theming
- Touch: tap to select and swipe to swap

## Tech Stack

- Kotlin, Android Gradle Plugin 8.x
- Jetpack Compose (Material 3), Activity Compose
- Coroutines, Lifecycle ViewModel

## Project Structure

- `android/` — Android app module
  - `app/src/main/java/com/example/puzzledrop/` — code
    - `game/` — ViewModel and game logic
    - `ui/` — Compose UI
    - `ui/theme/` — theme

## Getting Started

1. Open the `android` folder in Android Studio (Giraffe or newer).
2. Let Android Studio install the recommended Gradle and Android SDK components.
3. Run on a device or emulator (minSdk 24).

## Build

- Debug: just Run from Android Studio.
- Release: Generate signed bundle/APK as usual. ProGuard/R8 is enabled for release.

## Gameplay

- Swap adjacent gems by swiping or tapping two neighbors.
- Matches of 3+ clear and score. New gems fall with descent animations.
- Cascades grant multipliers. You have 50 moves.
- Press Reset to start a new board.

## Notes & Customization

- Tune board size and animation timing in `GameViewModel` and `GameBoard`.
- Colors and shapes are in `ui/theme` and `GameScreen`.

## License

MIT