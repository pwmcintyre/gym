# PRD: Splash Screen & Logo Refresh

## Introduction

The app launch experience is functional but generic. This PRD covers a logo redesign and launcher icon refresh to give the app a stronger visual identity. The platform splash is already wired up — what remains is the brand exploration and asset replacement.

**Hard constraint:** Startup must remain fast. No intentional delays, no animated splash screens that linger. Visual polish only if it does not noticeably slow launch.

## Goals

- Replace the generic launcher icon with a purpose-built gym/fitness mark
- Update all mipmap densities with the new icon
- Keep the splash experience fast and non-blocking

## User Stories

### US-001: Design a new app logo mark
**Description:** As a user, I want to see a distinctive app icon that immediately communicates this is a gym/workout tracking app.

**Acceptance Criteria:**
- [ ] A new logo concept is chosen — a simple, bold mark (e.g. barbell, dumbbell silhouette, stylized letter, or abstract fitness symbol)
- [ ] Logo works at small sizes (as small as 48×48dp) — no fine detail that disappears at launcher scale
- [ ] Logo is defined as a vector drawable in `res/drawable/` (SVG-compatible XML)
- [ ] Typecheck passes

### US-002: Generate and replace launcher icons at all densities
**Description:** As a developer, I need all mipmap densities updated with the new logo so the launcher shows the correct icon on all devices.

**Acceptance Criteria:**
- [ ] PNG launcher icons replaced at all five densities: mdpi (48px), hdpi (72px), xhdpi (96px), xxhdpi (144px), xxxhdpi (192px)
- [ ] Adaptive icon foreground (`ic_launcher_foreground.xml`) updated to use new vector drawable
- [ ] Adaptive icon background color remains consistent with app theme
- [ ] Round icon variants (`ic_launcher_round`) also updated
- [ ] App installs cleanly and shows new icon on device launcher
- [ ] Build passes (`./gradlew assembleDebug --no-daemon`)

## Functional Requirements

- FR-1: New vector drawable added to `app/src/main/res/drawable/`
- FR-2: All mipmap PNG files in `app/src/main/res/mipmap-*/` replaced
- FR-3: `ic_launcher.xml` and `ic_launcher_round.xml` adaptive icon definitions updated if needed

## Non-Goals

- No animated splash screen or custom launch Activity
- No branding changes to in-app UI (just the launcher icon)
- No new colors or theme changes beyond what the icon requires

## Technical Considerations

- Mipmap PNGs must be generated from the vector at correct pixel densities
- Use Android Studio Image Asset Studio conventions for sizing
- The platform splash already uses the launcher icon, so updating mipmaps automatically updates the splash

## Success Metrics

- New icon visible on device launcher after install
- Icon looks crisp at all densities, including xxxhdpi
- No regression in build time or APK size beyond the new assets

## Open Questions

- Should the icon use a light or dark background? (Consider: most launchers support adaptive icons that adapt to the user's theme)
