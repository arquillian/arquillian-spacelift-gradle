package org.arquillian.spacelift.gradle

import groovy.transform.CompileStatic

/**
 * Created by tkriz on 22/04/15.
 */
@CompileStatic
class MyOwnDefaultTestChild extends DefaultTest {

    DeferredValue<String> randomString = DeferredValue.of(String.class).from("Hello world!")

    MyOwnDefaultTestChild(String testName, Object parent) {
        super(testName, parent)
    }

    MyOwnDefaultTestChild(String testName, DefaultTest other) {
        super(testName, other)
    }
}
