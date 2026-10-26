package com.lms.webapp.smoke;

import java.util.function.Function;
import java.util.Locale;

public final class SmokeTestSettings {
    private SmokeTestSettings() { }

    public static String required(String property) {
        return required(property, System::getProperty, System::getenv);
    }

    static String required(String property, Function<String, String> properties,
                           Function<String, String> environment) {
        String environmentKey = property.toUpperCase(Locale.ROOT).replace('.', '_');
        String value = properties.apply(property);
        if (value == null) {
            value = environment.apply(environmentKey);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing smoke prerequisite: set " + environmentKey
                    + " (or " + property + "). No default credentials are used.");
        }
        return value;
    }
}
