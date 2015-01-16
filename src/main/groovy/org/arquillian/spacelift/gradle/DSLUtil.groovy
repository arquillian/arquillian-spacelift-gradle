package org.arquillian.spacelift.gradle

import org.apache.commons.lang3.SystemUtils


class DSLUtil {

    public static Map osMapping = [
        windows: { return SystemUtils.IS_OS_WINDOWS },
        mac    : { return SystemUtils.IS_OS_MAC_OSX },
        linux  : { return SystemUtils.IS_OS_LINUX },
        solaris: { return SystemUtils.IS_OS_SOLARIS || SystemUtils.IS_OS_SUN_OS },
    ]

    static List<MetaProperty> availableClosureProperties(Object object) {
        MetaClass metaClass = object.metaClass

        metaClass.properties.findAll { MetaProperty property ->
            // find all properties that are of type Closure and haven't been already defined
            property.type.isAssignableFrom(Closure.class)
        }
    }

    static List<MetaProperty> undefinedClosurePropertyMethods(Object object) {
        availableClosureProperties(object).findAll { MetaProperty property ->
            // find all properties that don't have DSL setter defined
            !object.metaClass.respondsTo(object, property.name)//, Object.class)
        }
    }

    static List<String> availableClosurePropertyNames(Object object) {
        availableClosureProperties(object).collect { MetaProperty property -> property.name }
    }

    static void generateClosurePropertyMethods(Object object) {
        undefinedClosurePropertyMethods(object).each { property ->
            object.metaClass."${property.name}" = { Object... lazyClosure ->
                println "Calling ${property.name}(Object...) at ${delegate.class.simpleName} ${delegate.name}"
                delegate.@"${property.name}" = lazyValue(lazyClosure).dehydrate()
            }
        }
    }

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
