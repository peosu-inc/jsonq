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

group = "com.peosu"
version = "0.0.3"
val  artifactId = "fn-utils"

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

    // YAML support
    implementation("org.yaml:snakeyaml:2.2")

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
                name.set("fn-utils")
                description.set("Functional utilities for Java/Kotlin including JsonQ for JSON/YAML querying")
                url.set("https://github.com/peosu-inc/fn-utils")
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
                        id.set("peosu")
                        name.set("Peosu")
                        email.set("dev@peosu.com")
                        url.set("https://github.com/peosu")
                    }
                }

                issueManagement {
                    system.set("GitHub")
                    url.set("https://github.com/peosu-inc/fn-utils/issues")
                }

                scm {
                    connection.set("scm:git:git://github.com/peosu-inc/fn-utils.git")
                    developerConnection.set("scm:git:ssh://github.com/peosu-inc/fn-utils.git")
                    url.set("https://github.com/peosu-inc/fn-utils")
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

// Task to publish bundle to Maven Central Portal via REST API
tasks.register("publishToCentralPortal") {
    group = "publishing"
    description = "Uploads the bundle to Maven Central Portal (central.sonatype.com)"
    dependsOn("bundleForMavenCentral")

    doLast {
        val bundleFile = layout.buildDirectory.file("maven-central-bundle/function-$artifactId-${project.version}.zip").get().asFile

        if (!bundleFile.exists()) {
            throw GradleException("Bundle file not found: ${bundleFile.absolutePath}")
        }

        val username = findProperty("centralPortal.username") as String?
            ?: System.getenv("CENTRAL_PORTAL_USERNAME")
            ?: throw GradleException("Central Portal username not configured. Set centralPortal.username in gradle.properties or CENTRAL_PORTAL_USERNAME env var")

        val token = findProperty("centralPortal.token") as String?
            ?: System.getenv("CENTRAL_PORTAL_TOKEN")
            ?: throw GradleException("Central Portal token not configured. Set centralPortal.token in gradle.properties or CENTRAL_PORTAL_TOKEN env var")

        val publishingType = findProperty("centralPortal.publishingType") as String? ?: "USER_MANAGED"

        println("Uploading ${bundleFile.name} to Maven Central Portal...")
        println("Bundle size: ${bundleFile.length() / 1024} KB")

        val process = ProcessBuilder(
            "curl", "-X", "POST",
            "https://central.sonatype.com/api/v1/publisher/upload",
            "-u", "$username:$token",
            "-F", "bundle=@${bundleFile.absolutePath}",
            "-F", "publishingType=$publishingType",
            "-H", "Accept: text/plain",
            "-w", "\nHTTP Status: %{http_code}\n"
        ).redirectErrorStream(true).start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        println(output)

        if (exitCode != 0 || output.contains("HTTP Status: 4") || output.contains("HTTP Status: 5")) {
            throw GradleException("Failed to upload bundle to Central Portal")
        }

        println("Bundle uploaded successfully!")
        println("Check status at: https://central.sonatype.com/publishing/deployments")
    }
}
