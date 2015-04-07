package org.arquillian.spacelift.gradle.configuration

import org.junit.Test

import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.CoreMatchers.nullValue
import static org.hamcrest.core.Is.is
import static org.hamcrest.core.IsSame.sameInstance
import static org.junit.Assert.assertThat

/**
 * Created by tkriz on 25/03/15.
 */
class BuiltinConfigurationItemConvertersTest {

    @Test
    void "Test boolean converter"() {
        def converter = BuiltinConfigurationItemConverters.getConverter(Boolean)

        assertThat(converter.toString(Boolean.FALSE), is('false'))
        assertThat(converter.toString(Boolean.TRUE), is('true'))

        assertThat(converter.toString(false), is('false'))
        assertThat(converter.toString(true), is('true'))

        assertThat(converter.fromString('true'), is(true))
        assertThat(converter.fromString('false'), is(false))

        assertThat(converter.fromString(converter.toString(true)), is(true))
        assertThat(converter.fromString(converter.toString(false)), is(false))

        assertThat(converter.toString(converter.fromString('true')), is('true'))
        assertThat(converter.toString(converter.fromString('false')), is('false'))
    }

    @Test
    void "Test byte converter"() {
        def converter = BuiltinConfigurationItemConverters.getConverter(Byte)

        for(byte i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++) {
            assertThat(converter.fromString(converter.toString(i)), is(i))
        }
    }

    @Test
    void "Test short converter"() {
        def converter = BuiltinConfigurationItemConverters.getConverter(Short)

        for(short i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
            assertThat(converter.fromString(converter.toString(i)), is(i))
        }
    }

    @Test
    void "Test int converter"() {
        def converter = BuiltinConfigurationItemConverters.getConverter(Integer)

        for(int i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
            assertThat(converter.fromString(converter.toString(i)), is(i))
        }
    }


    @Test
    void "Test long converter"() {
        def converter = BuiltinConfigurationItemConverters.getConverter(Long)

        for(long i = Short.MIN_VALUE; i < Short.MAX_VALUE; i++) {
            assertThat(converter.fromString(converter.toString(i)), is(i))
        }
    }

    @Test
    void "Test boolean array converter"() {
        def converter = BuiltinConfigurationItemConverters.getConverter(Boolean[])

        Boolean[] arrayRepresentation = [true, false, 1, 0]
        def stringRepresentationDefault = '["true","false","true","false"]'
        def stringRepresentationShort = '[true, false, true, false]'

        assertThat(converter.fromString(stringRepresentationDefault), is(arrayRepresentation))
        assertThat(converter.fromString(stringRepresentationShort), is(arrayRepresentation))

        assertThat(converter.toString(arrayRepresentation), is(stringRepresentationDefault))

        assertThat(converter.fromString(converter.toString(arrayRepresentation)), is(arrayRepresentation))
        assertThat(converter.toString(converter.fromString(stringRepresentationDefault)), is(stringRepresentationDefault))
        assertThat(converter.toString(converter.fromString(stringRepresentationShort)), is(stringRepresentationDefault))
    }

    @Test
    void "Test integer array converter"() {
        def converter = BuiltinConfigurationItemConverters.getConverter(Integer[])

        Integer[] arrayRepresentation = [-100, 0, 1, 400]
        def stringRepresentationDefault = '["-100","0","1","400"]'
        def stringRepresentationShort = '[-100, 0, 1, 400]'

        assertThat(converter.fromString(stringRepresentationDefault), is(arrayRepresentation))
        assertThat(converter.fromString(stringRepresentationShort), is(arrayRepresentation))

        assertThat(converter.toString(arrayRepresentation), is(stringRepresentationDefault))

        assertThat(converter.fromString(converter.toString(arrayRepresentation)), is(arrayRepresentation))
        assertThat(converter.toString(converter.fromString(stringRepresentationDefault)), is(stringRepresentationDefault))
        assertThat(converter.toString(converter.fromString(stringRepresentationShort)), is(stringRepresentationDefault))
    }

    @Test
    void "Test string array converter"() {
        def converter = BuiltinConfigurationItemConverters.getConverter(String[])

        def arrayRepresentation = ["Hello", "World", ", Peace"].toArray(new String[0])
        def stringRepresentation = '["Hello","World",", Peace"]'

        assertThat(converter.toString(arrayRepresentation), is(stringRepresentation))
        assertThat(converter.fromString(stringRepresentation), is(arrayRepresentation))

        assertThat(converter.fromString(converter.toString(arrayRepresentation)), is(arrayRepresentation))
        assertThat(converter.toString(converter.fromString(stringRepresentation)), is(stringRepresentation))
    }

    @Test
    void "Test class converter"() {
        def converter = new BuiltinConfigurationItemConverters.ClassConfigurationItemConverter()

        def builtinConfigurationItemConvertersClassName = 'org.arquillian.spacelift.gradle.configuration.BuiltinConfigurationItemConverters'
        def classConfigurationItemConverterClassName = 'org.arquillian.spacelift.gradle.configuration.BuiltinConfigurationItemConverters$ClassConfigurationItemConverter'

        assertThat(converter.toString(BuiltinConfigurationItemConverters), is(builtinConfigurationItemConvertersClassName))
        assertThat(converter.toString(BuiltinConfigurationItemConverters.ClassConfigurationItemConverter),
                is(classConfigurationItemConverterClassName))

        assertThat(converter.fromString(builtinConfigurationItemConvertersClassName), sameInstance(BuiltinConfigurationItemConverters))
        assertThat(converter.fromString(classConfigurationItemConverterClassName),
                sameInstance(BuiltinConfigurationItemConverters.ClassConfigurationItemConverter))

        assertThat(converter.fromString(converter.toString(BuiltinConfigurationItemConverters)), sameInstance(BuiltinConfigurationItemConverters))
        assertThat(converter.fromString(converter.toString(BuiltinConfigurationItemConverters.ClassConfigurationItemConverter)),
                sameInstance(BuiltinConfigurationItemConverters.ClassConfigurationItemConverter))

        assertThat(converter.toString(converter.fromString(builtinConfigurationItemConvertersClassName)),
                is(builtinConfigurationItemConvertersClassName))
        assertThat(converter.toString(converter.fromString(classConfigurationItemConverterClassName)),
                is(classConfigurationItemConverterClassName))
    }

    @Test
    void "Test builtin converters can be resolved"() {
        assertThat(BuiltinConfigurationItemConverters.getConverter(Boolean), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(Byte), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(Short), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(Integer), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(Long), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(CharSequence), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(String), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(Class), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(File), is(not(nullValue())))
    }

    @Test
    void "Test builtin array converters can be resolved"() {
        assertThat(BuiltinConfigurationItemConverters.getConverter(Boolean[].class), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(Byte[].class), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(Short[].class), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(Integer[].class), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(Long[].class), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(CharSequence[].class), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(String[].class), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(Class[].class), is(not(nullValue())))
        assertThat(BuiltinConfigurationItemConverters.getConverter(File[].class), is(not(nullValue())))
    }

}
