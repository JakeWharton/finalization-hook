package com.jakewharton.finalization

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class FinalizationHookTest {
	@Test fun callsInPlaceContract() = runTest {
		var success: Boolean
		withFinalizationHook(
			hook = { },
			block = { success = true },
		)
		assertThat(success).isTrue()
	}

	@Test fun blockStartedWithoutSuspension() = runTest {
		var started = false
		backgroundScope.launch(start = UNDISPATCHED) {
			withFinalizationHook(
				hook = { },
				block = {
					started = true
					awaitCancellation()
				},
			)
		}
		assertThat(started).isTrue()
	}

	@Test fun hookRunsOnNormalCompletion() = runTest {
		var ran = false
		val value = Any()
		val result = withFinalizationHook(
			hook = { ran = true },
			block = { value },
		)
		assertThat(ran).isTrue()
		assertThat(result).isSameInstanceAs(value)
	}

	@Test fun hookRunsOnException() = runTest {
		var ran = false
		val exception = MyException()
		assertFailure {
			withFinalizationHook(
				hook = { ran = true },
				block = {
					throw exception
				},
			)
		}.isSameInstanceAs(exception)
		assertThat(ran).isTrue()
	}

	@Test fun hookRunsOnCancellation() = runTest {
		var ran = false
		val job = launch(start = UNDISPATCHED) {
			withFinalizationHook(
				hook = { ran = true },
				block = { awaitCancellation() },
			)
		}
		job.cancelAndJoin()
		assertThat(ran).isTrue()
	}

	private class MyException : RuntimeException()
}
