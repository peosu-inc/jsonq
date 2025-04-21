import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.invoke
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository


plugins {
    kotlin("jvm") version "1.9.23"
    `java-library`
    `maven-publish`
    signing
}

group = "com.africapoa.fn"
version = "0.0.2"
val  artifactId = "util"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))

    // Google Cloud
    implementation(platform("com.google.cloud:libraries-bom:26.50.0"))
    implementation("com.google.cloud:google-cloud-storage")
    implementation("com.google.auth:google-auth-library-oauth2-http")

    // Google API Services
    implementation("com.google.apis:google-api-services-sheets:v4-rev612-1.25.0")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20230707-2.0.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("io.mockk:mockk:1.13.5")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // Coordinates
            groupId = project.group.toString()
            version = project.version.toString()
            artifactId = artifactId

            // Artifacts
            from(components["java"])

            // POM
            pom {
                name.set("util")
                description.set("Sample application")
                url.set("https://github.com/nitusima/fn-utils")
                inceptionYear.set("2021")


                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://spdx.org/licenses/Apache-2.0.html")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("nitusima")
                        name.set("Nitu")
                        email.set("nitu@africapoa.com")
                        url.set("https://github.com/nitusima")
                    }
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/nitusima/fn-utils/issues")
                }

                scm {
                    connection.set("scm:git:git://github.com/nitusima/fn-utils.git")
                    developerConnection.set("scm:git:ssh://github.com/nitusima/fn-utils.git")
                    url.set("https://github.com/nitusima/fn-utils")
                }
            }
        }
    }

    repositories {
        // Local staging directory for manual upload
        maven {
            name = "staging"
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
        // Optional: publish to local Maven repo
//        mavenLocal()
    }
}

signing {
    val keyFile=findProperty("signing.keyFile") as String
    val password=findProperty("signing.password") as String
    val key =  layout.projectDirectory.file(keyFile).asFile.readText(Charsets.UTF_8)

    useInMemoryPgpKeys(key, password)
    sign(publishing.publications["maven"])
}


tasks.register<Zip>("bundleForMavenCentral") {
    group = "distribution"
    description = "Bundles published artifacts into a ZIP for Maven Central."

    // 1. Unwrap the staging dir provider
    val publicationDir = layout.buildDirectory.dir("staging-deploy")

    // 2. Declare inputs up front (configuration phase)
    from(publicationDir.map { it.asFile })

    // 3. Include empty dirs if needed
    includeEmptyDirs = true

    // 4. Name and destination
    val ver = project.version.toString()
    println("project artificat is $artifactId")
    println("project version is $ver")
    archiveBaseName.set("function-$artifactId")
    destinationDirectory.set(layout.buildDirectory.dir("maven-central-bundle"))

    // 5. Ensure publish runs first
    dependsOn(tasks.withType<PublishToMavenRepository>())


    // 6. Always rebuild (optional)
    outputs.upToDateWhen { false }
}
