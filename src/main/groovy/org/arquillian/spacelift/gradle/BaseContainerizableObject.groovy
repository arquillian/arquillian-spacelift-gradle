package org.arquillian.spacelift.gradle

import groovy.lang.Closure;
import groovy.transform.CompileStatic;

import java.lang.reflect.Field

import org.gradle.api.Project

@CompileStatic
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
    // FIXME, same as propertyMissing, this is causing issues trying to resolve closures we do not control
    /*
    def methodMissing(String name, args) {

        println "In method: ${name} as ${this}"

        List<String> availableDSL = DSLUtil.availableClosurePropertyNames(this).collect { "${it}(Object...objects)"} + DSLUtil.availableContainerNames(this).collect { "${it}(Closure closure)"};
        throw new GroovyRuntimeException("${this.class} named ${this.name}, unable to call method ${name}(" +
        args.collect { it.class }.join(', ')+"), have you meant one of: ${availableDSL.join(', ')}?");
    }
    */
}
