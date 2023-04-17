package com.vladsch.plugins.consolefilecaddy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(value = Parameterized.class)
public class SampleTest {
    final String location;
    final int expected;
    final int actual;

    public SampleTest(final String location, final int expected, final int actual) {
        this.location = location;
        this.expected = expected;
        this.actual = actual;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return new ParamRowGenerator()
                .row(new Object[] { 1, 1 })
                .row(new Object[] { 2, 3 })
                .row(new Object[] { 3, 3 })
                .rows;
    }

    @Test
    public void test_sample() {
        assertEquals(location, expected, actual);
    }
}

