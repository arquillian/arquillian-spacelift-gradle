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
            // find all properties that are of type Closure
            Closure.class.isAssignableFrom(field.type)
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

    public static Closure lazyValue(Object... args) throws IllegalArgumentException {

        // if nothing is defined, return empty closure, e.g. closure that returns null
        if(args==null || args.length==0) {
            return {}
        }
        // if there is one parameter, it can be a lot of things, try to determine type
        else if(args.length==1) {
            Object arg = args[0]
            // wrap to call that returns String
            if(arg instanceof CharSequence) {
                return { arg.toString() }
            }
            // wrap to call that returns List
            else if(arg instanceof Object[]) {
                return { Arrays.asList(arg) }
            }
            // wrap to call that returns Collection
            else if(arg instanceof Collection) {
                return { arg }
            }
            else if(arg instanceof Closure) {
                return arg.dehydrate()
            }
            else if(arg instanceof Map) {
                Closure platformValue = null
                osMapping.each { platform, condition ->
                    if (condition.call()) {
                        platformValue = lazyValue(arg[platform])
                        return
                    }
                }
                if (platformValue == null) {
                    throw new IllegalArgumentException("System ${System.getProperty('os.name')} is not supported, should be one of: " + arg.keySet().join(", "))
                }
                else {
                    return platformValue
                }
            }
            // arbitrary value we have no support for, such as boolean
            else {
                return { arg }
            }
        }
        else if(args.length>1) {
            // all of them are strings
            if(args.inject(true) { value, arg ->  value &= (arg instanceof CharSequence)}) {
                return wrapAsListOfStrings(args)
            }
            // if there are behavior arguments, for real value we should slice the array
            if(!getBehaviors(args).isEmpty()) {
                return lazyValue(Arrays.asList(args).tail().toArray())
            }
        }

        // we do not support passing different values in DSL, make sure that we won't return null by incomplete branching
        throw new IllegalArgumentException("Unsupported parameters passed to method (" + args.collect { it.class?.simpleName}.join(', ') +")")
    }

    public static Map getBehaviors(Object...args) {
        Map defaultBehaviors = [:]
        if(args!= null && args.length>1 && args[0] instanceof Map) {
            // now we know there is a map, what we don't know is whether this is behavior or not
            // make an educated guess based on OS conditions, if there is no such key, it is behavior
            Map map = args[0]
            if(osMapping.inject(true) { value, os -> value &= !map.containsKey(os.key)}) {
                return map
            }
        }

        return defaultBehaviors
    }

    private static Closure wrapAsListOfStrings(Object...args) {
        if(args.inject(true) { value, arg ->  value &= (arg instanceof CharSequence)}) {
            return {
                // unwrap collection an return everything as Collection<String>
                return args.collect { it.toString() }
            }
        }
        throw new IllegalArgumentException("Unable to wrap collection ${args} as List<String>")
    }
}