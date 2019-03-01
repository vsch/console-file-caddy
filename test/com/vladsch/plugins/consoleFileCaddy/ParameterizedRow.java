package com.vladsch.plugins.consoleFileCaddy;

public class ParameterizedRow {
    private final String locationPrefix;
    private int index;

    public ParameterizedRow(final String locationPrefix) {
        this.locationPrefix = locationPrefix.endsWith("/") ? locationPrefix : locationPrefix + "/";
        index = 0;
    }

    public static class LineInfo {
        final public String file;
        final public int line;
        final public int index;

        public LineInfo(final String file, final int line, final int index) {
            this.file = file;
            this.line = line;
            this.index = index;
        }
    }

    public LineInfo getCallerInfo(int callerDepth) {
        StackTraceElement callerInfo = Thread.currentThread().getStackTrace()[callerDepth];
        return new LineInfo(locationPrefix + callerInfo.getFileName(), callerInfo.getLineNumber(), index++);
    }
}
