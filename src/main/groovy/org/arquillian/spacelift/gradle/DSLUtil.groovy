package org.arquillian.spacelift.gradle

import java.lang.reflect.Field

import org.apache.commons.lang3.SystemUtils


class DSLUtil {

    public static Map osMapping = [
        windows: { return SystemUtils.IS_OS_WINDOWS },
        mac    : { return SystemUtils.IS_OS_MAC_OSX },
        linux  : { return SystemUtils.IS_OS_LINUX },
        solaris: { return SystemUtils.IS_OS_SOLARIS || SystemUtils.IS_OS_SUN_OS },
    ]

    static List<Field> availableClosureProperties(Object object) {

        // using fields here on purpose, MetaProperty will use getter if available, changing type
        object.class.declaredFields.findAll { Field field ->
            // find all properties that are of type Closure and haven't been already defined
            field.type.isAssignableFrom(Closure.class)
        }
    }

    static List<Field> undefinedClosurePropertyMethods(Object object) {
        availableClosureProperties(object).findAll { Field field ->
            // find all properties that don't have DSL setter defined
            !object.metaClass.respondsTo(object, field.name, Object[].class)
        }
    }

    static List<String> availableClosurePropertyNames(Object object) {
        availableClosureProperties(object).collect { Field field -> field.name }
    }

    static void generateClosurePropertyMethods(Object object) {
        undefinedClosurePropertyMethods(object).each { Field field ->
            object.metaClass."${field.name}" = { Object... lazyClosure ->
                //println "Calling ${field.name}(Object...) at ${delegate.class.simpleName} ${delegate.name}"
                delegate.@"${field.name}" = lazyValue(lazyClosure).dehydrate()
            }
        }
    }

    // FIXME this method is not very robust
    public static Closure lazyValue(Object... args) {
        Closure retVal;
        Object arg;

        // check how many arguments we've received. If just one, unwrap it from array
        if(args.length > 1) {
            arg = args
        }
        else {
            arg = args[0]
        }

        if (arg instanceof Closure) {
            retVal = arg.dehydrate()
            return retVal
        } else if (arg instanceof Map) {
            osMapping.each { platform, condition ->
                if (condition.call()) {
                    retVal = lazyValue(arg[platform])
                    return
                }
            }
            if (retVal == null) {
                throw new IllegalStateException("Unknown system ${System.getProperty('os.name')}")
            }
            else {
                return retVal
            }
        } else if(arg == null) {
            return arg
        }

        // return value wrapper as closure
        return { arg }
    }
}
