package org.arquillian.spacelift.gradle

import org.gradle.api.Project

/**
 * An object that can be put into {@link InheritanceAwareContainer}
 *
 * Name must be unique across all objects in Spacelift DSL.
 *
 * Also, all objects must provide Constructor(String name, Object parent)
 *
 * @author kpiwko
 *
 * @param <REAL_TYPE>
 */
interface ContainerizableObject<REAL_TYPE extends ContainerizableObject<REAL_TYPE>> extends Cloneable {

    /**
     * Returns as unique name of the object
     * @return
     */
    String getName()

    /**
     * Clones the object under new unique name
     * @param name
     * @return
     */
    REAL_TYPE clone(String name)
}
