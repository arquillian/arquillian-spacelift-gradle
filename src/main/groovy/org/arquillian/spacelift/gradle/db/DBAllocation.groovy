package org.arquillian.spacelift.gradle.db

import java.util.Properties

class DBAllocation {

    private Properties properties

    DBAllocation(Properties properties) {
        this.properties = properties
    }

    // FIXME this overrides Groovy method, it should rather have better name
    @Override
    String getProperty(String property) {
        return properties.getProperty(property)
    }

    Properties getProperties() {
        return properties
    }
}
