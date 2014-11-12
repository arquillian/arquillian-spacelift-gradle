package org.arquillian.spacelift.gradle

import org.gradle.api.Project
import org.slf4j.Logger

@Mixin(ValueExtractor)
class Test {

    // required by gradle to be defined
    final String name

    final String testName

    Closure execute

    Closure dataProvider = { [null]}

    Closure beforeSuite = {}

    Closure beforeTest = {}

    Closure afterSuite = {}

    Closure afterTest = {}

    Project project

    Test(String testName, Project project) {
        this.name = this.testName = testName
        this.project = project
    }

    def executeTest(Logger logger) {

        // before suite
        beforeSuite.rehydrate(new GradleSpaceliftDelegate(), this, this).call()

        // iterate through beforeTest, execute and afterTest based on data provider
        dataProvider.rehydrate(new GradleSpaceliftDelegate(), this, this).call().each { data ->

            if(data==null) {
                beforeTest.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
            }
            else {
                beforeTest.rehydrate(new GradleSpaceliftDelegate(), this, this).call(data)
            }

            if(data==null) {
                logger.lifecycle(":test:${name}")
                afterTest.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
            }
            else {
                logger.lifecycle(":test:${name} (${data})")
                execute.rehydrate(new GradleSpaceliftDelegate(), this, this).call(data)
            }
            if(data==null) {
                afterTest.rehydrate(new GradleSpaceliftDelegate(), this, this).call()
            }
            else {
                afterTest.rehydrate(new GradleSpaceliftDelegate(), this, this).call(data)
            }
        }

        // after suite
        afterSuite.rehydrate(new GradleSpaceliftDelegate(), this, this).call()

    }

    def execute(arg) {                
        this.execute = extractValueAsLazyClosure(arg).dehydrate()
        this.execute.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def dataProvider(arg) {
        this.dataProvider = extractValueAsLazyClosure(arg).dehydrate()
        this.dataProvider.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def beforeSuite(arg) {
        this.beforeSuite = extractValueAsLazyClosure(arg).dehydrate()
        this.beforeSuite.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def beforeTest(arg) {
        this.beforeTest = extractValueAsLazyClosure(arg).dehydrate()
        this.beforeTest.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def afterSuite(arg) {
        this.afterSuite = extractValueAsLazyClosure(arg).dehydrate()
        this.afterSuite.resolveStrategy = Closure.DELEGATE_FIRST
    }

    def afterTest(arg) {
        this.afterTest = extractValueAsLazyClosure(arg).dehydrate()
        this.afterTest.resolveStrategy = Closure.DELEGATE_FIRST
    }
}
