package org.arquillian.spacelift.gradle.text

import static org.junit.Assert.*
import static org.hamcrest.CoreMatchers.*

import org.arquillian.spacelift.Spacelift
import org.junit.Test

class ProcessTemplateTest {

    @Test
    void "process simple template"() {
        File input = createTemplate('''Hello ${foobar}!''')
        File output = Spacelift.task(input, ProcessTemplate).bindings([foobar: "world"]).execute().await()

        assertThat output.text, is('Hello world!')
    }

    @Test
    void "template simple method call"() {
        File input = createTemplate('''Hello ${foobar.length()}!''')
        File output = Spacelift.task(input, ProcessTemplate).bindings([foobar: "world"]).execute().await()

        assertThat output.text, is('Hello 5!')
    }

    @Test
    void "template nested property call"() {
        File input = createTemplate('''Hello ${foo.bar}!''')
        File output = Spacelift.task(input, ProcessTemplate).bindings([foo: [ bar: "world"]]).execute().await()

        assertThat output.text, is('Hello world!')
    }

    @Test
    void "template dotted property call"() {
        File input = createTemplate('''Hello ${foo.bar}!''')
        File output = Spacelift.task(input, ProcessTemplate).bindings(["foo.bar": "world"]).execute().await()

        assertThat output.text, is('Hello world!')
    }

    @Test
    void "template double bindings call"() {
        File input = createTemplate('''${foo.greeting} ${foo.bar} from ${tool}!''')
        File output = Spacelift.task(input, ProcessTemplate)
                .bindings([foo:[greeting: "Hello"]])
                .bindings(["foo.bar": "world", tool: "Space"])
                .bindings([tool: "Spacelift"])
                .execute().await()

        assertThat output.text, is('Hello world from Spacelift!')
    }
    File createTemplate(String content) {
        File output =  File.createTempFile("spacelift-tmp-test", null)
        output.withWriter('UTF-8') { out ->
            out << content
        }
        return output
    }
}
