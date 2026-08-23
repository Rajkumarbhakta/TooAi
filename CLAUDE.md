# TooAi — working notes for Claude Code

Offline-first Android app. On-device LLM chat plus ML Kit vision tools (doc scan, QR, text
recognition, subject segmentation). Single module `:app`, package `com.rkbapps.tooai`.

Kotlin 2.3.10 · AGP 9.0.0 · Compose BOM 2026.02.01 · minSdk 26 / target 36 · Hilt · Room ·
Navigation 3 · LiteRT-LM.

---

## Build & run

```sh
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Two activities: `activity/MainActivity` (launcher, Navigation 3) and `activity/AiWriterActivity`
(system-wide text actions, translucent).

Drive the AI Writer without a host app:

```sh
adb shell "am start -a android.intent.action.PROCESS_TEXT -t text/plain \
  --es android.intent.extra.PROCESS_TEXT 'we should of went their yesterday' \
  --ez android.intent.extra.PROCESS_TEXT_READONLY false \
  -n com.rkbapps.tooai/.activity.AiWriterActivity"
```

Quote the whole `am start` for the remote shell or spaces in the extra get word-split.

Unit tests are `./gradlew testDebugUnitTest`. AGP 9 only configures unit tests for the **debug**
variant — `testReleaseUnitTest` does not exist.

---

## Releasing

`.github/workflows/android_release.yml` builds a signed release APK on every push to `master` and
attaches it to a GitHub Release. No manual build or upload.

The tag is `v<versionName>`, read out of `app/build.gradle.kts`. **If that tag already exists the
release is skipped and the job still ends green** — so publishing a release means bumping
`versionCode` *and* `versionName` first. Pushing anything else to master is harmless.

Signing: the workflow base64-decodes `secrets.KEYSTORE_BASE64` to `app/release.keystore`, which is
what `signingConfigs` at `app/build.gradle.kts:31-41` looks for; passwords come from
`KEY_STORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD`. Local key is
`~/dev/keystore/tooai_keystore.jks`, alias `key0`, cert `CN=Rajkumar Bhakta, O=RKB APPS`.

Two traps that guard rails now exist for — don't remove them:

- The signing config only applies `if (keystoreFile.exists())`, so a missing secret yields a
  silently **unsigned** APK rather than an error. The workflow asserts every secret is non-empty
  before building, and rejects `app-release-unsigned.apk` after.
- The keystore is decoded *before* the test run, so every Gradle invocation in the job sees it. If
  it appeared only later, a reused configuration cache could pin the "no keystore" branch.

`*.keystore`/`*.jks`, `app/release/`, and `gradle/gradle-daemon-jvm.properties` are gitignored. The
last one pins `toolchainVendor=JETBRAINS`; committing it makes CI reject Temurin and re-download a
JetBrains Runtime every run.

---

## Architecture

- **DI**: Hilt. Only one module — `di/DatabaseModule.kt` (Gson, Room `Database`, all DAOs).
  Repositories are plain `@Inject constructor`, unscoped.
- **DB**: Room v5, `app_database`. Entities `QrScan`, `RecognizedText`, `DocumentScans`,
  `LlmModel`, `ChatSession`, `ChatMessage`.
- **Nav**: Navigation 3 (`navigation/NavManager.kt`), back stack is a process-global
  `MainActivity.backStack` snapshot list. `ChatViewModel.init` reads its route off that static —
  which is why nothing else should reuse `ChatViewModel`.
- **LLM**: LiteRT-LM. User imports `.task` / `.litertlm` files into
  `getExternalFilesDir(null)/-imports/`, registered as `LlmModel` rows. `Backend.CPU` hard-coded.

Two independent LLM paths, deliberately:

| | Chat | AI Writer |
|---|---|---|
| Repository | `ui/screens/chat/ChatRepository.kt` | `ui/screens/ai_writer/AiWriterRepository.kt` |
| Persists to DB | yes (`ChatDao`) | **no — injects only `LlmModelDao`** |
| System prompt | markdown-friendly | output-only, no fences/preamble |

The writer must never touch `ChatDao`: a rewrite triggered from another app's selection must not
create chat sessions. That separation is enforced structurally (no `ChatDao` in its constructor).

---

## AI Writer

Android's `ACTION_PROCESS_TEXT` puts an entry in the text-selection toolbar of **every** app. The
activity receives the selection and, when `EXTRA_PROCESS_TEXT_READONLY` is false, returns
replacement text that Android writes back into the field. `ACTION_SEND` is the share-sheet
fallback (read-only — Replace is hidden there).

### Pages

`AiWriterPages { HOME, PROMPT, POLISH, SUMMARIZE, PROOFREAD }` lives in `AiWriterState.kt` (not
the UI file — the ViewModel needs it).

Each generation page is a filter over `PredefinePrompts.listOfPrompts`. No page-specific prompt
plumbing:

| Page | `TypeOfPrompt` | Chips | Default |
|---|---|---|---|
| POLISH | Rewrite | Formal · Casual · Friendly | Formal |
| SUMMARIZE | Summary | Bullet points · Short paragraph · Concise | Short paragraph |
| PROOFREAD | Proofread | Grammar · Clarity · Concise | Grammar |
| PROMPT | — | none (free-form input) | — |

Helpers: `promptType()`, `prompts()`, `defaultPrompt()`, `isGenerationPage()`, `titleRes()`.
Opening a generation page auto-runs its default; `PROMPT` waits for typed input.

### State

`Stage = Idle | LoadingModel | Generating(partial) | Done(variants, index) | Error(message)`.

`Done` holds a **list**: regenerate and tone-chip changes append a variant and move the pager
(`‹ 2 ›`). Variants reset on page change. The list lives in the ViewModel (`variants`) so a
regenerate can append while `Generating` is on screen.

`run()` takes a **fully composed prompt string**, not a `Prompts` — that's what lets the tone
pages and the free-form page share one path, and lets `regenerate()`/`retry()` replay either via
`lastFullPrompt`.

Free-form composition (`PROMPT` page):
`"$instruction\n\nText:\n$source"` when `useSourceAsContext`, else the instruction alone.

### Engine lifecycle

Engine loads cold on first action, stays loaded across actions on a page, and is released on
**every** exit path — `AiWriterActivity.finish()` is overridden to call `viewModel.dismiss()`, so
the close button, swipe-dismiss, system back, Replace and the model-manager hand-off are all
covered. `onCleared()` repeats it as an idempotent safety net.

Verified: ~1.9 GB PSS while generating → ~100 MB after dismiss.

### Key files

```
activity/AiWriterActivity.kt            intent parsing, result return, finish() override
ui/screens/ai_writer/AiWriterState.kt   pages + helpers + state/Stage
ui/screens/ai_writer/AiWriterViewModel.kt  engine, variants, cancellation
ui/screens/ai_writer/AiWriterRepository.kt one-shot LiteRT (no ChatDao)
ui/screens/ai_writer/AiWriterSheet.kt   all Compose (~1000 lines)
utils/PredefinePrompts.kt               every prompt string + type/sub-type enums
```

---

## ⚠️ Uncommitted ML Kit GenAI scaffolding — currently dead code

`AiWriterViewModel.kt` contains ~170 lines of Google ML Kit GenAI (Gemini Nano) sample code:
`Proofreader`, `Summarizer`, `Rewriter` clients plus `prepareAndStart*` / `start*Request`
functions.

**None of it is reachable.** Nothing outside the ViewModel calls any of it — every UI path still
goes through LiteRT-LM. The functions discard their results (`runInference(...).await().results`
assigned and dropped; the streaming lambda body is an empty comment) and never touch
`AiWriterState`. The only runtime effect is that three clients are constructed at ViewModel init
and closed in `onCleared()`.

Do not treat this as the architecture, and do not "fix" it silently — ask first whether the
intent is to migrate Polish/Summarize/Proofread onto Gemini Nano (with LiteRT as fallback for
devices without it) or to delete it.

Also uncommitted and worth knowing:

- `libs.versions.toml`: `genai-rewriting` uses `version.ref="summarization"`. The `rewriting`
  alias on line 29 is unused. Same value today, so it compiles, but the two will drift.
- `kotlinx-coroutines-guava` was already present; it's what makes `.await()` work on ML Kit's
  `ListenableFuture`.
- No R8 rules or manifest `<meta-data>` for GenAI feature auto-download.

---

## Gotchas (learned the hard way — don't rediscover these)

**PROCESS_TEXT**
- Never set `launchMode="singleTask"`/`singleInstance` or `noHistory` on `AiWriterActivity` —
  they silently break `setResult`, and the replacement text is dropped with no error.
- The entry only appears after the app has been launched once post-install (Android excludes
  stopped packages from intent resolution).
- **Samsung One UI gates it behind an opt-in allowlist** stored in
  `Settings.Global.process_text_manager_apps` (`#`-delimited activity names). Not an AOSP key —
  stock Android shows it automatically. An app can read that key but cannot write it
  (`WRITE_SECURE_SETTINGS` is signature/privileged), so this can't be automated. The share-sheet
  entry point has no such gate.

**Coroutines**
- `catch (e: Exception)` around a `collect` swallows `CancellationException` and lands it as an
  `Error` stage on top of whatever Stop/back just set — it surfaced as
  "StandaloneCoroutine was cancelled" in the result card. Always rethrow it first.
- Cancelling the coroutine does **not** stop the native LiteRT decode. You must also call
  `conversation.cancelProcess()` (`repository.cancel`). See `cancelGeneration()`.

**Compose**
- An infinite `rememberInfiniteTransition` left running in an idle state burns a full CPU core.
  `TypingDots` was being shown in `Stage.Idle` and did exactly that. Measure with
  `/proc/<pid>/stat` deltas — `top` output on this device is unreliable to parse.
- `Modifier.rotate()` in a draw scope rotates the **geometry**, not just the brush. The animated
  gradient border clips to the border ring (`Path.combine(Difference, outer, inner)`) and rotates
  an oversized gradient rect inside that clip. The colour list must repeat its first entry last
  or the sweep shows a seam.
- `FlowRow` is `androidx.compose.foundation.layout`, not `material3`.
- The sheet needs `imePadding()` — without it the keyboard covers the `PROMPT` page composer.
  Only that page has a text field, so it's easy to miss.

**Misc**
- XML comments cannot contain `--`; it fails the manifest merger with an opaque parse error.
- `getPromptTypeIfApplied()` in `ChatScreen.kt` is now derived from `listOfPrompts` rather than a
  hand-written prefix chain (the old one silently omitted `REWRITE_CASUAL` and
  `CODE_SNIPPET_TYPESCRIPT`). It must stay in agreement with `removePromptIfApplied()` or
  messages render their raw prompt prefix.
- `Prompts.subType` carries a `displayString`, not the enum `.name` — don't regex-split it
  ("JavaScript" would become "Java Script").

---

## Verifying AI Writer changes on device

```sh
# generation stopped?  (~500 jiffies/5s = one core busy, 0 = idle)
PID=$(adb shell pidof com.rkbapps.tooai | tr -d '\r')
A=$(adb shell cat /proc/$PID/stat | awk '{print $14+$15}'); sleep 5
B=$(adb shell cat /proc/$PID/stat | awk '{print $14+$15}'); echo $((B-A))

# engine released?  should return to ~100 MB after dismiss
adb shell dumpsys meminfo com.rkbapps.tooai | grep "TOTAL PSS"

# writer must not create chat rows
adb shell "run-as com.rkbapps.tooai cat /data/data/com.rkbapps.tooai/databases/app_database" > /tmp/a.db
sqlite3 /tmp/a.db "select count(*) from chat_sessions; select count(*) from chat_messages;"

# selection-menu registration
adb shell dumpsys package com.rkbapps.tooai | grep -A5 PROCESS_TEXT
```

Screenshot with `adb exec-out screencap -p > shot.png`. Sheet element positions shift between
pages — screenshot before blind-tapping, or taps land on the scrim and dismiss the sheet.
