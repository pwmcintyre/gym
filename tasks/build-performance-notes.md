# Build Performance Notes

## Baseline Measurement — 2026-03-14

### Environment
- Gradle 8.6
- Android Gradle Plugin 8.3.2
- JVM args: `-Xmx2g -XX:MaxMetaspaceSize=512m`
- Parallel: `true` (already set)
- Caching: `true` (already set)
- Profile: `build/reports/profile/profile-2026-03-14-18-54-21.html`

### Clean Build Summary (no-build-cache)
| Phase                 | Duration |
|-----------------------|----------|
| Total Build Time      | 52.9s    |
| Startup               | 2.2s     |
| Settings and buildSrc | 0.4s     |
| Loading Projects      | 0.4s     |
| Configuring Projects  | 3.4s     |
| Artifact Transforms   | 18.2s    |
| Task Execution        | ~113s    |

### Top 5 Slowest Tasks (clean build, no-build-cache)

| Rank | Task | Duration | Phase |
|------|------|----------|-------|
| 1 | `:features:feature-workouts:compileDebugKotlin` | 13.4s | Execution |
| 2 | `:app:mergeExtDexDebug` | 11.7s | Execution |
| 3 | `:features:feature-history:compileDebugKotlin` | 8.8s | Execution |
| 4 | `:core:core-model:compileDebugKotlin` | 8.5s | Execution |
| 5 | `:features:feature-scan:compileDebugKotlin` | 5.5s | Execution |

### Additional Notable Tasks
- `:core:core-database:kspDebugKotlin` — 4.2s (Room annotation processing)
- `:core:core-camera:kspDebugKotlin` — 3.5s (Hilt annotation processing)
- `:core:core-ai:compileDebugKotlin` — 3.4s
- `:app:compileDebugKotlin` — 3.3s

### Phase Classification

**Configuration-phase costs:**
- `:app` project config: 2.0s (heaviest — applies all Android DSL)
- Root project `:` config: 0.7s
- `:core:core-ai` config: 0.2s
- All other modules: 0.04–0.14s each
- Total configuration: 3.4s

**Execution-phase costs (dominant):**
- `compileDebugKotlin` across 9 modules is the dominant cost — Kotlin compilation of Compose-heavy feature modules is ~50–60s total
- `kspDebugKotlin` (KSP/Room/Hilt annotation processing) adds ~10s
- `mergeExtDexDebug` (external DEX merging) adds ~12s
- Artifact transforms: 18s (AAR → classes transformation, one-time per dependency version)

### Task Graph for `compileDebugKotlin` (via `--dry-run`)

Total tasks in graph: 184 (all SKIPPED in dry-run = no work to do on up-to-date tree).

Key compilation order (topological):
```
:core:core-model:compileDebugKotlin
  → :core:core-database:kspDebugKotlin → :core:core-database:compileDebugKotlin
  → :features:feature-history:kspDebugKotlin → :features:feature-history:compileDebugKotlin
  → :core:core-ai:kspDebugKotlin → :core:core-ai:compileDebugKotlin
  → :core:core-camera:kspDebugKotlin → :core:core-camera:compileDebugKotlin
  → :features:feature-scan:kspDebugKotlin → :features:feature-scan:compileDebugKotlin
  → :core:core-drivebackup:kspDebugKotlin → :core:core-drivebackup:compileDebugKotlin
  → :features:feature-settings:kspDebugKotlin → :features:feature-settings:compileDebugKotlin
  → :features:feature-workouts:kspDebugKotlin → :features:feature-workouts:compileDebugKotlin
  → :app:kspDebugKotlin → :app:compileDebugKotlin
```

`core-model` is the root dependency — changes to it invalidate all downstream modules.

### Key Observations

1. **Kotlin compilation dominates** (~60% of execution time). Compose compiler plugin makes each feature module expensive.
2. **KSP (Room + Hilt)** adds significant overhead — `core-database` and `core-camera` each take 3–4s for annotation processing.
3. **`mergeExtDexDebug`** (11.7s) is an AGP task for merging external dependency DEX — hard to optimize directly.
4. **Configuration phase** (3.4s) is mostly `:app` project evaluation. With configuration cache this should be near-zero on repeat builds.
5. **Artifact transforms** (18s) are a one-time cost per dependency version change — not repeated on incremental builds.
6. **Parallel execution** is already enabled, so modules compile concurrently where dependency graph allows.
7. **Build cache** is already enabled — cached builds drop from ~53s to ~14s (73% faster).

### Optimization Opportunities

| Opportunity | Expected Impact | Effort |
|-------------|----------------|--------|
| Raise `-Xmx` to `-Xmx3g` | Reduce GC pressure during Kotlin compilation | Low |
| Enable configuration cache | Eliminate 3.4s config phase on repeat builds | Low |
| Enable Gradle daemon | Warm JVM reduces startup 2.2s → <0.5s | Low |
| Upgrade AGP to 8.5+ | Better compileSdk 35 support, potential perf fixes | Medium |
| Move from kapt to KSP (if any kapt remaining) | KSP is faster than KAPT for annotation processing | Medium |

---

## Before/After Comparison

*(To be filled in after BP-003 and BP-004 — see below)*
