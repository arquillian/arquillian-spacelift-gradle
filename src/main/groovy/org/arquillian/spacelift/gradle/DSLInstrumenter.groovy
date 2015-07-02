package org.arquillian.spacelift.gradle

import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

    private static Logger log = LoggerFactory.getLogger("DSLInstrumenter")

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
        availableDeferredValues(object.getClass()).each { Field field ->

            field.setAccessible(true)
            DeferredValue<?> deferredValue = (DeferredValue<?>) field.get(object)

            // setup name of all deferred values
            deferredValue.named(field.name)
            // setup owner of all deferred values
            deferredValue.ownedBy(object)

            // define all DSL setters, note delegate will become this object
            if(!object.metaClass.respondsTo(object, field.name, Object[].class)) {
                object.metaClass."${field.name}" = { Object... lazyClosure ->
                    ((DeferredValue) delegate.@"${field.name}").from(lazyClosure)
                }
                log.debug("Created DSL setter ${object.getClass().getSimpleName()}#${field.name}(Object...)")
            }

            String getterName = "get"
            Type retValType = field.getGenericType()
            // FIXME this does not work for nested-generic parameters!
            if(retValType instanceof ParameterizedType && Boolean.class == retValType.getActualTypeArguments()[0]) {
                getterName = "is"
            }
            // camel case
            getterName += field.name.substring(0, 1).toUpperCase() +  field.name.substring(1)

            // define all DSL getters, note delegate will become this object
            if(!object.metaClass.respondsTo(object, getterName, null)) {
                object.metaClass."${getterName}" = {
                    delegate.@"${field.name}".resolveWith(delegate)
                }
                log.debug("Created DSL getter ${object.getClass().getSimpleName()}#${getterName}()")
            }
        }
    }

    static void generateContainerMethods(Object object) {
        availableContainers(object.getClass()).each { Field field ->
            if(!object.metaClass.respondsTo(object, field.name, Closure.class)) {
                object.metaClass."${field.name}" = { Closure configureClosure ->
                    //println "Calling ${field.name}(Closure) at ${delegate.class.simpleName} ${delegate.name}"
                    delegate.@"${field.name}".configure(configureClosure)
                    return delegate
                }
            }
        }
    }

    private static List<Field> availableContainers(Class<?> cls) {
        return getAllDeclaredFields(cls).findAll { Field field ->
            // find all properties that are of type Closure
            InheritanceAwareContainer.class.isAssignableFrom(field.type)
        }
    }

    private static List<Field> availableDeferredValues(Class<?> cls) {
        return getAllDeclaredFields(cls).findAll { Field field ->
            // find all properties that are of type DeferredValue
            DeferredValue.class.isAssignableFrom(field.type)
        }
    }

    private static List<Field> getAllDeclaredFields(Class<?> cls) {
        ArrayList<Field> fields = new ArrayList<>()

        Class<?> current = cls
        // We do not want to parse Object.class
        while(current.getSuperclass() != null) {
            // using fields here on purpose, MetaProperty will use getter if available, changing type
            fields.addAll(current.declaredFields)
            current = current.superclass
        }

        return fields
    }
}