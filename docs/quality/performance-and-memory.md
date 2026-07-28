# Performance and memory quality gates

## Measured user journeys

Run these on a physical, release-like benchmark build. Emulators are useful for correctness but not acceptance metrics.

1. Cold start to first drawn frame and Home.
2. Warm start from a notification into a conversation.
3. Scroll a populated conversation list and a long message history.
4. Open, send, background and restore a conversation.
5. Discover peers, start and stop a transfer, then leave the screen.
6. Open the Moments feed, scroll media-heavy content and return to Home.

`MainActivity.reportFullyDrawn()` marks the user-visible startup endpoint, and the `P2PService.start` trace section identifies foreground-service startup cost in Perfetto traces.

## Benchmark implementation

The dedicated `:benchmark` `com.android.test` module runs Macrobenchmark against the release-like app variant. Publish its JSON plus Perfetto traces as CI artifacts.

Required benchmark classes:

- `StartupBenchmark`: cold and warm startup, using `StartupTimingMetric`.
- `BaselineProfileGenerator`: cold startup plus primary Tab navigation, producing baseline and startup profiles on a physical API 33+ device (or rooted API 28+).
- `HomeScrollBenchmark`: frame timing while switching tabs and scrolling lists.
- `ConversationBenchmark`: open a conversation, send a message and return.
- `MomentsFeedBenchmark`: reserved until the feed feature lands.

Do not merge a numeric regression until a physical-device baseline exists. Initial release targets are p95 cold startup below 2.5 s on the selected reference device, no sustained scroll jank above 5%, and no regression larger than 10% from the approved baseline.

Macrobenchmarks are intentionally separate from emulator CI. Run them on the approved physical-device lab and retain the generated JSON and Perfetto traces as build artifacts; generic emulator results are correctness signals only.

## Baseline Profile workflow

Run `./gradlew :app:generateReleaseBaselineProfile -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile` on the reference device. The Gradle plugin writes the generated profile into the app's generated baseline-profile source set, where it is packaged with the release build. Repeat the Macrobenchmark using the release-like build; do not write profile rules by hand.

## Memory and leak gates

- LeakCanary is enabled for debug builds. Resolve retained Activity, ViewModel, Service and large Bitmap references before release.
- Capture a heap profile for each measured journey before and after repeated navigation cycles. The retained heap must stabilize after GC; a monotonic increase is a release blocker.
- For media, P2P and WebRTC, explicitly release streams, codecs, sockets, callbacks and coroutine scopes when the owning lifecycle ends.
- Publish heap dumps and Perfetto traces for detected regressions as CI artifacts; never include message contents or user identifiers.

## Database and reliability gates

- Every database version increments with an explicit migration registered in `WeDatabaseMigrations`.
- Exported Room schemas are committed to `core/database/schemas`.
- Each migration has an instrumentation test that creates the previous schema, inserts representative contacts, sessions and messages, runs the migration, and verifies both schema and data.
- Production must not use destructive migration.
