package org.arquillian.spacelift.gradle

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException;

class DSLUtilTest {

    private Closure listValidator = { int size, List matches, Closure content ->
        assertThat content(), is(instanceOf(List))
        assertThat content().size(), is(size)
        assertThat content(), hasItems(matches.toArray())
    }

    private Closure stringValidator = { String expectedContent, Closure content ->
        assertThat content(), isA(String)
        assertThat content(), is(expectedContent)
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    void "pass string"() {
        Closure c = passString "myString"
        Closure d = passString 'myString'
        Closure e = passString "${d()}"
        Closure f = passString { "myString" }

        def v = stringValidator.curry("myString")
        v(c); v(d); v(e); v(f)
    }

    @Test
    void "pass non-string"() {
        Closure c = passBoolean false

        assertThat c(), is(false)
    }

    @Test
    void "pass multiple strings"() {
        Closure c = passMultipleStrings "alfa", "beta", "gama"
        // following call is not possible as this would be handled by propertyMissing
        //Closure invalidC = passMultipleStrings ["alfa", "beta", "gama"]
        Closure d = passMultipleStrings (["alfa", "beta", "gama"])
        Closure e = passMultipleStrings Arrays.asList("alfa", "beta", "gama")
        Closure f = passMultipleStrings "alfa", "beta"

        def v = listValidator.curry(3, ["alfa", "beta", "gama"])
        v(c); v(d); v(e)
        v = listValidator.curry(2, ["alfa", "beta"])
        v(f)
    }

    @Test
    void "pass multiple strings as variables"() {

        Closure c = passMultipleStrings alfa, beta, gama
        Closure d = passMultipleStrings ([alfa, beta, gama])
        Closure e = passMultipleStrings Arrays.asList(alfa, beta, gama)

        def v = listValidator.curry(3, ["alfa", "beta", "gama"])
        v(c); v(d); v(e)
    }

    @Test
    void "ignore behavior for value parsing"() {

        Closure c = passMapAndString("foo":"bar") { "abc" }
        Closure d = passMapAndStringAlt "foo":"bar", { "abc" }
        Closure e = passMapAndStringRaw "foo":"bar", "abc"
        def abc = "abc"
        Closure g = passMapAndStringDirect "bar":"baz", "foo":"bar", { abc }

        def v = stringValidator.curry("abc")
        v(c); v(d); v(e); v(g)

        Closure f = passMapAndStringReverse "abc", "abcd", "foo":"bar"
        Closure h = passMapAndStringReverse (["abc", "abcd"], "foo":"bar")
        v = listValidator.curry(2, ["abc", "abcd"])
        v(f); v(h)
    }

    @Test
    void "platform dependent content"() {
        Closure c = passOSMap linux:"abc", windows:"abc", mac:"abc", solaris: "abc"
        Closure d = passBehavAndOSMap (["foo":"val"], [linux:"abc", windows:"abc", mac:"abc", solaris: "abc"])


        def v = stringValidator.curry("abc")
        v(c); v(d)
    }

    @Test
    void "map and list are invalid values"() {
        exception.expect(IllegalArgumentException)
        Closure e = passInvalid([linux:"abc", windows:"abc", mac:"abc", solaris: "abc"], ["foo-as-list"])
    }

    @Test
    void "two maps are invalid if first is not behaviors"() {
        exception.expect(IllegalArgumentException)
        Closure d = passBehavAndOSMap ([linux:"abc", windows:"abc", mac:"abc", solaris: "abc"], ["foo":"val"] )
    }


    def methodMissing(String name, args) {
        //System.err.println("Called ${name}(" + args.collect { it.class?.simpleName}.join(', ') +")")
        return DSLUtil.deferredValue(args)
    }

    def propertyMissing(String name) {
        return name
    }
}
