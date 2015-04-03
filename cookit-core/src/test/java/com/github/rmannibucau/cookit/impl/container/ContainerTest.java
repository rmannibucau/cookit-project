package com.github.rmannibucau.cookit.impl.container;

import com.github.rmannibucau.cookit.api.environment.Value;
import com.github.rmannibucau.cookit.spi.Container;
import org.junit.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContainerTest {
    @Test
    public void container() {
        try (final Container container = new OWBContainer().start()) {
            final PleaseInjectMe injected = container.inject(new PleaseInjectMe());
            assertEquals("default", injected.str);
            assertTrue(container.isStarted());
        }
    }

    @Dependent
    public static class PleaseInjectMe {
        @Inject
        @Value(key = "sample", or = "default")
        private String str;
    }
}
