## Console File Caddy

A JetBrains IDE plugin which adds a console `UrlFilter` to recognize absolute file paths with or
without the `file://` protocol prefix and with optional Line/Column Position suffix and convert
them to navigable links.

Paths in the form: `/...../some-file.ext:xxx` for OS X/Unix systems and
`x:\....\some-file.ext:xxx` or `x:/..../some-file.ext:xxx` for Windows systems, where either `/`
and `\` will be recognized.

The plugin is liberal in recognizing the suffix with `xxx` being line number and `yyy` column on
the line and will accept `#Lxxx`, `:xxx:yyy`, `(xxx:yyy)`, `[xxx-yyy]` and their permutations.

Useful for any console application which outputs absolute file path and line information as part
of its operation. For example Scala sbt will output warnings with file/line information which
this plugin will convert to navigable link right to the file/line.

It can also be used for parameterized tests with the `assertXXX(message, expected, actual)` test
assertions where the message contains the file/line of the source of the parameters for the
tests which are a PITA to find when they fail because the IDE provides the link for the test
source but the issue is not the test but the parameters used for the test.

I use something like
[SampleTest.java](test/com/vladsch/plugins/consoleFileCaddy/SampleTest.java) in my tests to
generate my test parameter data.

Running the above test results in a failed test output with a navigable link to the errant
parameter row:

![ScreenShot_TestResults](assets/images/ScreenShot_TestResults.png)
