## Console File Caddy

[Get From Marketplace](https://plugins.jetbrains.com/embeddable/install/21513)

A JetBrains IDE to convert console output into navigable links for absolute paths, `file://`,
`fqn://` and `diff://` URIs, with optional `#line:column` references to specify the line and
column location in the editor.

* Absolute paths in the form: `/...../some-file.ext:xxx` on OS X/Unix systems and
  `x:\....\some-file.ext:xxx` or `x:/..../some-file.ext:xxx` on Windows systems, where `xxx` is
  line/column information, will be converted into links.

* `fqn://` protocol followed by a language specific fully qualified class name and optional line
  and column information. This link only requires a fully qualified class name which is
  independent of the physical location of the project.

* `diff://` protocol followed by two file paths as `/somePath/...../file1.ext:xxx ?
  /someOtherPath/file2.ext:xxx &` to be opened in the IDE difference view with each file's caret
  to be located at the optional coordinates given by `xxx`. To prevent other console filters
  from hijacking the individual paths as links to single files, the file paths can be optionally
  wrapped in `[]` after the first `/`, as:
  `diff:///[somePath/...../file1.ext:xxx]?/[someOtherPath/file2.ext:xxx]&`. Spaces around `?`
  and the terminating `&` are optional.

The plugin is liberal in recognizing the line/column suffix with `yyy` being line number and
`xxx` column and will accept `#Lyyy`, `:yyy:xxx`, `(yyy:xxx)`, `[yyy-xxx]` and their
permutations.

`diff://` links allow creating output with integrated difference links similar to test console
output in IntelliJ for test failure. It allows creating these links in test frameworks not
recognized by the IDE or where the IDE does not have the facility to easily show difference view
for files.

`fqn://` links are for generating links in test output with only the class name to specify the
file location. This includes parameterized tests with `assertXXX(message, expected, actual)`
assertions. Adding an `fqn://` reference for the file/line/column of the source of the test
parameter data to create a navigable link to the line of the failed parameter data, instead of
the test source.

Parametrized tests are a real pain to maintain because finding the location of the data for a
failed test can be an exercise in frustration. The IDE test console provides a convenient link
to the source of the failed test but not the location of the parameterized data used for the
test.

Using code similar to
[SampleCustomTest.java](src/test/com/vladsch/plugins/consolefilecaddy/SampleCustomizedTest.java)
in parameterized tests to generate a link in the message for failed tests, takes the pain out by
linking to the data location, without a long search or guesswork.

A simpler one line version, without the hassles of computing column information will navigate to
the start of line for the parameterized data definition
[SampleTest.java](src/test/com/vladsch/plugins/consolefilecaddy/SampleTest.java).

Running these tests for this project in IntelliJ will result in a failed test output with a
navigable link to the errant test parameter data row:

![ScreenShot_TestResults](assets/images/ScreenShot_TestResults.png)

Navigation takes you right to the location of the data:

![ScreenShot_NavigatedLink](assets/images/ScreenShot_NavigatedLink.png)

---

Copyright 2000-2019 JetBrains s.r.o.  
Copyright 2019-2023 Vladimir Schneider

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
in compliance with the License. You may obtain a copy of the License at

<https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software distributed under the
License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
express or implied. See the License for the specific language governing permissions and
limitations under the License.

