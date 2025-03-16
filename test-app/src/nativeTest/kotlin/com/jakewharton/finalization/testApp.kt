package com.jakewharton.finalization

import kotlin.experimental.ExperimentalNativeApi

const val testAppJvmPath = "build/install/test-app/bin/test-app"

@OptIn(ExperimentalNativeApi::class)
val testAppNativePath: String = buildString {
	append("build/bin/")
	append(
		when (val os = Platform.osFamily) {
			OsFamily.MACOSX -> "macos"
			OsFamily.LINUX -> "linux"
			OsFamily.WINDOWS -> "mingw"
			else -> throw IllegalStateException("Unsupported OS: $os")
		},
	)
	append(
		when (val cpu = Platform.cpuArchitecture) {
			CpuArchitecture.ARM64 -> "Arm64"
			CpuArchitecture.X64 -> "X64"
			else -> throw IllegalStateException("Unsupported CPU: $cpu")
		},
	)
	append("/releaseExecutable/test-app")
	append(
		when (Platform.osFamily) {
			OsFamily.WINDOWS -> ".exe"
			else -> ".kexe"
		},
	)
}
