package org.arquillian.spacelift.gradle


import java.util.concurrent.ConcurrentHashMap

import org.apache.commons.lang3.SystemUtils
import org.gradle.api.Project
import org.gradle.util.Configurable

/**
 * Object container that allows additional behavoirs to be used for DSL definition.
 *
 * Mainly, this container supports inheritance of existing DSL elements
 *
 * @author kpiwko
 *
 * @param <T>
 */
class InheritanceAwareContainer<TYPE extends ContainerizableObject<TYPE>, DEFAULT_TYPE extends TYPE> implements Iterable<TYPE>, Configurable<TYPE>, Collection<TYPE>, Cloneable {

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
        Map behavior = DSLUtil.getBehaviors(args)
        Closure configureClosure = DSLUtil.lazyValue(args).dehydrate()
        create(name, behavior, configureClosure)
    }

    TYPE create(String name, Map behavior, Closure closure) {

        ContainerizableObject<?> object

        // if from behavior is not specified, default to default type for container
        // @Deprecated inherits - inherits to be deprecated and replaced with from
        Object from = behavior.get('from', behavior.get('inherits', defaultType))

        // FIXME we need to unwrap reference, but it is not clear why this happens
        if(from instanceof List && from.size()==1) {
            from = from[0]
        }

        if(from instanceof Class && type.isAssignableFrom(from)) {
            object = from.newInstance(name, project)
        }
        else {
            object = resolveParent(from).clone(name)
        }

        DSLUtil.generateClosurePropertyMethods(object)
        closure = closure.rehydrate(new GradleSpaceliftDelegate(), parent, object)

        // configure and store object
        object = project.configure(object, closure)
        // store configured object in the container
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
        TYPE element = null;
        List<String> names = new ArrayList<String>();
        for(TYPE o: objects) {
            names.add(o.name)
            if(o.name == name) {
                return o
            }
        }
        throw new MissingPropertyException("Unable to get ${type.getSimpleName()} ${name}, have you meant one of ${names.join(', ')}?")
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
        else if(reference!=null && type.isAssignableFrom(reference.class)) {
            (TYPE) reference
        }
        else if(reference!=null && !type.isAssignableFrom(reference.class)) {
            throw new MissingPropertyException("Reference ${reference} is not compatible with type ${type.getSimpleName()}")
        }
        else {
            throw new MissingPropertyException("Reference ${reference} of type${type.getSimpleName()} was not found.")
        }
    }
}
