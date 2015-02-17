package org.arquillian.spacelift.gradle

import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

import org.apache.commons.lang3.SystemUtils


/**
 * Generator of DSL related methods on ContainerizableObject
 *
 * @author kpiwko
 *
 */
class DSLInstrumenter {

    /**
     * Generate methods handling {@link DeferredValue} and @{link InheritanceAwareContainer}
     * fields in the {@code object}
     *
     * @param object Object to be instrumented
     *
     * @return instrumented object
     */
    static <TYPE> TYPE instrument(TYPE object) {
        generateContainerMethods(object)
        generateDeferredValueMethods(object)
        return object
    }

    static void generateDeferredValueMethods(Object object) {
        availableDeferredValues(object).each { Field field ->

            // setup name of all deferred values
            object.@"${field.name}".named(field.name)
            // setup owner of all deferred values
            object.@"${field.name}".ownedBy(object)

            // define all DSL setters, note delegate will become this object
            if(!object.metaClass.respondsTo(object, field.name, Object[].class)) {
                object.metaClass."${field.name}" = { Object... lazyClosure ->
                    ((DeferredValue) delegate.@"${field.name}").from(lazyClosure)
                }
            }

            String getterName = "get"
            Type retValType = field.getGenericType()
            if(retValType instanceof ParameterizedType && Boolean.class.isAssignableFrom(retValType.getActualTypeArguments()[0])) {
                getterName = "is"
            }
            // camel case
            getterName += field.name.substring(0, 1).toUpperCase() +  field.name.substring(1)

            // define all DSL getters, note delegate will become this object
            if(!object.metaClass.respondsTo(object, getterName, null)) {
                object.metaClass."${getterName}" = {
                    delegate.@"${field.name}".resolveWith(delegate)
                }
            }
        }
    }

    static void generateContainerMethods(Object object) {
        availableContainers(object).each { Field field ->
            if(!object.metaClass.respondsTo(object, field.name, Closure.class)) {
                object.metaClass."${field.name}" = { Closure configureClosure ->
                    //println "Calling ${field.name}(Closure) at ${delegate.class.simpleName} ${delegate.name}"
                    delegate.@"${field.name}".configure(configureClosure)
                    return delegate
                }
            }
        }
    }

    private static List<Field> availableContainers(Object object) {
        // using fields here on purpose, MetaProperty will use getter if available, changing type
        return object.class.declaredFields.findAll { Field field ->
            // find all properties that are of type Closure
            InheritanceAwareContainer.class.isAssignableFrom(field.type)
        }
    }

    private static List<Field> availableDeferredValues(Object object) {
        // using fields here on purpose, MetaProperty will use getter if available, changing type
        return object.class.declaredFields.findAll { Field field ->
            // find all properties that are of type DelayedValue
            DeferredValue.class.isAssignableFrom(field.type)
        }
    }
}