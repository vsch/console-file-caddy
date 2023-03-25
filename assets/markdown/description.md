A plugin to convert console output into navigable links for absolute paths, `file://`, `fqn://`
and `diff://` URI with optional `#line:column` anchor refs to specify the line and column
location in the editor.

* Absolute paths in the form: `/...../some-file.ext:xxx` on OS X/Unix systems and
  `x:\....\some-file.ext:xxx` or `x:/..../some-file.ext:xxx` on Windows systems, will be
  converted into links.

* `fqn://` protocol followed by a language specific fully qualified class name and optional line
  and column information. This link only requires a fully qualified class name which is
  independent of the physical location of the project.

* `diff://` protocol followed by two file paths as `/somePath/...../file1.ext:xxx ?
  /someOtherPath/file2.ext:xxx &` to be opened in the IDE difference view with each file's caret
  to be located at the optional coordinates given by `xxx`, same as absolute paths. To prevent
  other console filters from hijacking the individual paths as links to single files, the file
  paths can be optionally wrapped in `[]` after the first `/`, as:
  `diff:///[somePath/...../file1.ext:xxx]?/[someOtherPath/file2.ext:xxx]&`. Spaces around `?` or
  the terminating `&` are optional.

* The plugin is liberal in recognizing the line/column suffix with `yyy` being line number and
  `xxx` column and will accept `#Lyyy`, `:yyy:xxx`, `(yyy:xxx)`, `[yyy-xxx]` and their
  permutations.

Great for navigating to file references in console application which outputs absolute file path
with line information.

`fqn://` links are great for generating links in test output, including parameterized tests,
with `assertXXX(message, expected, actual)` assertions. Adding an `fqn://` reference for the
file/line/column of the source of the test parameter data, allows to get a failed test with a
navigable link to the line of parameter data, which failed. See
[README](https://github.com/vsch/console-file-caddy/blob/master/README.md)

`diff://` links allow creating output with integrated difference links similar to test console
output in IntelliJ for test failure. It allows creating these links in test frameworks not
recognized by the IDE or where the IDE does not have the facility to easily show difference view
based on test output.

