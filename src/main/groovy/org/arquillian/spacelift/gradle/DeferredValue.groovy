package org.arquillian.spacelift.gradle

import org.apache.commons.lang3.SystemUtils


/**
 * A container for a value to be retrieved later. Allows to encapsulate a value defined in
 * DSL and retrieve it later, when needed, in a type-safe manner.
 *
 * It also provide automatic casting and system-dependent value resolution
 *
 * @author kpiwko
 *
 * @param <TYPE> Type of deferred value
 */
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


    // type of deferred value
    final Class<TYPE> type

    // holder of deferred value
    Closure valueBlock = {}

    // name of deferred value
    String name

    // owner of deferred value
    Object owner

    /**
     * Creates a new instance of DeferredValue of {@code type} type. If you don't
     * care about type at all, set it to {@see Void}, if this can be any value, set
     * it to {@see Object}.
     *
     * Note, only first level of generics can be used here, so type is {@code List.class}
     *
     *
     * @param type Type of deferred value
     * @return new instance of deferred value with
     */
    static <T> DeferredValue<T> of(Class<T> type) {
        return new DeferredValue<TYPE>(type)
    }

    /**
     * Tries to guess behavior data from the arguments passed to deferred value. Behavior
     * map must be the first parameter of the {@code args} and it alter behavior of created
     * object
     *
     * @param args Arguments to be parsed
     * @return Behavior map
     */
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

    /**
     * Private constructor
     * @param type
     */
    private DeferredValue(Class<TYPE> type) {
        this.type = type
    }

    /**
     * Adds a name to deferred value. Most likely, it is a name of the field
     * value is assigned to. You can use {@see DSLInstrumenter#instrument(Object)}
     * to set name of all deferred values in the object
     *
     * @param name Name of the deferred value
     * @return
     */
    DeferredValue<TYPE> named(String name) {
        this.name = name
        return this
    }

    /**
     * Adds a name to deferred value. Most likely, it is a name of the field
     * value is assigned to. You can use {@see DSLInstrumenter#instrument(Object)}
     * to set name of all deferred values in the object.
     *
     * Warning: Owner object is mandatory
     *
     * @param name Name of the deferred value
     * @return
     */
    DeferredValue<TYPE> ownedBy(Object owner) {
        this.owner = owner
        return this
    }

    /**
     * Sets any data to be used to compute deferred value later
     * @param data data to compute value. Can be anything, most likely
     * {@see String}, {@see List}, @{see Closure} etc.
     * @return
     */
    DeferredValue<TYPE> from(Object... data) {
        this.valueBlock = defer(data)
        return this
    }

    TYPE apply(TYPE delegate) {
        this.valueBlock.resolveStrategy = Closure.DELEGATE_FIRST
        return resolveWith(delegate, null)
    }

    /**
     * Resolves deferred value using owner of the object as the delegate.
     *
     * @return
     */
    TYPE resolve() {
        return resolveWith(owner, null)
    }

    /**
     * Resolves deferred value using provided object as the delegate
     * @param delegate the delegate of the deferred value resolution
     * @return
     * @throw IllegalArgumentException if resolved object does not match TYPE
     */
    TYPE resolveWith(Object delegate) {
        return resolveWith(delegate, null)
    }

    /**
     * Resolves deferred value using provided object as the delegate. Additionally passes
     * arguments to the {@see Closure} that contains deferred value.
     *
     * @param delegate the delegate of the deferred value resolution
     * @param arguments arguments to be passed to closure during resolution
     * @return
     * @throw IllegalArgumentException if resolved object does not match TYPE
     */
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

    /**
     * Creates a copy of deferred value, allowing to use it in different context
     * @return
     */
    DeferredValue<TYPE> copy() {
        DeferredValue copy = new DeferredValue(type)
        copy.valueBlock = (Closure) valueBlock.clone()
        copy.owner = this.owner
        copy.name = this.name
        return copy
    }

    /**
     * NOT YET IMPLEMENTED
     * @param callback
     * @return
     */
    DeferredValue<TYPE> done(DoneCallback<TYPE> callback) {

    }

    /**
     * NOT YET IMPLEMENTED
     * @param callback
     * @return
     */
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
