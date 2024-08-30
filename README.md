# Finalization Hook for Kotlin

Run a "hook" lambda after a block of code regardless of whether the block
succeeds, fails, or the application is killed.

```kotlin
changeSomething()
withFinalizationHook(
  hook = { resetSomething() },
  block = {
    doWork()
  },
)
```

This function should only be used when the changes being made are outside the application
itself. An example would be writing a lock file to the file system, and then deleting it when
an operation completes. If you are only changing state within the application, a regular
`try`/`finally` will suffice.


## Test App

Build the `:test-app` project to see this in action. Run the app and then hit
<kbd>Ctrl</kbd>+<kbd>C</kbd> or `kill` the PID.

```
$ ./test-app/build/bin/macosArm64/releaseExecutable/test-app.kexe
START
BLOCK
^CHOOK
```

```
$ ./test-app/build/bin/macosArm64/releaseExecutable/test-app.kexe
START
BLOCK
HOOK
Terminated: 15
```
In another terminal pane:
```
$ ps | grep test-app
32769 ttys004    0:00.02 ./test-app/build/bin/macosArm64/releaseExecutable/test-app.kexe
32943 ttys005    0:00.00 grep test-app

$ kill 32769
```

A JVM version is also available at `test-app/build/install/test-app/bin/test-app`.


## Download

```groovy
dependencies {
  implementation("com.jakewharton.finalization:finalization-hook:0.1.0")
}
```

Documentation is available at [jakewharton.github.io/finalization-hook/docs/0.x/](https://jakewharton.github.io/finalization-hook/docs/0.x/).

<details>
<summary>Snapshots of the development version are available in Sonatype's snapshots repository.</summary>
<p>

```groovy
repository {
  mavenCentral()
  maven {
    url 'https://oss.sonatype.org/content/repositories/snapshots/'
  }
}
dependencies {
  implementation("com.jakewharton.finalization:finalization-hook:0.2.0-SNAPSHOT")
}
```

Snapshot documentation is available at [jakewharton.github.io/finalization-hook/docs/latest/](https://jakewharton.github.io/finalization-hook/docs/latest/).

</p>
</details>

## License

    Copyright 2024 Jake Wharton

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
