# HashPony release ProGuard/R8 rules.
# BouncyCastle keeps reflective references to some algorithm classes; keep the
# digest classes we actually use so R8 does not strip them.
-keep class org.bouncycastle.crypto.digests.** { *; }
-dontwarn org.bouncycastle.**
