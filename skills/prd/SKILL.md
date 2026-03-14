---
name: prd
description: "Generate a Product Requirements Document (PRD) for a new feature. Use when planning a feature, starting a new project, or when asked to create a PRD. Triggers on: create a prd, write prd for, plan this feature, requirements for, spec out."
user-invocable: true
---

# PRD Generator

Create detailed Product Requirements Documents that are clear, actionable, and suitable for autonomous implementation by Ralph.

---

## The Job

1. Receive a feature description from the user
2. Ask 3-5 essential clarifying questions (with lettered options)
3. Generate a structured PRD based on answers
4. Save to `tasks/prd-[feature-name].md`

**Important:** Do NOT start implementing. Just create the PRD.

---

## Step 1: Clarifying Questions

Ask only critical questions where the initial prompt is ambiguous. Focus on:

- **Problem/Goal:** What problem does this solve?
- **Core Functionality:** What are the key actions?
- **Scope/Boundaries:** What should it NOT do?
- **Success Criteria:** How do we know it's done?

### Format Questions Like This:

```
1. What is the primary goal of this feature?
   A. Improve workout logging experience
   B. Add analytics/insights
   C. Reduce friction in a specific flow
   D. Other: [please specify]

2. What is the scope?
   A. Minimal viable version
   B. Full-featured implementation
   C. Backend/data layer only
   D. UI only (data layer already exists)
```

This lets users respond with "1A, 2C" for quick iteration.

---

## Step 2: PRD Structure

Generate the PRD with these sections:

### 1. Introduction/Overview
Brief description of the feature and the problem it solves.

### 2. Goals
Specific, measurable objectives (bullet list).

### 3. User Stories

Each story needs:
- **Title:** Short descriptive name
- **Description:** "As a [user], I want [feature] so that [benefit]"
- **Acceptance Criteria:** Verifiable checklist of what "done" means

Each story must be small enough to implement in one focused Ralph iteration (one context window).

**Format:**
```markdown
### US-001: [Title]
**Description:** As a [user], I want [feature] so that [benefit].

**Acceptance Criteria:**
- [ ] Specific verifiable criterion
- [ ] Another criterion
- [ ] Typecheck passes (`./gradlew compileDebugKotlin --no-daemon`)
- [ ] Build passes (`./gradlew assembleDebug --no-daemon`)
```

**Important:**
- Acceptance criteria must be verifiable, not vague. "Works correctly" is bad. "Tapping the set row opens the edit sheet" is good.
- For stories with testable logic, include "Tests pass (`./gradlew testDebugUnitTest --no-daemon`)"

### 4. Functional Requirements
Numbered list of specific functionalities. Be explicit and unambiguous.

### 5. Non-Goals (Out of Scope)
What this feature will NOT include. Critical for managing scope.

### 6. Design Considerations (Optional)
- UI/UX requirements
- Relevant existing Compose components to reuse
- Material3 patterns to follow

### 7. Technical Considerations (Optional)
- Known constraints or dependencies
- Integration points with existing modules
- Room schema changes (remember: use `fallbackToDestructiveMigration()`, no migrations)

### 8. Success Metrics
How will success be measured?

### 9. Open Questions
Remaining questions or areas needing clarification.

---

## Android-Specific Notes

- **No database migrations** — this is a PoC. Room uses `fallbackToDestructiveMigration()`.
- **Module structure**: features in `features/feature-*/`, shared code in `core/core-*/`
- **Build checks**: typecheck = `compileDebugKotlin`, build = `assembleDebug`, tests = `testDebugUnitTest`
- **No "volume"** in any user-facing strings

---

## Output

- **Format:** Markdown (`.md`)
- **Location:** `tasks/`
- **Filename:** `prd-[feature-name].md` (kebab-case)

---

## Checklist

Before saving the PRD:

- [ ] Asked clarifying questions with lettered options
- [ ] Incorporated user's answers
- [ ] User stories are small and specific (completable in one Ralph iteration)
- [ ] Functional requirements are numbered and unambiguous
- [ ] Non-goals section defines clear boundaries
- [ ] Acceptance criteria are verifiable
- [ ] Saved to `tasks/prd-[feature-name].md`
