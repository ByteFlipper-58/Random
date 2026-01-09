# CONTINUITY.md

## Ledger Snapshot
- **Goal**: Optimize startup time and refactor internal ad logic dependencies.
- **Constraints**: 
  - User's OS: Windows.
  - Project type: Android (Kotlin).
- **Key Decisions**: 
  - `MobileAds.initialize` moved to background thread.
  - `AdsController` decoupled from `Application`.
  - UI Screens delegate Ad checks to ViewModels, which hold `AdsController`.
- **State**:
    - **Done**: Startup optimization, Dependency Injection refactoring, UI Layer refactoring (ViewModels).
    - **Now**: Task completed. Ready for user verification.
    - **Next**: Awaiting user feedback.
- **Open Questions**: None.

## Working Set
- **Files**:
  - `RandomApplication.kt`
  - `MainActivity.kt`
  - `AdsController.kt`
  - `AppOpenAdManager.kt`
  - `AdsModule.kt`
  - UI Screens and ViewModels (`Coin`, `Dice`, `List`, `Lot`, `Numbers`, `Wheel`).
