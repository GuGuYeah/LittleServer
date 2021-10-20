import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date
import java.text.SimpleDateFormat

val gitTagName: String? get() = Regex("(?<=refs/tags/).*").find(System.getenv("GITHUB_REF") ?: "")?.value
val gitCommitSha: String? get() = System.getenv("GITHUB_SHA") ?: null
val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z").format(Date()) as String
val versionText: String get() = System.getenv("DBG_VERSION") ?: "0.0.0"

plugins {
    kotlin("jvm") version "1.5.10"
    application
}

val mainClassPath = "LittleServerMain"
val author = "Asforest"
val website = "https://github.com/updater-for-minecraft/LittleServer"

group = "com.github.updater-for-minecraft"
version = gitTagName ?: versionText

repositories {
    maven("https://maven.aliyun.com/nexus/content/groups/public/")
    mavenCentral()
}

dependencies {
//    implementation("org.yaml:snakeyaml:1.29")
    implementation("net.java.dev.jna:jna:5.9.0")
    implementation("com.esotericsoftware.yamlbeans:yamlbeans:1.15")
    implementation("org.nanohttpd:nanohttpd:2.2.0")
    implementation("org.nanohttpd:nanohttpd-webserver:2.2.0")
    // implementation(fileTree("libs") {include("*.jar")})

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set(mainClassPath)
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    // 添加Manifest
    manifest {
        attributes("Main-Class" to mainClassPath)
        attributes("Application-Version" to archiveVersion.get())
        attributes("Author" to author)
        attributes("Website" to website)
        attributes("Git-Commit" to (gitCommitSha ?: ""))
        attributes("Compile-Time" to timestamp)
        attributes("Compile-Time-Ms" to System.currentTimeMillis())
    }

    // 复制依赖库
    from(configurations.runtimeClasspath.get().map {
        println("- "+it.name)
        if (it.isDirectory) it else zipTree(it)//.matching { exclude("*") }
    })

    // 打包assets目录里的文件
    from("assets")

    // 打包源代码
    sourceSets.main.get().allSource.sourceDirectories.map {
        if(it.name != "resources")
            from(it) {into("sources/"+it.name) }
    }

    // 设置输出路径
    destinationDirectory.set(File(project.buildDir, "production"))
}