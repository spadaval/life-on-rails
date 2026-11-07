# LoR's launcher against Smart Launcher 6.6 (2026-11-06)

What I saw and read when comparing them, kept for the planning that follows. LoR is at Increment 7.1 (ab65877). Smart Launcher is 6.6 build 025, on Sumeet's Pixel 9a, where it has been installed since 2025-11-29.

## Sumeet's Smart Launcher, hands-on

- **Home is one page and mostly widgets.** At the top, SL's clock widget: big time, date, weather, the next event ("Nov 8 · Clean up the room") and the next alarm. Under it, the Tesla widget. Then three rows of 4, half of them folders, and a 4-icon dock. No search bar on home. Nothing to the left or right.
- **Every icon is themed from the wallpaper,** pink on maroon, including apps that ship no monochrome layer. Work apps carry a briefcase badge.
- **Folder tiles have no background.** The tile is a bare 2×2 of mini icons. Tapping one opens it full-screen over a tinted, blurred wallpaper, with the name editable in place and a + to add apps.
- **Badges show counts** (5, 40, 18), not dots.
- **The app menu** (long press on an icon) is a compact card: the app's shortcuts and a gear for its options. Nothing else.
- **The home menu** (long press on empty space) is a bottom sheet:
  - a padlock that locks the layout;
  - Add icon, Add folder, Add widget, Add page, Remove page;
  - Wallpaper, Global appearance, Home screen;
  - "Show all settings".
- **Page manager:** a spatial map with Search page above home (swipe down), App page below (swipe up), and left and right empty.
- **The drawer is one category per page,** swiped sideways. A bottom tab bar holds the categories as icons, with a dot on any category that has new apps. A large title shows the category ("Contacts"). The header has an index search, the Play Store and a menu. Labels take two lines.
- **Drawer search covers apps only.** With an empty field it shows predicted apps; it filters as you type.
- **Smart Search page** (swipe down):
  - With nothing typed, it shows starred contacts (a call button and an expand arrow on each row) and predicted apps. The Enter target is marked ↳.
  - For `12*7`, four Play Store watch faces come before the calculator's 84. Its ranking is poor.
- **Gestures are off.** Double tap is None, swipe gestures are disabled, and two-finger gestures are Pro. Sumeet uses none of SL's gesture system.
- **Smart Launcher isn't the default home on the Pixel.** A home press shows the chooser (Smart Launcher, Pixel Launcher, Nova).
- I couldn't record its motion. The Pixel's screen recording failed ("failed to retrieve playable Android recording"), so animations here come from the docs.

## Smart Launcher 6.6, from its docs and reviews

Sources:
- docs.smartlauncher.net (the `llms-full.txt` dump, which includes the changelog);
- t.me/s/smartlauncher (beta changelogs);
- smartlauncher.net/blog;
- the Play listing;
- Android Police (May 2026);
- the Android Central forum.

**Home**
- Pages are modules (Home, Apps, Search, Feed, Discover, Smartspacer), and any of them can be put on any of the four sides.
- Fluid grid: widgets resize past their declared sizes and may overlap.
- Dock (since mid-2025): any number of rows and pages, and it can hold widgets.
- Stickers. Separate foldable and landscape layouts.

**Widgets**
- Built-in: Clock (alarm, next event and weather), Weather (AccuWeather, free since 2026), Calendar (up to 21 days), Music (colours from the album art), Battery, Compass, Notes, Photo.
- Widget stacks: scroll either way, rotate automatically, return to a "cover" widget when you come home.
- Pro: popup widgets on an icon's swipe or double tap.

**Drawer**
- Automatic categories, sorted on their server. Since 6.5 that is opt-in, with an offline fallback.
- Drag an app to move it between categories. Editing categories is Pro.
- Sorting by name, install date, or custom order (Pro).
- Hidden apps, with an optional PIN or fingerprint.
- Shows Digital Wellbeing pauses and archived apps.
- The grid starts at the screen's centre, for one-handed reach.

**Search**
- Finds apps, shortcuts, contacts (by use), calendar events for the year ahead, its own settings, media, the Play Store, the web (editable providers, including ChatGPT and Perplexity), a calculator, units, parcel tracking and URLs.
- Results can be dragged onto home.

**Look**
- Ambient theme from the wallpaper. Blur, Frosted, Opaque Glass, Material You and Liquid Glass themes.
- Every Google Font, or your own files.
- About 30 icon shapes. Themed icons applied to any app.
- Badge size and shape. Folder colours and preview styles.

**Motion**
- Home animations: Blur, Fade, Pixel, Free Fall.
- Physics-based home return (6.6). Window-to-icon on closing an app (optional, uneven across devices).
- Haptics on icon gestures. 120 Hz.
- Not Launcher3-based, so app open and close under gesture navigation depend on the phone maker. The docs suggest 3-button navigation where it glitches.

**Gestures**
- Swipes, double tap, two-finger pinch and rotate (Pro), a binding for the home button.
- Per-icon swipes and double tap.
- Quick-launch folders: a tap opens the first app, a swipe opens the folder.
- Screen off and the shade need an Accessibility service.

**Other**
- Nightly backups, and import from Nova.
- A layout lock and Kid mode.

**What people praise:**
- automatic categories;
- search as a hub;
- widget stacks and popup widgets;
- looks;
- frequent updates.

**What people complain about:**
- Pro paywall and subscription;
- glitches under gesture navigation on some phones;
- the Accessibility service getting killed;
- poor categories for niche apps.
