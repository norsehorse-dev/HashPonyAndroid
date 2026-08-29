plugins {
    alias(libs.plugins.kotlin.jvm)
}

// :core is pure Kotlin/JVM — no Android imports. It holds the streaming
// multi-digest engine, the algorithm registry, the vendored xxHash port, and
// the hex/normalisation helpers, and is fully unit-testable on the JVM. It is
// also the part an eventual iOS twin would re-spec from.

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        // xxHash leans on ULong/UInt/ULongArray; opt in once for the module.
        freeCompilerArgs.add("-opt-in=kotlin.ExperimentalUnsignedTypes")
    }
}

dependencies {
    // One BouncyCastle dependency covers every cryptographic digest outside the
    // SHA-2 family. Confirm this coordinate/version matches PGPonyAndroid.
    api(libs.bouncycastle.prov)

    testImplementation(libs.junit)
}

tasks.withType<Test> {
    // The vector suites load fixtures from src/test/resources.
    useJUnit()
}
