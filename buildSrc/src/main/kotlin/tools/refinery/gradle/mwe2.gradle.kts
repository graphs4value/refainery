/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.gradle

import org.gradle.accessors.dm.LibrariesForLibs

plugins {
	id("tools.refinery.gradle.java-conventions")
}

val mwe2: Configuration by configurations.creating {
	isCanBeConsumed = false
	isCanBeResolved = true
	extendsFrom(configurations.implementation.get())
	// Exclude also here, because the name of this configuration doesn't end with {@code Classpath},
	// so it isn't caught by {@code tools.refinery.gradle.java-conventions}.
	exclude(group = "log4j", module = "log4j")
	exclude(group = "ch.qos.reload4j", module = "reload4j")
}

val libs = the<LibrariesForLibs>()

dependencies {
	mwe2(enforcedPlatform(project(":refinery-bom-dependencies")))
	mwe2(libs.mwe2.launch)
	mwe2(libs.slf4j.log4j)
	mwe2(libs.slf4j.simple)
}

eclipse.classpath.plusConfigurations += mwe2
