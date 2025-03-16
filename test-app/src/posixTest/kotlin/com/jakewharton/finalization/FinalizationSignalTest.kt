package com.jakewharton.finalization

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.UNLIMITED
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import platform.posix.SIGINT
import platform.posix.read

@OptIn(ExperimentalForeignApi::class)
class FinalizationSignalTest {
	@BeforeTest fun before() {
		// TODO Check if our ppid corresponds to a JDK. If so, we're probably being run in IntelliJ
		//  or Gradle and the
	}

	@Test fun jvm() = runTest {
		runTest(testAppJvmPath, SIGINT)
	}

	@Test fun native() = runTest {
		runTest(testAppNativePath, SIGINT)
	}

	private suspend fun TestScope.runTest(path: String, signal: Int) {
		startChildProcess(path).use { p ->
			val lines = Channel<String>(UNLIMITED)

			backgroundScope.launch(Dispatchers.IO) {
				val buf = ByteArray(100)
				buf.usePinned {
					while (isActive) {
						val read = read(p.stdout, it.addressOf(0), buf.size.convert()).toInt()
						if (read == -1) continue
						lines.trySend(buf.toKString(endIndex = read).trimEnd('\n'))
					}
				}
			}

			assertThat(lines.receive()).isEqualTo("START")
			assertThat(lines.receive()).isEqualTo("BLOCK")
			assertThat(lines.tryReceive()).prop(ChannelResult<String>::isFailure).isTrue()

			killChild(p.pid, signal)
			assertThat(lines.receive()).isEqualTo("HOOK")
		}
	}
}
