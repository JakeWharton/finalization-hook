@file:JvmName("Main")

package example

import com.jakewharton.finalization.withFinalizationHook
import kotlin.jvm.JvmName
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
	println("START")
	withFinalizationHook(
		hook = { println("HOOK") },
		block = {
			println("BLOCK")
			delay(30.seconds)
		},
	)
	println("END")
}
