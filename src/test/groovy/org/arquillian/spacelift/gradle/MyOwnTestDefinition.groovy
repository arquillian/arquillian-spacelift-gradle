package org.arquillian.spacelift.gradle

import groovy.transform.CompileStatic

import org.gradle.api.Project
import org.slf4j.Logger


@CompileStatic
class MyOwnTestDefinition extends BaseContainerizableObject<MyOwnTestDefinition> implements Test {

    DeferredValue<Object> myDSL = DeferredValue.of(Object.class)

    MyOwnTestDefinition(String name, Object parent) {
        super(name, parent)
    }

    MyOwnTestDefinition(String name, MyOwnTestDefinition other) {
        super(name, other)
        this.myDSL = other.@myDSL.copy()
    }

    @Override
    public MyOwnTestDefinition clone(String name) {
        return new MyOwnTestDefinition(name, this)
    }

    @Override
    public void executeTest(Logger logger) {
        Object value = myDSL.resolve()
        assert value != null
    }
}