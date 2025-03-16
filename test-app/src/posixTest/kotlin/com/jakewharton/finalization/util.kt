package com.jakewharton.finalization

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import platform.posix.SIGKILL
import platform.posix.SIG_DFL
import platform.posix._exit
import platform.posix.close
import platform.posix.dup2
import platform.posix.errno
import platform.posix.execl
import platform.posix.fork
import platform.posix.kill
import platform.posix.perror
import platform.posix.pipe
import platform.posix.signal

@OptIn(ExperimentalForeignApi::class)
private fun signalHandler(signal: Int) {
	// Ignore, and restore original handler.
	signal(signal, SIG_DFL)
}

@OptIn(ExperimentalForeignApi::class)
fun killChild(pid: Int, signal: Int) {
	// Killing a child process will propagate the signal upward to us, the parent. In order to not
	// also terminate, we install a signal handler that does nothing except remove itself.
	signal(signal, staticCFunction(::signalHandler))
	kill(pid, signal)
}

@OptIn(ExperimentalForeignApi::class)
fun startChildProcess(command: String): Process {
	val stdin = IntArray(2)
	stdin.usePinned { pipe(it.addressOf(0)) }
	val stdout = IntArray(2)
	stdout.usePinned { pipe(it.addressOf(0)) }

	val pid = fork()
	if (pid < 0) {
		error(errno)
	}

	if (pid == 0) {
		// Child process
		close(stdin[1])
		close(stdout[0])
		dup2(stdin[0], 0)
		dup2(stdout[1], 1)

		execl("/bin/sh", "sh", "-c", command, null)
		perror("execl")
		_exit(1)
	}

	// Parent process
	close(stdin[0])
	close(stdout[1])

	return Process(
		pid = pid,
		stdin = stdin[1],
		stdout = stdout[0],
	)
}

data class Process(
	val pid: Int,
	val stdin: Int,
	val stdout: Int,
) : AutoCloseable {
	override fun close() {
		close(stdin)
		close(stdout)
		kill(pid, SIGKILL)
	}
}
