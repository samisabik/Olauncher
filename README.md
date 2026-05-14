## This fork

A personal fork with a few additions for my own use — left here in case any are useful to others. Brush-stroke summary of what's different from upstream:

- **Font picker** — ~19 bundled font choices (system + Google Fonts) covering techy/mono, pixel, retro, elegant serifs and extravagant display faces. Each option renders in its own font in the picker.
- **Home text colour** — pick a Nord-palette accent for the home text (apps, clock, battery); doesn't affect settings.
- **Clock + battery row** — single line on home, e.g. `06:45 ~ 56%`.
- **Glucose (CGM) display** — pulls the current blood glucose from CamAPS FX's ongoing notification (via `NotificationListenerService`) and shows it under the clock with a trend arrow and Nord-palette colour bands (red / green / orange / red across the usual T1D ranges). Long-press the value for a 6-hour sparkline. Tap to open CamAPS. Optional, toggleable in settings.
- **Stripped-down settings** — removed About / More features / Daily wallpaper / Screen time / double-tap-to-lock / home-button-recents / Twitter-Share-Rate-GitHub-Privacy footer / minimal-todo promo.
- **In-place font + text-size changes** — switching either no longer recreates the activity / jumps back to home.
- **Status bar always hidden** by default on the launcher with stickier hide behaviour.

Everything else is upstream [Olauncher](https://github.com/tanujnotes/Olauncher) — GPLv3.
