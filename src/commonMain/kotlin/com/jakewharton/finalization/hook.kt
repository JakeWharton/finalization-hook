package com.jakewharton.finalization

import kotlinx.coroutines.CoroutineScope

/**
 * Run [block], and then run [hook] regardless of whether the block
 * succeeds, fails, or the application is killed.
 *
 * [hook] is not guaranteed to run, but every effort will be made to have it run.
 *
 * The general pattern for using this function is:
 * ```kotlin
 * changeSomething()
 * withFinalizationHook(
 *   hook = { resetSomething() },
 *   block = {
 *     doWork()
 *   },
 * )
 * ```
 *
 * If a signal is received while [block] is executing, its coroutine will be cancelled, the [hook]
 * will be run, and then the signal will be redelivered to the application to terminate execution.
 *
 * This function should only be used when the changes being made are outside the application
 * itself. An example would be writing a lock file to the file system, and then deleting it when
 * an operation completes. If you are only changing state within the application, a regular
 * `try`/`finally` will suffice.
 */
public expect suspend fun <R> withFinalizationHook(
	hook: () -> Unit,
	block: suspend CoroutineScope.() -> R,
): R
