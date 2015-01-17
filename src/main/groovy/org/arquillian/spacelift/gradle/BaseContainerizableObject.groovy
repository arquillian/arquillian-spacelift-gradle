package org.arquillian.spacelift.gradle

import groovy.lang.Closure;

import java.lang.reflect.Field

import org.gradle.api.Project

abstract class BaseContainerizableObject<TYPE extends BaseContainerizableObject<TYPE>> implements ContainerizableObject<TYPE> {

    final String name
    final Project project

    BaseContainerizableObject(String name, Project project) {
        this.name = name;
        this.project = project;
    }

    BaseContainerizableObject(String name, TYPE template) {
        this.name = name;
        // project is always copied as shallow object
        this.project = template.project;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * This method allows to provide nice warning if user is trying to specify closure
     * that is not supported by given object
     *
     * @param name name of the field
     * @param args
     * @return
     */
    def methodMissing(String name, args) {
        List<String> availableDSL = DSLUtil.availableClosurePropertyNames(this).collect { "${it}(Object...objects)"} + DSLUtil.availableContainerNames(this).collect { "${it}(Closure closure)"};
        throw new GroovyRuntimeException("${this.class} named ${this.name}, unable to call method ${name}(" +
        args.collect { it.class }.join(', ')+"), have you meant one of: ${availableDSL.join(', ')}?");
    }
}
