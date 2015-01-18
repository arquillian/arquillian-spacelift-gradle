package org.arquillian.spacelift.gradle

import org.gradle.api.Project
import org.slf4j.Logger

class MyOwnTestDefinition extends BaseContainerizableObject<MyOwnTestDefinition> implements Test {

    Closure myDSL = {}

    MyOwnTestDefinition(String name, Project project) {
        super(name, project)
    }

    MyOwnTestDefinition(String name, MyOwnTestDefinition other) {
        super(name, other)
        this.myDSL = other.@myDSL.clone()
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