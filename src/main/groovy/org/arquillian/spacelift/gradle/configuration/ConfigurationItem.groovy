package org.arquillian.spacelift.gradle.configuration

import groovy.transform.CompileStatic
import org.arquillian.spacelift.gradle.BaseContainerizableObject
import org.arquillian.spacelift.gradle.DeferredValue

/**
 * Created by tkriz on 24/03/15.
 */
@CompileStatic
class ConfigurationItem<T> extends BaseContainerizableObject<ConfigurationItem<T>> {

    DeferredValue<Class<T>> type = DeferredValue.of(Class.class).from(String.class)

    DeferredValue<String> description = DeferredValue.of(String.class).from("No description.")

    DeferredValue<ConfigurationItemConverter<T>> converter = DeferredValue.of(ConfigurationItemConverter.class).valueBlockSet {
        converterSet = true
    }

    DeferredValue<T> defaultValue = DeferredValue.of(T.class)

    DeferredValue<T> value = DeferredValue.of(T.class).valueBlockSet {
        set = true
    }

    boolean set = false

    boolean converterSet = false

    ConfigurationItem(String name, Object parent) {
        super(name, parent)
    }

    ConfigurationItem(String name, ConfigurationItem<T> template) {
        super(name, template)

        type = template.@type.copy()
        description = template.@description.copy()
        converter = template.@converter.copy()
        defaultValue = template.@defaultValue.copy()
        value = template.@value.copy()

        set = template.@set
        converterSet = template.@converterSet
    }

    boolean isSet() {
        return set
    }

    boolean isConverterSet() {
        return converterSet
    }

    T getValue() {
        DeferredValue<T> output = null
        if (set) {
            output = value
        } else {
            output = defaultValue
        }
        return output.resolve() //DeferredValue.of(type.resolve()).from(output.resolve()).ownedBy(output.getOwner()).resolve()
    }

    @Override
    ConfigurationItem<T> clone(String name) {
        return null
    }

// jbossHome type: File, description: "Hello", defaultValue: new File()

}
