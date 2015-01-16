package org.arquillian.spacelift.gradle


import java.util.concurrent.ConcurrentHashMap

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.Project
import org.gradle.util.Configurable

/**
 * This is a way how to convert DSL into tool, installation, profile or test, with support of
 * inheritance
 *
 * @author kpiwko
 *
 * @param <T>
 */
class InheritanceAwareContainer<TYPE extends ContainerizableObject<TYPE>, DEFAULT_TYPE extends TYPE> implements Iterable<TYPE>, Configurable<TYPE>, Collection<TYPE>, Cloneable {

    private static final ConcurrentHashMap<Class<?>, Class<?>> typeMetaClasses = new ConcurrentHashMap<Class<?>, Class<?>>()

    Class<TYPE> type

    Class<DEFAULT_TYPE> defaultType

    Set<TYPE> objects

    Project project

    Object parent

    InheritanceAwareContainer(Project project, Object parent, Class<TYPE> type, Class<DEFAULT_TYPE> defaultType) {
        this.project = project
        this.type = type
        this.defaultType = defaultType
        this.parent = parent
        this.objects = new LinkedHashSet<String, TYPE>()
    }

    InheritanceAwareContainer(InheritanceAwareContainer<TYPE, DEFAULT_TYPE> other) {
        this.project = other.project
        this.type = other.type
        this.parent = other.parent
        this.objects = new LinkedHashSet<TYPE>(other.objects)
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        new InheritanceAwareContainer<TYPE, DEFAULT_TYPE>(this)
    }

    /**
     * This method dynamically creates an object of type <T>.
     * Object must define a constructor of type (name, project)
     * @param name name of object to be created
     * @param args
     * @return
     */
    def methodMissing(String name, args) {

        Closure configureClosure
        Map behavior = [:]

        // unwrap array in case there is single argument
        if(args instanceof Object[] && args.size()==1) {
            configureClosure = DSLUtil.lazyValue(args[0]).dehydrate()
        }
        // check whether there was inheritance used or any named parameters were passed
        else if(args instanceof Object[] && args.size()==2) {
            if(args[0] instanceof Map) {
                behavior = args[0]
            }
            configureClosure = DSLUtil.lazyValue(args[1]).dehydrate()
        }
        else {
            configureClosure = DSLUtil.lazyValue(args).dehydrate()
        }

        create(name, behavior, configureClosure)
    }

    TYPE create(String name, Map behavior, Closure closure) {

        ContainerizableObject<?> object

        // if from behavior is not specified, default to default type for container
        // @Deprecated inherits - inherits to be deprecated and replaced with from
        Object from = behavior.get('from', behavior.get('inherits', defaultType))

        if(from instanceof Class) {
            object = from.newInstance(name, project)
            // this was the first initialization for type, generate closure setter methods
            //if(typeMetaClasses.putIfAbsent(from, from) == null) {
            DSLUtil.generateClosurePropertyMethods(object)
            //}
        }
        else {
            object = resolveParent(from).clone(name)
            DSLUtil.generateClosurePropertyMethods(object)
        }

        closure = closure.rehydrate(new GradleSpaceliftDelegate(), parent, object)

        // configure and store object
        object = project.configure(object, closure)
        // store configured object
        objects << object

        return object
    }

    @Override
    public TYPE configure(Closure configuration) {
        Closure config = DSLUtil.lazyValue(configuration).dehydrate()
        config.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
    }

    @Override
    public Iterator<TYPE> iterator() {
        objects.iterator();
    }

    /**
     * Allows to use subscript operator on container
     * @param name
     * @return
     */
    public TYPE getAt(String name) {
        try {
            for(TYPE o: objects) {
                if(o.name == name) {
                    return o
                }
            }
        }
        catch(MissingMethodException e) {
            throw new MissingPropertyException("Unable to get ${type.getSimpleName()} of name ${name}, does it have getName() method?")
        }
        catch(MissingPropertyException e) {
            throw new MissingPropertyException("Unable to get ${type.getSimpleName()} of name ${name}, does it have name property?")
        }

        throw new MissingPropertyException("Unable to get ${type.getSimpleName()} of name ${name}")
    }

    @Override
    public boolean add(TYPE object) {
        objects.add(object)
    }

    @Override
    public boolean addAll(Collection<? extends TYPE> others) {
        objects.addAll(others)
    }

    @Override
    public void clear() {
        objects.clear()
    }

    @Override
    public boolean contains(Object key) {
        objects.contains(key)
    }

    @Override
    public boolean containsAll(Collection<?> others) {
        objects.containsAll(others)
    }

    @Override
    public boolean isEmpty() {
        objects.isEmpty()
    }

    @Override
    public boolean remove(Object arg0) {
        objects.remove(arg0)
    }

    @Override
    public boolean removeAll(Collection<?> others) {
        objects.removeAll(others)
    }

    @Override
    public boolean retainAll(Collection<?> others) {
        objects.retainAll(others);
    }

    @Override
    public int size() {
        objects.size()
    }

    @Override
    public Object[] toArray() {
        objects.toArray()
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        objects.toArray(a)
    }

    /**
     * Resolves reference by either string. If it gets direct reference, checks whether type is compatible
     * @param reference reference to be resolved
     * @return
     */
    private TYPE resolveParent(def reference) {
        if(reference instanceof CharSequence) {
            getAt(reference.toString())
        }
        else if(type.isInstance(reference)) {
            (TYPE) reference
        }
        else {
            throw new MissingPropertyException("Unable to reference ${type.getSimpleName()} by ${reference}")
        }
    }
}
