# Signing

How HashPony's commits, tags and release APKs are signed, and how to check them.

## Commits and tags

Commits and tags are signed with the maintainer's SSH signing key, the one
registered as a signing key on the `norsehorse-dev` GitHub account. The signing
machine has this in its global git config:

```
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/norsehorse_signing.pub
git config --global commit.gpgsign true
git config --global tag.gpgsign true
```

The repo itself sets only the author identity (`norsehorse-dev`), so the same
checkout works on machines that don't hold the key.

### Commits made without the key

Work sometimes lands as commits on a machine or session that has no access to
the signing key. Those commits are unsigned. They get re-signed on the signing
machine before anything is pushed, so every commit on GitHub is signed.

Before the first push (no remote yet), re-sign the whole history:

```
git rebase --root --exec "git commit --amend --no-edit -S"
```

After that, re-sign only the commits that aren't on GitHub yet:

```
git rebase --exec "git commit --amend --no-edit -S" origin/master
```

Then check that nothing unsigned is left before pushing (`G` = good
signature; `N` means unsigned):

```
git log --format="%h %G? %s" origin/master..HEAD
```

`%G?` reports `G` only if the key is listed in `gpg.ssh.allowedSignersFile`;
without that file it shows `U` for a valid signature from an unknown key.

### Release and RC tags

Every release candidate and release gets a signed annotated tag on the commit
it was built from:

```
git tag -s v1.0.0-rc1 -m "HashPony 1.0.0-rc1"
git tag -v v1.0.0-rc1
```

F-Droid's update check follows release tags only, so RC tags never trigger a
store update.

## Release APKs

APKs are signed with the HashPony release keystore. Its path and passwords live
in `keystore.properties` at the repo root, which is gitignored; without that
file the release build is left unsigned instead of failing.

The APK that gets signed is never a working-tree build. Each release and RC
goes through `tools/verify_repro.sh rebuild <tag>`, which builds the tag twice
from clean clones and proves the two builds are identical; the signed APK is
then built inside the first clone and compared against that clean build, and
only a match gets published.

Every GitHub release and RC carries five files:

- `HashPony-<version>.apk`
- `HashPony-<version>.apk.sha256`, the SHA-256 of the APK
- `HashPony-<version>.apk.asc`, a detached, armored OpenPGP signature made
  with the NorseHorse release key (`0x53F9798E4919DE62`, the same key that
  signs PGPony releases)
- `NorseHorse-release-key.asc`, that key, armored
- `HashPony-<version>-signing-keys.txt`, the key's fingerprint and the APK
  signing certificate's SHA-256

Immutable releases are on for the repository, so none of these can be
replaced after publishing. Everything is attached to a draft, checked, and
then published.

To verify a download:

```
sha256sum -c HashPony-1.0.0.apk.sha256
gpg --import NorseHorse-release-key.asc
gpg --verify HashPony-1.0.0.apk.asc HashPony-1.0.0.apk
```

The APK signing certificate is separate from both the git SSH key and the GPG
key. Android checks it on install, and an update only installs over an
existing copy when the certificates match.
