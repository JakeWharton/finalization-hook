package com.jakewharton.finalization

import kotlin.concurrent.AtomicInt
import kotlin.concurrent.AtomicReference
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlin.coroutines.coroutineContext
import kotlin.system.exitProcess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import platform.posix.sleep

internal val signalHandlerRef = AtomicReference<((Int) -> Unit)?>(null)

internal expect fun installAllSignalHandlers()
internal expect fun restoreSignalHandlerAndTerminate(signal: Int): Boolean
internal expect fun clearAllSignalHandlers()

@OptIn(ExperimentalContracts::class)
public actual suspend fun <R> withFinalizationHook(
	hook: () -> Unit,
	block: suspend CoroutineScope.() -> R,
): R {
	contract {
		callsInPlace(block, EXACTLY_ONCE)
	}

	val job = Job(coroutineContext.job)
	val signalRef = AtomicInt(0)
	val signalHandler: (Int) -> Unit = { signal ->
		// It's possible multiple signals are received before we can clear this callback.
		// The code below only does a single read of this value, so the last received will win.
		signalRef.value = signal
		// Force the code below to jump directly into the 'finally' block.
		job.cancel()
	}
	check(signalHandlerRef.compareAndSet(null, signalHandler)) {
		"Cannot nest multiple shutdown hooks"
	}

	installAllSignalHandlers()

	return try {
		withContext(job, block)
	} finally {
		// Required so that the caller's scope can exit normally.
		job.complete()

		hook()

		val signalValue = signalRef.value
		if (signalValue != 0) {
			if (restoreSignalHandlerAndTerminate(signalValue)) {
				// Since signal handling is asynchronous, sleep in the hopes we are
				// killed before it returns.
				sleep(1U)
			}

			// If we fail to restore the default signal handler, fail to kill ourselves,
			// or fail to be terminated in a reasonable amount of time, fallback to an exit.
			exitProcess(1)
		}

		clearAllSignalHandlers()
		signalHandlerRef.value = null
	}
}
