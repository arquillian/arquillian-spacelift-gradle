package org.arquillian.spacelift.gradle

import groovy.lang.Closure;
import groovy.transform.CompileStatic;

import java.lang.reflect.Field
import java.util.List;

import org.gradle.api.Project

@CompileStatic
abstract class BaseContainerizableObject<TYPE extends BaseContainerizableObject<TYPE>> implements ContainerizableObject<TYPE> {

    final String name
    final Object parent

    BaseContainerizableObject(String name, Object parent) {
        this.name = name
        this.parent = parent
    }

    BaseContainerizableObject(String name, TYPE template) {
        this.name = name
        this.parent = template.parent
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "${this.getClass().simpleName} ${name}"
    }

    def propertyMissing(String name) {
        try {
            println "trying to get ${name} from ${parent}"
            return ((GroovyObject)parent).getProperty(name)
        }
        catch(MissingPropertyException e) {
            throw new MissingPropertyException("Unable to resolve property named ${name} in ${this.getClass().simpleName} ${this.name}, detailed message: ${e.getMessage()}");
        }
    }
}
