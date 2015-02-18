package org.arquillian.spacelift.gradle

import org.apache.commons.lang3.SystemUtils

class DeferredValue<TYPE> {

    // mapping for OS system values
    private static final Map osMapping = [
        windows: { return SystemUtils.IS_OS_WINDOWS },
        mac    : { return SystemUtils.IS_OS_MAC_OSX },
        linux  : { return SystemUtils.IS_OS_LINUX },
        solaris: {
            return SystemUtils.IS_OS_SOLARIS || SystemUtils.IS_OS_SUN_OS
        },
    ]


    final Class<TYPE> type
    Closure valueBlock = {}

    // both owner and name are set later on via Metaclass
    String name
    Object owner

    public static <T> DeferredValue<T> of(Class<T> type) {
        return new DeferredValue<TYPE>(type)
    }

    private DeferredValue(Class<TYPE> type) {
        this.type = type
    }

    DeferredValue<TYPE> named(String name) {
        this.name = name
        return this
    }

    DeferredValue<TYPE> ownedBy(Object owner) {
        this.owner = owner
        return this
    }

    DeferredValue<TYPE> from(Object... data) {
        this.valueBlock = defer(data)
        return this
    }

    TYPE apply(TYPE delegate) {
        this.valueBlock.resolveStrategy = Closure.DELEGATE_FIRST
        return resolveWith(delegate, null)
    }

    TYPE resolve() {
        return resolveWith(owner, null)
    }

    TYPE resolveWith(Object delegate) {
        return resolveWith(delegate, null)
    }

    TYPE resolveWith(Object delegate, Object arguments) {

        if(owner==null) {
            throw new IllegalStateException("DeferredValue was not correctly initialized, owner object was not set")
        }

        Object retVal = null;
        if(valueBlock.maximumNumberOfParameters==0) {
            retVal = valueBlock.rehydrate(delegate, owner, owner).call()
        }
        else {
            retVal = valueBlock.rehydrate(delegate, owner, owner).call(arguments)
        }

        if(retVal!=null && type.isAssignableFrom(retVal.getClass())) {
            return (TYPE) retVal
        }
        // if type was void, we don't care about the result, drop it
        else if(Void.class.isAssignableFrom(type)) {
            return null
        }
        // make it a collection if asked for
        else if(List.class.isAssignableFrom(type)) {
            if(retVal!=null) {
                // cast array if supplied
                if(retVal.getClass().isArray()) {
                    return Arrays.asList(retVal);
                }
                return Collections.singletonList(retVal)
            }
            return Collections.emptyList()
        }
        // cast to String if this is different implementation
        else if(retVal!=null && retVal instanceof CharSequence && type == String.class) {
            return (TYPE) retVal.toString()
        }
        // cast String to a File location in workspace
        else if(File.class.isAssignableFrom(type) && retVal!=null && retVal instanceof CharSequence) {
            return new File(new GradleSpaceliftDelegate().project()['spacelift']['workspace'], retVal.toString())
        }
        // cast String to a URL location
        else if(URL.class.isAssignableFrom(type) && retVal!=null && retVal instanceof CharSequence) {
            return new URL(retVal.toString())
        }
        // if that was null and collection return was not expected
        else if(retVal==null) {
            return null
        }

        throw new IllegalArgumentException("Unable to evaluate ${name} of ${valueBlock.delegate.getClass().simpleName}, expected return value of ${type.simpleName} but was ${retVal.getClass().simpleName}")
    }

    DeferredValue<TYPE> copy() {
        DeferredValue copy = new DeferredValue(type)
        copy.valueBlock = (Closure) valueBlock.clone()
        copy.owner = this.owner
        copy.name = this.name
        return copy
    }

    DeferredValue<TYPE> done(DoneCallback<TYPE> callback) {

    }

    DeferredValue<TYPE> fail(FailCallback<TYPE> callback) {

    }

    public static interface DoneCallback<X> {
        X call(X value);
    }

    public static interface FailCallback<X> {
        X call(Exception error, X value);
    }


    private Closure defer(Object... data) {
        // if nothing is defined, return empty closure, e.g. closure that returns null
        if(data==null || data.length==0) {
            return {}
        }
        // if there is one parameter, it can be a lot of things, try to determine type
        else if(data.length==1) {
            Object arg = data[0]
            // wrap to call that returns String
            if(arg instanceof CharSequence) {
                return { arg.toString() }
            }
            // wrap to call that returns List
            else if(arg!=null && arg.getClass().isArray()) {
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

                // are we dealing platform map? if not, return map
                if(osMapping.inject(true) { value, os -> value &= !arg.containsKey(os.key)}) {
                    return  { arg }
                }

                // return according to the platform
                Closure platformValue = null
                osMapping.each { platform, condition ->
                    if (condition.call() && arg[platform]) {
                        platformValue = defer(arg[platform])
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
        else if(data.length>1) {
            // all of them are strings
            if(data.inject(true) { value, arg ->  value &= (arg instanceof CharSequence)}) {
                return wrapAsListOfStrings(data)
            }
            // if there are behavior arguments, for real value we should slice the array
            if(!getBehaviors(data).isEmpty()) {
                return defer(Arrays.asList(data).tail().toArray())
            }
        }

        // we do not support passing different values in DSL, make sure that we won't return null by incomplete branching
        throw new IllegalArgumentException("Unsupported parameters passed to ${name} (" + data.collect { it.getClass().simpleName}.join(', ') +")")

    }

    static Map getBehaviors(Object...args) {
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
