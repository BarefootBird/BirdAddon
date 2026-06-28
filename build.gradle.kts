import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("fabric-loom")
    kotlin("jvm")
    `maven-publish`
}

group = property("maven_group")!!
version = property("mod_version")!!

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    exclusiveContent {
        forRepository {
            ivy("https://github.com/odtheking/Odin/releases/download/") {
                patternLayout {
                    artifact("[revision]/Odin-[revision].[ext]")
                }
                metadataSources {
                    artifact()
                }
            }
        }
        filter {
            includeGroup("com.odtheking")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings(loom.layered {
        mappings(file("mappings/identity.tiny"))
    })
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:${property("devauth_version")}")
    modImplementation("com.odtheking:Odin:${property("odin_version")}")

    modImplementation("com.github.stivais:Commodore:${property("commodore_version")}")

    property("minecraft_lwjgl_version").let { lwjglVersion ->
        modImplementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion")

        listOf("windows", "linux", "macos", "macos-arm64").forEach { os ->
            modImplementation("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-$os")
        }
    }
}

loom {
    noIntermediateMappings()

    runConfigs.named("client") {
        isIdeConfigGenerated = true
        vmArgs.addAll(
            arrayOf(
                "-Dmixin.debug.export=true",
                "-Ddevauth.enabled=true",
                "-Ddevauth.account=main",
                "-XX:+AllowEnhancedClassRedefinition"
            )
        )
    }

    runConfigs.named("server") {
        isIdeConfigGenerated = false
    }
}

afterEvaluate {
    loom.runs.named("client") {
        vmArg("-javaagent:${configurations.compileClasspath.get().find { it.name.contains("sponge-mixin") }}")
    }
}

tasks {
    remapJar {
        archiveFileName.set(
            "${project.name}-${version}-${project.property("minecraft_version")}.jar"
        )
    }

    remapSourcesJar {
        enabled = false
    }

    processResources {
        filesMatching("fabric.mod.json") {
            expand(getProperties())
        }
    }

    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_25
            freeCompilerArgs.add("-Xlambdas=class") //Commodore
        }
    }

    compileJava {
        sourceCompatibility = "25"
        targetCompatibility = "25"
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 26
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}
