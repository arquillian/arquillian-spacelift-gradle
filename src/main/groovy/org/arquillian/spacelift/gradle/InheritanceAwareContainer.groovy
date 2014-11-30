package org.arquillian.spacelift.gradle

import static groovy.transform.AutoCloneStyle.*
import groovy.transform.AutoClone

import org.gradle.api.Project
import org.gradle.internal.reflect.Instantiator
import org.gradle.util.Configurable

/**
 * This is a way how to convert DSL into tool, installation, profile or test, with support of 
 * inheritance
 * 
 * @author kpiwko
 *
 * @param <T>
 */
class InheritanceAwareContainer<T> implements Iterable<T>, Configurable<T>, Collection<T>, ValueExtractor, Cloneable {

    Class<T> type

    Set<T> objects

    Project project

    Object parent

    InheritanceAwareContainer(Project project, Object parent, Class<T> type) {
        this.project = project
        this.type = type
        this.parent = parent
        this.objects = new LinkedHashSet<String, T>();
    }

    InheritanceAwareContainer(InheritanceAwareContainer<T> other) {
        this.project = other.project
        this.type = other.type
        this.parent = other.parent
        this.objects = new LinkedHashSet<T>(other.objects)
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        new InheritanceAwareContainer<T>(this)
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
            configureClosure = extractValueAsLazyClosure(args[0]).dehydrate()
        }
        // check whether there was inheritance used or any named parameters were passed
        else if(args instanceof Object[] && args.size()==2) {
            if(args[0] instanceof Map) {
                behavior = args[0]
            }
            configureClosure = extractValueAsLazyClosure(args[1]).dehydrate()
        }
        else {
            configureClosure = extractValueAsLazyClosure(args).dehydrate()
        }

        create(name, behavior, configureClosure)
    }

    T create(String name, Map behavior, Closure closure) {
        def object

        // inherit from different object if set
        if(behavior.inherits) {
            object = resolveParent(behavior.inherits).clone()
            object.name = name
        }
        // create brand new instance
        else {
            object = type.newInstance(name, project)
        }


        closure = closure.rehydrate(new GradleSpaceliftDelegate(), parent, object)

        // configure and store object
        object = project.configure(object, closure)
        // store configured object
        objects << object

        object
    }

    @Override
    public T configure(Closure configuration) {
        Closure config = extractValueAsLazyClosure(configuration).dehydrate()
        config.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
    }

    @Override
    public Iterator<T> iterator() {
        objects.iterator();
    }

    /**
     * Allows to use subscript operator on container
     * @param name
     * @return
     */
    public T getAt(String name) {
        try {
            for(T o: objects) {
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
    public boolean add(T object) {
        objects.add(object)
    }

    @Override
    public boolean addAll(Collection<? extends T> others) {
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
    public <X> X[] toArray(X[] a) {
        objects.toArray(a)
    }

    /**
     * Resolves reference by either string. If it gets direct reference, checks whether type is compatible
     * @param reference reference to be resolved
     * @return
     */
    private T resolveParent(def reference) {
        if(reference instanceof CharSequence) {
            getAt(reference.toString())
        }
        else if(type.isInstance(reference)) {
            (T) reference
        }
        else {
            throw new MissingPropertyException("Unable to reference ${type.getSimpleName()} by ${reference}")
        }
    }

}
