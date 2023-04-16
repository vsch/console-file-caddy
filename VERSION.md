## Console File Caddy

[TOC]: # "Version History"

### Version History
- [Console File Caddy](#console-file-caddy)
  - [Next 1.2.0 - Future Enhancement Release](#next-120---future-enhancement-release)
  - [1.1.6 - Dev Build](#116---dev-build)
  - [1.1.4 - Dev Build](#114---dev-build)
  - [1.1.2 - Dev Build](#112---dev-build)
  - [1.1.0 - Enhancement Release](#110---enhancement-release)
  - [1.0.0 - Initial Release](#100---initial-release)

### Next 1.2.0 - Future Enhancement Release

* [ ] Fix: remove test for file existence for `file://` prefixed text
* [ ] Add: Handle fqn with relative path suffix to allow resource refs using query suffix to fqn
      class name. Something like `fqn://...?resourcePath#LineInfo`.

### 1.1.6 - Dev Build

* Fix: diff exception if a file is truncated to 0 size

### 1.1.4 - Dev Build

* Change: Plugin description to include `diff://` links.
* Fix: broken `fqn://` and file link handling

### 1.1.2 - Dev Build

* Add: Optional wrapper for `/path1#xxx` and `/path2#xxx` in `[]` after the first `/` , to make
  it into `/[path1#xxx]` and `/[path2#xxx]`, to hijack the paths and prevent other console
  filters (like CLion's cidr filter) from grabbing them as links to separate files instead of a
  single diff link.

### 1.1.0 - Enhancement Release

* Add: `diff:///path1#xxx?/path2#yyy&` link handling to launch diff viewer, where `xxx` and
  `yyy` are line number information, where to put the corresponding files' cursors.

### 1.0.0 - Initial Release

* Add: handling of `fqn://` prefix and fqn to file resolution
* Fix: handling of prefix to remove before testing for file existence.
* Add: Custom URL to filter file paths with line/column information and flexibility in what is
  recognized as a file link in IDE terminal console output.

