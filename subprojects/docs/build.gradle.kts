/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import org.siouan.frontendgradleplugin.infrastructure.gradle.RunYarn
import tools.refinery.gradle.JavaLibraryPlugin
import tools.refinery.gradle.utils.SonarPropertiesUtils

plugins {
	id("tools.refinery.gradle.frontend-workspace")
	id("tools.refinery.gradle.sonarqube")
}

frontend {
	assembleScript.set("run build")
}

val javadocs: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
}

dependencies {
	gradle.projectsEvaluated {
		for (subproject in rootProject.subprojects) {
			if (subproject.plugins.hasPlugin(JavaLibraryPlugin::class)) {
				javadocs(project(subproject.path, "javadocElements"))
			}
		}
	}

	javadocs(project(":refinery-gradle-plugins", "javadocElements"))
}

val srcDir = "src"

val docusaurusOutputDir = layout.buildDirectory.dir("docusaurus")

val javadocsDir = layout.buildDirectory.dir("javadocs")

val javadocsDocsDir = javadocsDir.map { root -> root.dir("develop/javadoc") }

val configFiles: FileCollection = files(
	rootProject.file("yarn.lock"),
	rootProject.file("package.json"),
	"package.json",
	rootProject.file("tsconfig.base.json"),
	"tsconfig.json",
	"babel.config.config.ts",
	"docusaurus.config.ts",
)

val lintConfigFiles: FileCollection = configFiles + files(
	rootProject.file(".eslintrc.cjs"), rootProject.file("prettier.config.cjs")
)

tasks {
	val extractJavadocs by registering {
		dependsOn(javadocs)
		outputs.dir(javadocsDir)
		doFirst {
			delete(javadocsDir)
		}
		doLast {
			javadocs.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
				copy {
					from(zipTree(artifact.file))
					into(javadocsDocsDir.map { root -> root.dir(artifact.moduleVersion.id.name) })
				}
			}
		}
	}

	assembleFrontend {
		dependsOn(extractJavadocs)
		inputs.dir(srcDir)
		inputs.dir("static")
		inputs.dir(javadocsDir)
		inputs.files(configFiles)
		outputs.dir(docusaurusOutputDir)
	}

	val typeCheckFrontend by registering(RunYarn::class) {
		dependsOn(installFrontend)
		inputs.dir(srcDir)
		inputs.files(configFiles)
		outputs.dir(layout.buildDirectory.dir("typescript"))
		script.set("run typecheck")
		group = "verification"
		description = "Check for TypeScript type errors."
	}

	val lintFrontend by registering(RunYarn::class) {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.dir(srcDir)
		inputs.files(lintConfigFiles)
		outputs.file(layout.buildDirectory.file("eslint.json"))
		script.set("run lint")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	register<RunYarn>("fixFrontend") {
		dependsOn(installFrontend)
		dependsOn(typeCheckFrontend)
		inputs.dir(srcDir)
		inputs.files(lintConfigFiles)
		script.set("run lint:fix")
		group = "verification"
		description = "Check for TypeScript lint errors and warnings."
	}

	check {
		dependsOn(typeCheckFrontend)
		dependsOn(lintFrontend)
	}

	clean {
		delete(".docusaurus")
		delete(".yarn")
	}

	val siteZip by registering(Zip::class) {
		dependsOn(assembleFrontend)
		from(docusaurusOutputDir)
		archiveFileName = "refinery-docs.zip"
		destinationDirectory = layout.buildDirectory
	}

	assemble {
		dependsOn(siteZip)
	}
}

sonarqube.properties {
	SonarPropertiesUtils.addToList(properties, "sonar.sources", srcDir)
	property("sonar.nodejs.executable", "${frontend.nodeInstallDirectory.get()}/bin/node")
	property("sonar.eslint.reportPaths", "${layout.buildDirectory.get()}/eslint.json")
}
