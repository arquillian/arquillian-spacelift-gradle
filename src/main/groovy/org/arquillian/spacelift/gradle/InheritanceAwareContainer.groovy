package org.arquillian.spacelift.gradle

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
class InheritanceAwareContainer<T> implements Iterable<T>, Configurable<T>, ValueExtractor {

    Class<T> type
    
    Set<T> objects

    Project project
    
    Object parent

    InheritanceAwareContainer(Project project, Object parent, Class<T> type) {
        this.project = project
        this.type = type
        this.parent = parent
        this.objects = new HashSet<T>();
    }

    /**
     * This method dynamically creates an object of type <T>.
     * Object must define a constructor of type (name, project) 
     * @param name name of object to be created
     * @param args
     * @return
     */
    def methodMissing(String name, args) {
                
        // unwrap array in case there is single argument
        if(args instanceof Object[] && args.size()==1) {
            args = args[0]
        }
        
        def object = project.gradle.services.get(Instantiator).newInstance(type, name, project)
        def configureClosure = extractValueAsLazyClosure(args).dehydrate()
        configureClosure.resolveStrategy = Closure.DELEGATE_FIRST
        configureClosure = configureClosure.rehydrate(new GradleSpaceliftDelegate(), parent, object)
        
        // configure object
        object = project.configure(object, configureClosure)
        // store configured object
        objects << object
        return object        
    }

    @Override
    public T configure(Closure configuration) {
        Closure config = extractValueAsLazyClosure(configuration).dehydrate()
        config.resolveStrategy = Closure.DELEGATE_FIRST
        config.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
    }
  
    @Override
    public Iterator<T> iterator() {
        return objects.iterator();
    }

}
