package org.arquillian.spacelift.gradle.configuration

import org.arquillian.spacelift.gradle.InheritanceAwareContainer

/**
 * Created by tkriz on 02/04/15.
 */
class ConfigurationContainer extends InheritanceAwareContainer<ConfigurationItem<?>, ConfigurationItem<?>> {

    ConfigurationContainer(Object parent) {
        super(parent, ConfigurationItem.class, ConfigurationItem.class)

    }

    ConfigurationContainer(ConfigurationContainer other) {
        super(other)
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        objects.each {
            builder.append("\t${it.name} (${it.type.resolve()})\n")
                    .append("\t\t${it.description.resolve()}\n\n")
        }

        return builder.toString();
    }
}
