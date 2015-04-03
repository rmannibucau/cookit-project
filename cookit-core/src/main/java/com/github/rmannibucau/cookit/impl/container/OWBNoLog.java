package com.github.rmannibucau.cookit.impl.container;

import org.apache.webbeans.logger.JULLoggerFactory;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OWBNoLog extends JULLoggerFactory {
    @Override
    public Logger getLogger(final Class<?> clazz, final Locale desiredLocale) {
        final Logger logger = super.getLogger(clazz, desiredLocale);
        logger.setLevel(Level.WARNING);
        return logger;
    }

    @Override
    public Logger getLogger(final Class<?> clazz) {
        final Logger logger = super.getLogger(clazz);
        logger.setLevel(Level.WARNING);
        return logger;
    }
}
