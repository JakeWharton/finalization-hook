@file:JvmName("Hook")

package com.jakewharton.finalization

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

@OptIn(ExperimentalContracts::class)
public actual suspend fun <R> withFinalizationHook(
	hook: () -> Unit,
	block: suspend CoroutineScope.() -> R,
): R {
	contract {
		callsInPlace(block, EXACTLY_ONCE)
	}

	val tryRunHook = object : AtomicBoolean(false), Runnable {
		override fun run() {
			if (compareAndSet(false, true)) {
				hook()
			}
		}
	}

	val runtime = Runtime.getRuntime()
	val hookThread = Thread(tryRunHook)
	runtime.addShutdownHook(hookThread)

	return try {
		coroutineScope(block)
	} finally {
		tryRunHook.run()
		runtime.removeShutdownHook(hookThread)
	}
}
