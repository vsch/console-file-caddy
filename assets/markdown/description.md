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
the source for the test parameter data then a failed test will have a navigable link. See
[README](../../README.md)
