package com.vladsch.plugins.consolefilecaddy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class SampleCustomizedTest {
    final String location;
    final String name;
    final int expected;
    final int actual;

    public SampleCustomizedTest(String location, String name, int expected, int actual) {
        this.location = location;
        this.name = name;
        this.expected = expected;
        this.actual = actual;
    }

    static class CustomParamRow extends ParamRowGenerator {
        public CustomParamRow() {
            super();
        }

        @Parameterized.Parameters(name = "{1}")
        public CustomParamRow row(String test, int expected, int actual) {
            return (CustomParamRow) super.row(
                    1,
                    new Object[] { test, expected, actual },
                    (index, prefix, suffix) -> prefix + "\ntest: " + suffix,
                    null,

                    // this puts the position at the end of the actual value provided standard formatting is used for all rows
                    row -> 22 // position of start of parameter 1
                            + (test == null ? 4 : 2 + test.length()) // length of text if null then 4, else 2 quotes + length of name
                            + 2 // comma + space
                            + String.valueOf(expected).length()
                            + 2 // comma + space
                            + String.valueOf(actual).length()
            );
        }
    }

    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> data() {
        return new CustomParamRow()
                .row("Pass", 1, 1)
                .row("Fail", 2, 3)
                .row("Pass", 4, 4)
                .rows;
    }

    @Test
    public void test_sample() {
        assertEquals(location, expected, actual);
    }
}

