## Console File Caddy

A JetBrains IDE plugin which  
Converts absolute file paths with optional line/column information in the console output to
navigable links to files.

Paths in the form: `/...../some-file.ext:xxx` on OS X/Unix systems and
`x:\....\some-file.ext:xxx` or `x:/..../some-file.ext:xxx` on Windows systems will be converted.

The plugin is liberal in recognizing the suffix with `yyy` being line number and `xxx` column
and will accept `#Lyyy`, `:yyy:xxx`, `(yyy:xxx)`, `[yyy-xxx]` and their permutations.

Useful for any console application which outputs absolute file path and line information as part
of its operation. For example Scala sbt will output warnings with file/line information which
this plugin will convert to navigable link right to the file/line.

It will also recognize `fqn://` protocol followed by a language specific fully qualified class
name and optional line and column information. This link only requires a fully qualified class
name which is independent of the physical location of the project.

This makes it useful for generating links from tests including parameterized tests with
`assertXXX(message, expected, actual)` assertions. If message contains the file/line/column of
the source for the test parameter data then a failed test will have a navigable link to the test
parameter data location.

Parametrized tests are a PITA to maintain because finding the location of the data for a failed
test is an exercise in frustration. The IDE test console provides a convenient link to the
source of the failed test but not the location of the parameterized data used for the test.

I use something like
[SampleCustomTest.java](test/com/vladsch/plugins/consoleFileCaddy/SampleCustomizedTest.java) in
my parameterized tests to generate a link in the message for failed tests. The link takes me
right to the data location, no long search or guesswork.

A simpler version one line version, without the hassles of computing column information will
navigate to the start of line for the parameterized data definition
[SampleTest.java](test/com/vladsch/plugins/consoleFileCaddy/SampleTest.java).

Running these tests for this project in IntelliJ will result in a failed test output with a
navigable link to the errant test parameter row:

![ScreenShot_TestResults](assets/images/ScreenShot_TestResults.png)
