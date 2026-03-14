# PRD: AI Assistant

## Introduction

Replace the current passive AI (scan-only) with an always-available, proactive assistant that feels like a coach standing over your shoulder. It lives in a floating chat UI, silently knows what the user is looking at, has read/write access to all app data, and can take actions on their behalf after a single confirmation tap. The user should never have to explain where they are or what they're doing — the AI already knows.

On every cold launch it greets the user with a 7-day summary and a psychological hook (e.g. "Want me to load last Tuesday's session as a starting point?") to drive engagement. At any other time the user can open it from a floating button and ask anything — looking up movement history, comparing weights, or logging new data.

## Goals

- Make the AI feel like a knowledgeable training partner, not a utility
- Reduce friction: one confirm tap to execute a write action
- Context-aware by default — the AI always knows where the user is in the app
- Full data access — the AI can answer questions about any movement, session, or set
- Proactive at launch — give the user a reason to interact immediately

## User Stories

### US-001: Floating chat FAB and dismissible overlay shell
**Description:** As a user, I want a floating chat button always visible so I can open the assistant at any time without leaving my current screen.

**Acceptance Criteria:**
- [ ] A floating action button (FAB) is visible in the bottom-right corner on every screen in the app
- [ ] Tapping the FAB opens a chat overlay that floats above the current page — the page content remains visible and partially interactive beneath it
- [ ] The overlay occupies roughly the bottom half of the screen (a bottom sheet style), leaving the top half showing the current page
- [ ] The overlay has a visible dismiss button (X or drag handle) that closes it and returns focus to the underlying page
- [ ] The overlay contains: a scrollable message list area, a text input field, and a send button
- [ ] The FAB is hidden while the overlay is open (no double-tap confusion)
- [ ] No AI calls yet — this story is UI scaffolding only
- [ ] Typecheck passes (`./gradlew compileDebugKotlin --no-daemon`)
- [ ] Build passes (`./gradlew assembleDebug --no-daemon`)

### US-002: Basic AI chat — send message, receive text response
**Description:** As a user, I want to type a message and receive a response from the AI so I can have a basic conversation.

**Acceptance Criteria:**
- [ ] Typing a message and tapping Send calls the AI (via existing `core/core-ai/`) with the user's message
- [ ] AI response is displayed in the message list as a chat bubble
- [ ] User messages appear on the right; AI messages appear on the left
- [ ] A loading indicator is shown while the AI is responding
- [ ] Errors are shown inline in the chat (not a toast or crash) — e.g. "Couldn't reach the AI, try again"
- [ ] Conversation is in-memory only — no persistence between app launches (for now)
- [ ] System prompt instructs the AI: it is a personal training coach, present in the moment, concise and direct — no filler, no disclaimers, no "As an AI..." hedging. Talks like a coach, not a chatbot.
- [ ] Typecheck passes
- [ ] Build passes

### US-003: Context injection — AI silently knows current screen
**Description:** As a user, I want the AI to know what screen I'm on so I never have to explain what I'm looking at — it just knows, like a coach standing beside me.

**Acceptance Criteria:**
- [ ] Every AI request includes the current screen name and any relevant entity (e.g. movement name, session date) in the system prompt — never surfaced in the UI
- [ ] No label, badge, or indicator is shown to the user about context — it is entirely invisible
- [ ] Context updates automatically if the user navigates while the overlay is open
- [ ] Screen context is passed as a structured field in the system prompt, not embedded in the user message
- [ ] The system prompt instructs the AI to use context naturally, never announce it ("I can see you're on...") — just use it
- [ ] Typecheck passes
- [ ] Build passes

### US-004: AI data access — read all workout data
**Description:** As a user, I want the AI to be able to answer questions about my training history so I can ask things like "how much did I bench last week?" and get a real answer.

**Acceptance Criteria:**
- [ ] On every AI request, inject the following as structured context in the system prompt:
  - All movement names known in the database
  - Last 14 days of sessions (date, movements, sets with weight/reps/modifier)
  - If a specific movement is in context (from US-003), include its full history (all sessions)
- [ ] System prompt includes a plain-English description of the data schema: what a Session, Movement, and Set are and how they relate
- [ ] System prompt tells the AI what write actions it can propose (see US-006)
- [ ] The AI can correctly answer "what was my best set on X this month?" using injected data
- [ ] Data is fetched fresh on each chat open (not cached across sessions)
- [ ] Typecheck passes
- [ ] Build passes

### US-005: Proactive cold-launch message with 7-day summary
**Description:** As a user, I want to see a helpful summary when I open the app so I'm immediately oriented and motivated to train.

**Acceptance Criteria:**
- [ ] On cold launch (process start, not resume from background), the chat overlay opens automatically after a short delay (300–500ms — enough for the home screen to render first)
- [ ] The AI generates a proactive opening message containing:
  - A brief summary of the last 7 days: number of sessions, notable movements or PRs
  - A concrete hook suggestion, e.g. "You trained 3 times last week — want me to queue up a lower body session?" or "You hit 100kg squat on Thursday — want to aim for 102.5kg today?"
- [ ] If no data exists (new user), the message is a friendly intro: "Welcome! Tap + to log your first workout, or ask me anything."
- [ ] The overlay can be dismissed with the same X button — no forced interaction
- [ ] The proactive message is only triggered on cold launch, not on every app resume
- [ ] Typecheck passes
- [ ] Build passes

### US-006: AI write actions with confirmation
**Description:** As a user, I want the AI to be able to propose actions like logging a set, and confirm them with one tap so I don't have to navigate the UI manually.

**Acceptance Criteria:**
- [ ] The AI can propose write actions by returning a structured JSON block alongside its message (format defined below)
- [ ] Supported actions in this story:
  - `add_set`: add a set to the current session (or today's session) — fields: movementName, weightKg, reps, modifier (optional)
  - `create_session`: start a new session for today
- [ ] When the AI returns an action, the UI renders it as a distinct action card below the AI's message: a brief human-readable description of the action + a "Confirm" button
- [ ] Tapping Confirm executes the write against the Room database and shows a brief success message inline ("Done — added Bench Press 80kg × 5")
- [ ] If the action would target a session that doesn't exist (e.g. add_set with no active session), the AI should first propose `create_session`, then the set
- [ ] Cancelled or ignored action cards remain visible in the chat history (no auto-dismiss)
- [ ] Action JSON format (the AI returns this as a fenced ```action block in its response):
  ```json
  {
    "type": "add_set",
    "movementName": "Bench Press",
    "weightKg": 80.0,
    "reps": 5,
    "modifier": null,
    "sessionDate": "today"
  }
  ```
- [ ] Typecheck passes
- [ ] Build passes

## Functional Requirements

- FR-1: The chat FAB and overlay are implemented as a global composable overlaid in `MainActivity` or the root `NavHost`, so they persist across navigation
- FR-2: A `ChatViewModel` (Hilt-scoped to the Activity) holds conversation state and current screen context
- FR-3: Navigation events update the `ChatViewModel` with the current route; the active destination name is human-readable (not a raw route string)
- FR-4: The system prompt is assembled per-request: base instructions + current screen + data context
- FR-5: Write action parsing: the AI response is scanned for a fenced ```action JSON block; if found, it is parsed and rendered as an action card
- FR-6: All Room writes from AI actions go through existing repositories — no direct DAO access from the chat layer
- FR-7: The cold-launch trigger fires once per process lifecycle (use a flag in the Application or a ViewModel scoped to the Activity)

## Non-Goals

- No conversation persistence across launches (in-memory only for now)
- No voice input
- No multi-step action sequences (one action card per AI response)
- No action types beyond `add_set` and `create_session` in this PRD
- No streaming AI responses (wait for full response, then display)
- No rate limiting or usage caps UI

## Design Considerations

- The overlay should feel light — it's a chat companion, not a takeover. Bottom-sheet style with ~50% screen height.
- FAB uses a chat/message icon, not the existing camera/scan icon
- AI message bubbles should have a subtle distinct color from user bubbles (use Material3 surface variants)
- The action card (confirm button) should be visually distinct from regular chat bubbles — a card with an accent border or background
- Context label ("Chatting on: X") should be secondary/muted text — informative, not prominent

## Technical Considerations

- Use existing `core/core-ai/` for the OpenAI API call — may need to extend it to support longer system prompts
- `ChatViewModel` lives in the `app/` module since it needs access to navigation state and all feature data
- Data injection for US-004 means pulling from multiple repositories — the ViewModel assembles this before each call
- For action parsing, a simple regex or string scan for ` ```action ` blocks is sufficient — no need for a streaming parser
- Cold launch detection: check if the Application's `onCreate` has fired in this process; a simple boolean in the Application class works

## Success Metrics

- User interacts with the launch message (taps Confirm or asks a follow-up) at least once in first session
- AI correctly answers a data question ("what did I do for X last week?") using injected context
- Write action executes correctly and appears in the workout log

## Open Questions

- Should the overlay remember scroll position if dismissed and reopened within the same session?
- When on a movement detail page, should the context injected into the system prompt include the movement name and recent history for that movement automatically, or only inject it when the user asks? (Recommendation: always inject it — the coach already knows what you're looking at.)
