# Fast flashy splash screen and logo refresh

Status: backlog idea

Reason:
- The app could benefit from a stronger first impression at launch.
- Any splash treatment must stay extremely fast so startup still feels immediate.

Scope:
- Explore a more compelling app logo and a sharper splash screen presentation.
- Keep the splash brief and non-blocking, with no intentional linger beyond normal app startup time.

Implementation notes:
- Prefer a lightweight system splash configuration and minimal animation over a custom delayed launch screen.
- Treat startup speed as the hard constraint; visual polish is only acceptable if it does not noticeably slow launch.

Progress notes:
- The app now uses a proper platform splash theme on supported Android versions, reusing the existing launcher mark and launch colours instead of falling straight into a blank window background.
- This keeps startup fast while making launch feel more intentional.
- What remains is true brand/logo exploration rather than launch plumbing.
