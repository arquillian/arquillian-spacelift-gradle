package org.arquillian.spacelift.gradle

import groovy.transform.CompileStatic

import org.gradle.api.Project
import org.slf4j.Logger


@CompileStatic
class MyOwnTestDefinition extends BaseContainerizableObject<MyOwnTestDefinition> implements Test {

    Closure myDSL = {}

    MyOwnTestDefinition(String name, Object parent) {
        super(name, parent)
    }

    MyOwnTestDefinition(String name, MyOwnTestDefinition other) {
        super(name, other)
        this.myDSL = (Closure) other.@myDSL.clone()
    }

    @Override
    public MyOwnTestDefinition clone(String name) {
        return new MyOwnTestDefinition(name, this)
    }

    @Override
    public void executeTest(Logger logger) {
        Object value = DSLUtil.resolve(myDSL, this)
        assert value != null
    }
}