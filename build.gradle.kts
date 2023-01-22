plugins {
    kotlin("jvm") version "1.7.10"
    id("io.papermc.paperweight.userdev") version "1.3.8"
}

group = "me.dolphin2410"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("io.github.monun:tap-api:4.8.0")
    compileOnly("io.github.monun:kommand-api:3.0.0")
    compileOnly("io.github.monun:heartbeat-coroutines:0.0.4")
    compileOnly("io.github.dolphin2410:mcphysics-core:0.1.2")
    compileOnly("io.github.dolphin2410:mcphysics-tap:0.1.2")
    paperDevBundle("1.19.2-R0.1-SNAPSHOT")
}

tasks {
    jar {
        archiveFileName.set("gomtory.jar")
    }
}