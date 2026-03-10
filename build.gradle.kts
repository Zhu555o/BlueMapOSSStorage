plugins {
    id("java")
    alias(libs.plugins.spotless)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation("com.aliyun.oss:aliyun-sdk-oss:3.18.1")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    compileOnly(libs.bluemap.core)
    compileOnly(libs.bluemap.common)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks {
    test {
        useJUnitPlatform()
    }
    shadowJar {
        archiveClassifier.set("")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

spotless {
    java {
        importOrder()
        removeUnusedImports()
        removeWildcardImports()

        formatAnnotations()

        licenseHeaderFile(rootProject.file(".spotless/Copyright.java"))
    }
}