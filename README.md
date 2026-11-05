# HashPony for Android

A file hasher for Android. Hash files, whole folders and text, check a
download against a published hash, verify a folder against its sums file,
and write sums files that `sha256sum -c`, `shasum -c` and `xxhsum -c`
accept byte for byte.

HashPony asks for no Android permissions. There is no INTERNET permission,
so it can't send anything anywhere, and you can check that in the manifest
yourself. All file access goes through the system file picker and the share
sheet. No accounts, no ads, no analytics, no tracking.

Requires Android 8.0 or newer.

## Features

- Hash one file, many files or a whole folder with any mix of algorithms,
  all in a single read of each file
- Live digests for typed or pasted text (UTF-8)
- Compare against a published value. A match turns that digest green; if
  nothing matches, HashPony shows the closest digest of the same length and
  marks every character that differs
- Verify a folder against its sums file, with an OK, Failed or Missing
  result for every entry
- Export sums files in GNU, BSD tag or SFV format, saved or shared as text
- Algorithm kits: Downloads (SHA-256, SHA-512, SHA-1, MD5), Forensics
  (MD5, SHA-1, SHA-256), Everything, or your own Custom set
- File against file: pick two files and see whether they're identical
- Show any digest as a QR code. Scanning one in hands off to a scanner app
  you already have, so HashPony never touches the camera
- A plain-text verify receipt listing every file, its result and digests,
  and when it was checked
- A Quick Settings tile that opens the file picker, and a share target so
  any app can send files straight in

## Algorithms

Everything is off by default except SHA-256. Turn on what you need in
Settings, Algorithms, or pick a kit.

- SHA-2: SHA-256, SHA-512, SHA-384, SHA-224
- SHA-3: SHA3-256, SHA3-512, SHA3-384, SHA3-224, plus Keccak-256 (the
  Ethereum padding, which gives different output from SHA3-256)
- BLAKE: BLAKE3, BLAKE2b-512, BLAKE2b-256, BLAKE2s-256
- Older: MD5, SHA-1, RIPEMD-160, Whirlpool, MD4, MD2. MD5, SHA-1, MD4 and
  MD2 are broken for security and are only here to match sums people
  already publish
- National standards: SM3 (China, GB/T 32905), Streebog-256 and
  Streebog-512 (Russia, GOST R 34.11-2012)
- Checksums, not for security: CRC32, Adler-32, XXH3-64, XXH3-128, XXH64,
  XXH32

## Sums files

HashPony reads the formats the common tools write:

- GNU coreutils (`sha256sum` and friends), including the binary-mode marker
  and escaped file names
- BSD tag format (`shasum --tag`, and the BSD `sha256` and `md5` tools)
- OpenSSL `dgst` output, 1.x and 3.x
- `xxhsum`, default and tagged
- SFV
- A file holding a single bare hash

When a file doesn't say which algorithm it uses, HashPony works it out from
the tag, then the file name (`SHA256SUMS`, `.sha512`, `.md5`), then the hash
length, and asks you only when that still leaves more than one choice.

Exports are GNU format (two spaces, lowercase by default, LF line endings,
coreutils escaping), BSD tag or SFV, with names like `SHA256SUMS`,
`file.sha256` or `folder.sfv` suggested for you.

## Languages

English, Deutsch, Español, Français, 日本語, 한국어, Português (Brasil),
Русский, Türkçe, Українська and 简体中文. Switch in Settings, Language. On
Android 13 and newer it's also under the system's per-app language settings.

## Permissions, exactly

The source manifest declares no permissions. After the build merges in the
AndroidX libraries, the manifest has one `uses-permission` entry,
`com.hashpony.android.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. AndroidX
Core adds it to every app that uses it. It's a signature permission
HashPony defines for itself, so no other app can hold it, and it grants
nothing outside HashPony. To check:

```
aapt dump permissions HashPony-<version>.apk
```

## Build

```
./gradlew test
./gradlew assembleRelease
```

The release build is signed only when `keystore.properties` is present at
the repo root (it's gitignored). Without it you get an unsigned APK, so
anyone can build from source.

## Verify a release

The release APK is signed with the HashPony release key. Its certificate
SHA-256 fingerprint is:

```
67d26ab7f468ca4bcbfe7fb6535330f88ca36785c9b4b5198153dfa09f4f69c7
```

Check a downloaded APK against it with the Android build tools:

```
apksigner verify --print-certs HashPony-<version>.apk
```

The printed "certificate SHA-256 digest" must match the fingerprint above.

Every GitHub release and release candidate carries five files, and
releases are immutable, so they can't be swapped after publishing:

- `HashPony-<version>.apk`, the APK
- `HashPony-<version>.apk.sha256`, its SHA-256
- `HashPony-<version>.apk.asc`, its detached OpenPGP signature
- `NorseHorse-release-key.asc`, the armored OpenPGP release key
- `HashPony-<version>-signing-keys.txt`, the OpenPGP release key
  fingerprint and the APK signing certificate SHA-256

```
sha256sum -c HashPony-<version>.apk.sha256
gpg --import NorseHorse-release-key.asc
gpg --verify HashPony-<version>.apk.asc HashPony-<version>.apk
```

Before trusting the key that ships with a release, check its fingerprint
against a source GitHub doesn't control: keys.openpgp.org, keys.pgpony.app,
or the verify page on pgpony.app. It's the same key that signs PGPony.

Commits and tags are signed with the norsehorse-dev SSH signing key:

```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIPhzdDdquHgaZq8dgEQzcncPBnSZvIVlusBg0TvOA8To
```

To check a tag or commit locally, list the key in an allowed signers file
and point git at it from inside a clone:

```
echo "norsehorse-dev@users.noreply.github.com ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIPhzdDdquHgaZq8dgEQzcncPBnSZvIVlusBg0TvOA8To" > allowed_signers
git config gpg.ssh.allowedSignersFile "$PWD/allowed_signers"
git verify-tag v<version>
git log --show-signature -1
```

The same key is registered on the norsehorse-dev GitHub account, so GitHub
marks these commits and tags Verified. The copy here lets you check them
without relying on that badge.

Rebuild from source and compare. The release gate script clones a tag twice
into clean, isolated build roots, proves the two builds are identical, and
compares them against a downloaded APK, ignoring only the signature:

```
tools/verify_repro.sh rebuild v<version> HashPony-<version>.apk
```

It needs an Android SDK and network access for the clone and the dependency
downloads, and prints the per-dex SHA-256s and a content hash. Each release's
notes carry the whole-file SHA-256 for downloaders and the content hash for
rebuilders. `SIGNING.md` has the rest.

## License

Apache License 2.0. See `LICENSE`. Third-party notices, including the
xxHash port and Bouncy Castle, are in `NOTICE`.

## Links

- Website: https://hashpony.app
- Source: https://github.com/norsehorse-dev/HashPonyAndroid
- The other Pony apps: https://pony.norsehor.se

## A note on how this is built

This app is developed solo and with heavy use of AI assistance, which I
don't hide. What matters for a tool like this is that the digests are
right. Every algorithm is tested against published reference vectors, and
the sums parser and writer are tested against files produced by coreutils,
shasum, OpenSSL and xxhsum. The full source is here to audit, and bug
reports and code review are welcome.
