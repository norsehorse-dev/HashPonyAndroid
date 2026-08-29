# HashPony

Planning document, Rev 2
August 29, 2026
Status: scheduled as a between-releases project. Not yet started.
Origin: suggested by a GitHub user (Aug 2026), who also guessed the name unprompted. If built, they get the first RC link.

If the project starts, this file becomes `PLANNING_1_0_0.md` at the repo root.

---

## 0. Decisions to date

- Aug 29, 2026: **Slot.** Between-releases project. No fixed start date; it fills the next gap between larger releases. Phase A is a clean candidate to hand to a Cowork session. (Closes Q1.)
- Aug 29, 2026: **Verify flow.** Both entry flows ship, folder-first primary. (Closes Q5.)
- Aug 29, 2026: **Algorithms.** Kitchen sink, including xxHash. Concrete set in section 5; xxHash lands as a vendored pure-Kotlin port rather than a dependency, for the minSdk reasons documented there. (Closes Q4.)

Remaining open questions (section 17) carry recommendations that stand as provisional decisions unless overridden.

## 1. Summary

A file and text hashing utility for Android. Pick files, get digests, compare against an expected value, verify whole sets against a sums file, and write sums files that desktop tools accept byte for byte. Modern UI, zero permissions, fully offline. The sha256sum family behind a screen instead of a shell.

## 2. Why this app, why now

Market as of August 2026:

- DeadHash (CodeDead): last release 1.8.3 in March 2025. Single-file oriented, no sums-file verify, no export. The F-Droid build requests shared-storage read permissions, which a SAF-based app does not need.
- Hash Checker: source repo archived August 2025, developer states the app is unsupported, no longer updated on Google Play.
- Hash Droid: algorithm list is Adler-32/Haval/MD2/RIPEMD era, predates SHA-3, effectively dead for years.

Every incumbent is stale or archived. That is the same gap PGPony grew into: a tool people depend on that nobody maintains anymore. Among the pipeline peers (FeedPony queued after the current work; VaultPony and PassPony already shipped and in follow-up phases), this is the smallest build and the cleanest match to the replace-a-dying-service filter.

Name check (August 28, 2026): nothing called HashPony on Google Play, the App Store, F-Droid, or the wider web. Clear.

## 3. Goals and non-goals

Goals for 1.0:

- Hash one or many files with any combination of enabled algorithms, computed in a single read pass per file.
- Hash typed or pasted text.
- Compare a pasted expected value against results with an unmissable match verdict. This is the single most common real-world use: checking a downloaded ISO or APK against a published hash.
- Verify a sums file (SHA256SUMS, .sha256, .md5, .sfv, BSD tag format, plain single-hash files) against the actual files, reporting OK, FAILED, and MISSING per entry.
- Export sums files that desktop `sha256sum -c` and `shasum -c` accept without complaint, plus .sfv.
- Act as a share target so any app can send files straight in.
- Zero permissions, no network. Nothing leaves the device except what the user explicitly shares or saves.

Non-goals, deliberate:

- No network features ever, including fetching sums files from URLs. The manifest ships without INTERNET, which makes the privacy claim independently checkable.
- No password hashing, KDFs, or keyed hashing at 1.0. HMAC is a Phase E candidate (see section 11).
- No encryption. Other Pony apps own that, and Settings cross-links to them.
- No duplicate-file finder at 1.0. It falls out of the engine almost for free, so it stays on the Phase E list rather than in scope.
- No APK signing-certificate verification. AppVerifier already owns that niche and does it well; competing there adds nothing.

## 4. Product shape

Three tabs plus Settings.

**Files tab.** The main screen. Pick files via the system picker (multi-select), or arrive here via the share sheet from any other app. Each file gets a card: name, size, per-algorithm digest rows filling in as computation streams, progress bar during, tap any digest to copy, long-press for uppercase/share. A compare field sits above the results: paste an expected value and every matching digest row lights up green, mismatches stay neutral, and a clear verdict line states match or no match. Pasted input is normalized first (whitespace, colons, and byte separators stripped, case ignored), so output pasted from CertUtil or a website table compares cleanly. With the kitchen-sink algorithm set, several digests share output lengths; the compare field never guesses an algorithm, it compares against everything computed, so ambiguity costs nothing here.

**Text tab.** A text box, live digests below it for the enabled algorithms, tap to copy. UTF-8 encoding. Parity with what DeadHash offered, nothing more.

**Verify tab.** Sums-file checking. Detailed design in section 6.

**Settings.** Enabled algorithms in the four groups from section 5, hex case, export format default, theme, language, the standard family sections (About, help, licenses, "More from NorseHorse" list, hub link).

## 5. Algorithms

Decision (Aug 29): kitchen sink. The set below is the 1.0 target; everything outside the defaults lives behind grouped Settings toggles, off by default, so the Files tab stays clean regardless of how much is available.

**Groups as shown in Settings:**

| Group | Algorithms | Source |
|---|---|---|
| Modern | SHA-224, SHA-256, SHA-384, SHA-512, SHA3-224, SHA3-256, SHA3-384, SHA3-512, BLAKE2b-256, BLAKE2b-512, BLAKE2s-256, BLAKE3-256, Keccak-256 | Platform for SHA-2; Bouncy Castle for the rest |
| Legacy (labeled as such) | MD5, SHA-1, MD4, MD2, RIPEMD-160, Whirlpool | Platform for MD5/SHA-1; Bouncy Castle for the rest |
| Regional | SM3 (GB/T 32905), Streebog-256, Streebog-512 (GOST R 34.11-2012) | Bouncy Castle |
| Checksums and fast hashes (labeled non-cryptographic) | CRC32, Adler-32, XXH32, XXH64, XXH3-64, XXH3-128 | java.util.zip for CRC32/Adler; vendored pure-Kotlin port for xxHash |

Notes:

- Platform MessageDigest (MD5, SHA-1, SHA-2 family) is BoringSSL-backed and uses the ARMv8 crypto extensions where present, so SHA-2 throughput exceeds flash read speed on any recent device.
- One Bouncy Castle dependency covers every remaining cryptographic digest, including the regional pair. PGPonyAndroid presumably already pins a bcprov version; confirm and reuse the same version.
- Keccak-256 is the Ethereum-style padding variant, distinct from SHA3-256; it gets a one-line explainer in the UI because the two are confused constantly.
- SM3 and Streebog are there on purpose: the zh and ru user bases meet these in the wild, and no incumbent Android hasher offers either.
- **xxHash implementation decision.** The obvious dependency, hash4j, implements all four xxHash variants but reaches memory through VarHandle, which Android only provides from API 33, and the fallback library (Zero-Allocation-Hashing) leans on Unsafe. Either would quietly sabotage the minSdk 26 recommendation. So xxHash ships as a small vendored pure-Kotlin port inside `:core`, adapted from the Apache-2.0 implementations with attribution in NOTICE, verified against the reference vectors. XXH32 and XXH64 are short, mechanical ports. XXH3-64/128 is the dense one: committed for 1.0, with the honest fallback that it slips to 1.1 if the port fights back. No new dependency either way.

**Defaults:** SHA-256 enabled; SHA-512, SHA-1, and MD5 one tap away on the Files tab; everything else via Settings. Single-pass multi-digest as before: one 1 MiB read loop updates every enabled digest, so hashing a 4 GB file with six algorithms costs one read, not six.

Explicitly out even from the kitchen sink: KangarooTwelve, Skein, Tiger, Haval, GOST 94. Anything here gets in later only with a real use case attached.

## 6. Verify mode design

Decision (Aug 29): both flows ship, folder-first primary.

This is the one genuinely fiddly part of the app, because sums files reference other files by relative name and SAF has no real paths.

**Folder-first (primary).** User picks a folder via OpenDocumentTree. The app scans the grant for recognizable sums files (SHA256SUMS, MD5SUMS, *.sha256, *.sha512, *.md5, *.sha1, *.b2, *.sfv, xxHash conventions, case-insensitive), lists them, user taps one, verification runs. Relative names resolve inside the granted tree, including `subdir/file` entries via tree walking. This flow matches how the files actually sit on disk after a download.

**Sums-file-first (secondary).** User opens a sums file directly, or arrives via open-with from a file manager. The app parses it, shows the entry list, and asks for the containing folder grant to resolve the referenced files. One extra tap, but it makes open-with work.

Results: per-entry OK / FAILED / MISSING with counts up top, failures pinned first, tap an entry for details (expected vs computed). Entries only; files present in the folder but absent from the sums file are not reported at 1.0, matching `sha256sum -c` behavior.

Algorithm detection order: BSD or xxhsum tag if present, else filename convention (extension or SHA256SUMS-style name; .sfv means CRC32), else hex length, else ask the user. With the kitchen sink, bare hex length is ambiguous (8 hex chars is CRC32 or XXH32; 64 hex chars has half a dozen candidates), so the length fallback only auto-picks when exactly one enabled algorithm matches; otherwise the picker appears.

Open-with registration: intent filters for the common extensions plus text/plain and application/octet-stream with a lenient match, because sums files rarely carry a useful MIME type. Same lesson as the PGPony key-file picker: tight MIME filters make files invisible. In-app "open a sums file" button always exists as the fallback.

Implementation note: DocumentFile tree traversal does one provider round-trip per node and crawls on big trees. Query children in batches through DocumentsContract child-document URIs directly instead of the DocumentFile wrapper.

## 7. Sums file compatibility

Parse (accept):

- GNU coreutils format: `<hex><space><space><name>` and `<hex><space>*<name>` (binary marker). This is what sha256sum, sha512sum, md5sum, and b2sum emit.
- Escaped-filename convention: lines prefixed with `\` where the name contains `\n` or `\\` escapes.
- BSD tag format: `ALGO (name) = hex`, as emitted by `shasum --tag` and BSD digest tools.
- OpenSSL variants: `SHA256(name)= hex` and 3.x-style `SHA2-256(name)= hex`.
- xxhsum output, both its default GNU-style layout and its tagged form; exact tag strings (XXH32, XXH64, XXH3, XXH128) get locked in from real xxhsum fixtures during Phase B rather than from memory.
- SFV: `<name> <crc32hex>` with `;` comment lines. Note the reversed field order relative to everything else.
- Plain single-hash files: a lone hex string, optionally followed by a name. Common next to ISO downloads.
- Comment lines starting with `#`, blank lines, CRLF endings, mixed-case hex. All tolerated silently.

Emit (write):

- Default: GNU format, two-space separator, lowercase hex, LF endings, names relative to the selected root. Byte-for-byte acceptable to `sha256sum -c` and `shasum -c`; this round-trip is a release-gating test, not an aspiration.
- Toggles: BSD tag format, uppercase hex.
- SFV output when the selected algorithm is CRC32, in the conventional `<name> <hex>` order with a comment header.
- Filenames containing newlines get the coreutils escape treatment on write, same as on read.
- Save via CreateDocument with a sensible suggested name (`SHA256SUMS` for a set, `<file>.sha256` for a single file, `<folder>.sfv` for SFV), plus share-as-text for pasting into release notes.

## 8. Architecture and stack

- Kotlin, Jetpack Compose, Material 3. DataStore for preferences. No JSON anywhere at 1.0 (if that changes, the Moshi KSP codegen convention applies).
- Two Gradle modules: `:app` and `:core`. `:core` is pure Kotlin with no Android imports: the streaming multi-digest engine, the algorithm registry, the vendored xxHash port, the sums parser and emitter, hex/normalization helpers. Fully unit-testable on the JVM, and it is the part an eventual iOS twin would re-spec from. No separate HashPonyCore repo; the module is small enough that extraction can happen later if ever needed (see Q12).
- Coroutines throughout. Hash jobs run on a dispatcher sized for I/O, files processed sequentially (parallel files thrash flash storage for no gain), digests updated in the read loop.
- Cancel is first-class: per-file and whole-batch.
- Progress from ContentResolver-reported sizes; indeterminate fallback when a provider reports no size.
- Project layout per standards: `~/Apps/HashPonyAndroid`, PascalCase, its own directory, repo-local git identity `norsehorse-dev` set immediately after init, standard gitignore set (local.properties, keystores, .DS_Store, build/, .gradle/, .kotlin/).

## 9. Performance

Targets, not hopes:

- SHA-256 on a mid-range arm64 device: saturate storage read speed. The hash is not the bottleneck with hardware SHA-2; the read is.
- 1 GB file, single algorithm: a few seconds on UFS storage.
- Several simultaneous algorithms including BC software implementations: still I/O-bound or close to it; BC's pure-Java digests run in the hundreds of MB/s, acceptable at this file-size scale. The vendored xxHash port trades some speed against hash4j's VarHandle tricks and remains far faster than the storage it reads from.
- Buffer: 1 MiB, revisit only with numbers in hand.
- Files over 2 GiB: streams and Long sizes throughout, no int truncation anywhere.

## 10. Privacy posture

- Manifest requests zero permissions. Not "minimal". Zero. All file access flows through SAF pickers and share intents.
- No INTERNET permission, no analytics, no crash reporting, no network stack at all.
- Watch transitive dependencies at every version bump so a library never sneaks a permission or a native lib into the manifest; the merged-manifest check joins the release checklist. The xxHash vendoring decision keeps the dependency list at exactly one (Bouncy Castle), which makes this check trivial.
- This is the headline differentiator against DeadHash and it goes first in every store listing.

## 11. Phase plan

Estimates are part-time evenings. Total to 1.0 is roughly four to six weeks of part-time work after the kitchen-sink expansion.

**Phase A: core.** Project scaffold, `:core` engine with single-pass multi-digest, algorithm registry across all four groups, vendored xxHash port with vector tests, Files tab (pick, hash, progress, copy), Text tab, compare field with normalization, share-target intake. 6 to 9 evenings. Flagged as a clean Cowork handoff.

**Phase B: verify.** Sums parser with the full accept-list from section 7 including SFV and xxhsum fixtures, folder-first and sums-file-first flows, tree resolution including subdirectories, results UI, open-with registration. 7 to 9 evenings. This is the phase where the app earns its existence.

**Phase C: export.** Sums emitter, GNU, BSD, and SFV output, CreateDocument save, share-as-text, round-trip tests against desktop coreutils locked in CI. 3 to 4 evenings.

**Phase D: polish and release readiness.** Folder hashing from the Files tab (recursive, relative paths), file-vs-file compare, Settings complete with family sections, string freeze, localization pass, F-Droid metadata, store assets. 4 to 6 evenings.

**Phase E: post-1.0 candidate list, unscheduled.** HMAC mode, duplicate finder, optional progress notifications (see Q6), XXH3 if it slipped out of 1.0, any algorithm requests that arrive with real use cases attached.

1.0 = A through D. Ship nothing before B works; a hasher without verify is DeadHash again.

## 12. Distribution and release engineering

- Channels at launch: GitHub releases on `norsehorse-dev/HashPonyAndroid`, direct APK page on the hub, F-Droid main repo via a fdroiddata inclusion MR. IzzyOnDroid as the fast interim channel while the MR sits in queue (see Q8); BurnPony's MR is the live data point on current wait times.
- Google Play: recommendation is no at launch. New personal-account apps now carry the paid 14-day closed-testing requirement, and the portfolio history is that Ponies other than PGPony do not earn their keep on Play. Revisit on demand. See Q7.
- Reproducible from day one, applying the PGPony lessons at zero cost instead of paying them later: exactly one working copy, release builds only from the committed git tree, Gradle wrapper and AGP pinned, no toolchain drift between the tag and the published APK.
- Signing: one release key, so direct-APK and reproducible F-Droid installs update over each other in place, same arrangement PGPony has now.
- Versioning: versionName 1.0.0, versionCode 100, family convention.
- Every shipped update gets its section in NorseHorse_Android_Update_Log.md, including the iOS port notes column even while no iOS twin exists, so a future port inherits a complete ledger.

## 13. Branding

- Name: HashPony, checked clear (section 2).
- Icon: drawn in-house in the family style, as told to the person who suggested the app. Suggestion: this app's family color becomes steel silver on graphite, two tones with a diagonal split as a quiet nod to their concept without borrowing OpenKeychain's mark. Silver reads as precision and sits cleanly next to PassPony orange and VaultPony gold. See Q11.
- Domain: none at launch. The hub page carries it; hashpony.app stays available to register if the app earns it. See Q10.
- Tagline candidate: "Checksums, checked." Store listing leads with zero permissions and offline operation.

## 14. Localization

Family set: en, de, es, fr, zh-Hans, pt-BR, ru. String freeze at the end of Phase D, volunteer review for ru, pt-BR, and zh-Hans through the existing tester pool, machine drafts acceptable for UI strings but any security-adjacent copy gets a human pass, per family practice. The Regional algorithm group gives the zh and ru listings something concrete to say. A hasher's string surface is small; this is a day of coordination, not a phase.

## 15. Testing

- `:core` unit tests against official vectors: FIPS 180-4 (SHA-2), FIPS 202 (SHA-3), RFC 7693 (BLAKE2), the BLAKE3 reference vectors, Keccak known answers, SM3 standard vectors, Streebog vectors (RFC 6986), CRC32 knowns, and the xxHash reference vectors for all vendored variants.
- Parser corpus: real outputs from coreutils (GNU two-space and asterisk forms, escaped filenames), `shasum` and `shasum --tag`, OpenSSL 1.x and 3.x, xxhsum in both layouts, SFV files from actual tools, CRLF files produced on Windows, lone-hash files, hostile junk. Every accepted format in section 7 has a fixture.
- Golden round-trip in CI: export sums files, verify them with actual `sha256sum -c`, `shasum -c`, and `xxhsum -c` on the runner. Release-gating.
- Instrumented tests for the SAF flows and share-target intake.
- Manual matrix through the volunteer testers: GrapheneOS/Pixel, MIUI (the PGPony Issue #6 lesson says MIUI file handling deserves its own pass), Samsung OneUI. Several testers from the other Pony apps have volunteered for RC testing before; the person who suggested the app gets the first link regardless.
- RC hosting on the hub, same pattern as pgpony.app/rc.

## 16. iOS and desktop

- iOS twin: was already small before the kitchen sink and is less small now (CryptoKit covers SHA-2 plus Insecure.MD5/SHA-1; nearly everything else in section 5 would need packages or ports). Recommendation unchanged and now stronger: hold. App Store review flagged VaultPony and Umbra under 4.3(a) Design Spam on the same day this week, and a minimalist hash utility is exactly the category that rule gets applied to. The Android gap is where the actual need is. Revisit after the 4.3(a) situation resolves. See Q9.
- Desktop: no. GtkHash, OpenHashTab, and RHash have desktop covered; there is no dying-service gap to fill there.

## 17. Open questions

Q1, Q4, and Q5 are decided (section 0). The rest carry recommendations that stand as provisional decisions unless overridden. Answers roll into Rev 3.

2. **Repo and package.** `norsehorse-dev/HashPonyAndroid` and `com.hashpony.android`, matching PGPonyAndroid and PassPonyAndroid. Recommendation: yes to both.
3. **minSdk.** Recommendation: 26. Nothing in the app needs newer (the xxHash vendoring decision exists precisely to keep it that way), and 24 is available nearly free if F-Droid reach argues for it; below 24 is not worth the friction.
6. **Zero permissions vs progress notifications.** Background progress notifications would require POST_NOTIFICATIONS and foreground-service declarations, breaking the zero-permission claim. Recommendation: hold the line at 1.0; hash in-app with the screen kept on, and revisit only if real users hash 100 GB batches.
7. **Google Play at launch.** Recommendation: no (section 12).
8. **IzzyOnDroid as interim channel.** Recommendation: yes, while the fdroiddata MR waits.
9. **iOS twin timing.** Recommendation: hold until 4.3(a) resolves (section 16).
10. **Domain.** Register hashpony.app now for a few dollars a year, or hub-only until traction. Recommendation: hub-only; nothing about the name is at risk.
11. **Brand color.** Steel silver on graphite as proposed, or something else from the family palette. Recommendation: silver.
12. **Core packaging.** Internal `:core` module vs a separate HashPonyCore repo from day one. Recommendation: internal module; extract later only if something real consumes it.
13. **Text-tab extras.** Live-as-you-type hashing only, or add file-encoding options and an HMAC field later. Recommendation: UTF-8 only at 1.0, HMAC stays Phase E.
