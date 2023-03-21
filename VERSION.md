## Console File Caddy

[TOC]: # " Version History"

### Version History
- [Console File Caddy](#console-file-caddy)
  - [Next 1.2.0 - Enhancement Release](#next-120---enhancement-release)
  - [Next 1.1.0 - Enhancement Release](#next-110---enhancement-release)
  - [1.0.0 - Initial Release](#100---initial-release)


### Next 1.2.0 - Enhancement Release

* [ ] Fix: remove test for file existence for `file://` prefixed text
* [ ] Add: Handle fqn with relative path suffix to allow resource refs using query suffix to fqn
      class name. Something like `fqn://...?resourcePath#LineInfo`.

### Next 1.1.0 - Enhancement Release

* Add: `diff:///path1#xxx?/path2#yyy&` link handling to launch diff viewer, where `xxx` and
  `yyy` are line number information, where to put the corresponding files' cursors.

### 1.0.0 - Initial Release

* Add: handling of `fqn://` prefix and fqn to file resolution
* Fix: handling of prefix to remove before testing for file existence.
* Add: Custom URL filter for file paths with line/column information to have flexibility in what
  is recognized as a file link in IDE terminal console output.

