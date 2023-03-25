package com.vladsch.plugins.consoleFileCaddy;

import org.junit.Test;

import static org.junit.Assert.fail;

public class SampleDiffTest {
    @Test
    public void testDiffSample() {
        fail("diff:///Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy/SampleTest.java:23:36 ? /Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy/SampleCustomizedTest.java:49:56&");
    }

    @Test
    public void testDiffDirSample() {
        fail("diff:///Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy ? /Users/vlad/src/projects/console-file-caddy/src/com/vladsch/plugins/consoleFileCaddy/ &");
    }

    @Test
    public void testDiffSample1() {
        fail("diff:///Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy/SampleTest.java:23:36?/Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy/SampleCustomizedTest.java:49:56&");
    }

    @Test
    public void testDiffDirSample1() {
        fail("diff:///Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy?/Users/vlad/src/projects/console-file-caddy/src/com/vladsch/plugins/consoleFileCaddy/ &");
    }

    @Test
    public void testDiffSample2() {
        fail("diff:///Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy/SampleTest.java:23:36?/Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy/SampleCustomizedTest.java:49:56&");
    }

    @Test
    public void testDiffDirSample2() {
        fail("diff:///Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy?/Users/vlad/src/projects/console-file-caddy/src/com/vladsch/plugins/consoleFileCaddy/&");
    }
    @Test
    public void testDiffSample3() {
        fail("diff:///[Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy/SampleTest.java:23:36]?/[Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy/SampleCustomizedTest.java:49:56]&");
    }

    @Test
    public void testDiffDirSample3() {
        fail("diff:///[Users/vlad/src/projects/console-file-caddy/test/com/vladsch/plugins/consoleFileCaddy]?/[Users/vlad/src/projects/console-file-caddy/src/com/vladsch/plugins/consoleFileCaddy/]&");
    }
}
