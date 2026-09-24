import de.siphalor.jcyo.gradle.JcyoTask

plugins {
	java
	`maven-publish`
	alias(libs.plugins.jcyo)
	alias(mcLibs.plugins.smcmtk)
	alias(mcLibs.plugins.fabric.loom)
}

val minecraftVersionDescriptor = project.properties["minecraft.version.descriptor"] as String

group = "de.siphalor.${project.name}"
val archivesBaseName = "${project.name}-mc${minecraftVersionDescriptor}"
val shortVersion = "${properties["version"]}"
version = "${shortVersion}+mc${mcLibs.versions.minecraft.get()}"

val extraSources = smcmtk.mcProps.getting("extra_sources").orNull?.split(",")?.map { file("src/${it.trim()}") } ?: listOf()
val mergedAccessWidenerDir = project.layout.buildDirectory.dir("merged-accesswidener")
val mergedAccessWidenerName = "coat.accesswidener"
val mergedAccessWidenerFile = mergedAccessWidenerDir.map { it.file(mergedAccessWidenerName) }

sourceSets.main {
	extraSources.forEach {
		java.srcDir(it.resolve("java"))
		resources.srcDir(it.resolve("resources"))
	}
	resources.srcDir(mergedAccessWidenerDir)
}
val testmodSourceSet = sourceSets.register("testmod") {
	compileClasspath += sourceSets.main.get().compileClasspath
	runtimeClasspath += sourceSets.main.get().runtimeClasspath
}

fun generateMergedAccessWidener() {
	val wideners = extraSources.flatMap { it.listFiles { _, name -> name.endsWith(".accesswidener") }.orEmpty().toList() }
	mergedAccessWidenerDir.get().asFile.mkdirs()
	val merged = mergedAccessWidenerDir.get().file(mergedAccessWidenerName).asFile
	merged.createNewFile()
	val writer = merged.writer()
	writer.write("accessWidener v1 named\n")
	wideners.forEach {
		it.bufferedReader().let { reader ->
			reader.readLine()
			writer.write(reader.readText())
			reader.close()
		}
	}
	writer.close()
}
generateMergedAccessWidener()
tasks.processResources {
	doFirst {
		generateMergedAccessWidener()
	}
}

tasks.validateAccessWidener {
	enabled = false
}

smcmtk {
	useMojangMappings()
	useAccessWidener(mergedAccessWidenerFile.get())
	createModConfigurations(listOf(sourceSets.main.get(), testmodSourceSet.get()))
}

loom {
	runs {
		create("testmodClient") {
			client()
			name("Testmod Client")
			source(sourceSets.getByName("testmod"))
		}
	}
}

repositories {
	maven {
		name = "Siphalor"
		url = uri("https://maven.siphalor.de")
		mavenContent {
			includeGroupAndSubgroups("de.siphalor")
		}
	}
	mavenLocal()
}

configurations {
	val mcMajorVersion = smcmtk.mcProps.getting("minecraft.version.major").get()
	apiElements {
		outgoing.capability("${project.group}:$archivesBaseName:$shortVersion")
		outgoing.capability("de.siphalor:coat-$mcMajorVersion:$shortVersion")
	}
	runtimeElements {
		outgoing.capability("${project.group}:$archivesBaseName:$shortVersion")
		outgoing.capability("de.siphalor:coat-$mcMajorVersion:$shortVersion")
	}
}

dependencies {
	annotationProcessor(libs.lombok)
	compileOnly(libs.lombok)

	minecraft(mcLibs.minecraft)

	"modImplementation"(libs.fabric.loader)

	"modTestmodImplementation"(mcLibs.amecs.priorityKeyMappings)

	"modTestmodImplementation"(fabricApi.module("fabric-api-base", mcLibs.versions.fabric.api.get()))
	"modTestmodImplementation"(fabricApi.module(smcmtk.mcProps.getting("fabric.api.key_mapping_module").get(), mcLibs.versions.fabric.api.get()))
	"modTestmodImplementation"(fabricApi.module("fabric-resource-loader-v0", mcLibs.versions.fabric.api.get()))

	"testmodImplementation"(sourceSets.main.map { it.output })
}

tasks.processResources {
	inputs.property("version", project.version)
	val mixins = sourceSets.main.get().resources.srcDirs
		.flatMap { it.listFiles { f -> f.name.endsWith("mixins.json") }.orEmpty().toList() }
		.joinToString(",") { "\"${it.name}\"" }
	inputs.property("extraMixins", mixins)

	from(sourceSets.main.get().resources.srcDirs) {
		include("fabric.mod.json")
		expand(
			"version" to project.version,
			"mixins" to mixins
		)
		duplicatesStrategy = DuplicatesStrategy.INCLUDE
	}
}

java {
	sourceCompatibility = JavaVersion.toVersion(mcLibs.versions.java.get())
	targetCompatibility = JavaVersion.toVersion(mcLibs.versions.java.get())
}

val jcyo = registerJcyoTask("jcyo", "src/main/java")
val renderStateHelpersJcyo = registerJcyoTask("renderStateHelpersJcyo", "src/render-state-helpers/java")
val customCursorsJcyo = registerJcyoTask("customCursorsJcyo", "src/custom-cursors/java")
val testmodJcyo = registerJcyoTask("testmodJcyo", "src/testmod/java")
fun registerJcyoTask(name: String, input: String): TaskProvider<JcyoTask> {
	return tasks.register<JcyoTask>(name) {
		inputDirectory = file(input)
		variables = smcmtk.mcProps.map { props ->
			props.filter { it.key.startsWith("preprocessor.") }
				.map { it.key.substring("preprocessor.".length) to it.value}
				.toMap()
		}
	}
}

tasks.compileJava {
	dependsOn(jcyo, renderStateHelpersJcyo, customCursorsJcyo)
}
tasks.named("compileTestmodJava") {
	dependsOn(testmodJcyo)
}

tasks.jar {
	from(file("LICENSE"))
}


publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = archivesBaseName
			version = shortVersion

			from(components["java"])
		}
	}

	repositories {
		if (project.hasProperty("siphalor.maven.user")) {
			maven {
				name = "Siphalor"
				url = uri("https://maven.siphalor.de/upload.php")
				credentials {
					username = project.property("siphalor.maven.user") as String
					password = project.property("siphalor.maven.password") as String
				}
			}
		}
	}
}

