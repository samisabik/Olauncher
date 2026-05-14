![Olauncher](https://repository-images.githubusercontent.com/278638069/db0acb80-661b-11eb-803e-926cae5dccb4)

## This fork

A personal fork with a few additions for my own use — left here in case any are useful to others. Brush-stroke summary of what's different from upstream:

- **Font picker** — ~19 bundled font choices (system + Google Fonts) covering techy/mono, pixel, retro, elegant serifs and extravagant display faces. Each option renders in its own font in the picker.
- **Home text colour** — pick a Nord-palette accent for the home text (apps, clock, battery); doesn't affect settings.
- **Clock + battery row** — single line on home, e.g. `06:45 ~ 56%`.
- **Glucose (CGM) display** — pulls the current blood glucose from CamAPS FX's ongoing notification (via `NotificationListenerService`) and shows it under the clock with a trend arrow and Nord-palette colour bands (red / green / orange / red across the usual T1D ranges). Long-press the value for a 6-hour sparkline. Tap to open CamAPS. Optional, toggleable in settings.
- **Stripped-down settings** — removed About / More features / Daily wallpaper / Screen time / double-tap-to-lock / home-button-recents / Twitter-Share-Rate-GitHub-Privacy footer / minimal-todo promo.
- **In-place font + text-size changes** — switching either no longer recreates the activity / jumps back to home.
- **Status bar always hidden** by default on the launcher with stickier hide behaviour.

Everything else is upstream Olauncher. Original README below.

---

# Olauncher | Minimal AF Launcher
AF stands for Ad-Free

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/app.olauncher)
[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
    alt="Get it on Play Store"
    height="80">](https://play.google.com/store/apps/details?id=app.olauncher)

### Install using [F-Droid](https://f-droid.org/packages/app.olauncher), [Play Store](https://play.google.com/store/apps/details?id=app.olauncher) or the [latest APK](https://github.com/tanujnotes/Olauncher/releases/).

- To maintain the simplicity of the launcher, a few niche features are available but hidden.

- Please check out the **[About](https://tanujnotes.substack.com/p/olauncher-minimal-af-launcher?utm_source=github)** page in the Olauncher settings for a complete list of features and **FAQs**.

##

License: [GNU GPLv3](https://www.gnu.org/licenses/gpl-3.0.en.html)

Dev: [X/twitter](https://x.com/tanujnotes) • [Bluesky](https://bsky.app/profile/tanujnotes.bsky.social)

##

### My other apps:

- [Pro Launcher](https://play.google.com/store/apps/details?id=app.prolauncher) - Pro version of Olauncher with extra features like widgets, weather, folders, etc.

- [Note to Self](https://play.google.com/store/apps/details?id=com.makenotetoself) - Free and [open source](https://github.com/jeerovan/ntsapp) notes app with chat like interface and end-to-end encryption.

- [Pentastic](https://play.google.com/store/apps/details?id=app.pentastic) - Minimal todo lists. Free and [open source](https://github.com/tanujnotes/Pentastic).
