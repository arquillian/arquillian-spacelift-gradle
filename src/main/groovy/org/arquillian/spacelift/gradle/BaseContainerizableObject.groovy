package org.arquillian.spacelift.gradle

import groovy.transform.CompileStatic

import org.slf4j.Logger
import org.slf4j.LoggerFactory

@CompileStatic
abstract class BaseContainerizableObject<TYPE extends BaseContainerizableObject<TYPE>> implements ContainerizableObject<TYPE> {
    private static final Logger logger = LoggerFactory.getLogger(BaseContainerizableObject)

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

        GroovyObject ancestor = (GroovyObject) parent
        while(ancestor) {
            try {
                Object val = ancestor.getProperty(name)
                logger.debug("Retrieved property \"${name}\"a from ${ancestor}")
                return val
            }
            // if property was not found, try to get it from parent of parent
            catch(MissingPropertyException e) {
                if(ancestor.hasProperty("parent")) {
                    ancestor = (GroovyObject) ancestor.getProperty("parent")
                }
                else {
                    throw new MissingPropertyException("Unable to resolve property \"${name}\" in ${this.getClass().simpleName} ${this.name}. Failed with: ${e.getMessage()}")
                }
            }
        }
    }
}
