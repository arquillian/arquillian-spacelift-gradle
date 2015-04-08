package org.arquillian.spacelift.gradle.configuration

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import groovy.transform.CompileStatic

/**
 * Created by tkriz on 25/03/15.
 */
@CompileStatic
class BuiltinConfigurationItemConverters {

    private static
    final Map<Class<?>, ConfigurationItemConverter<?>> BUILTIN_CONVERTERS =
            (Map<Class<?>, ConfigurationItemConverter<?>>) [
                    (Boolean)     : new SimpleConfigurationItemConverter<Boolean>() {
                        @Override
                        Boolean fromString(String value) throws ConversionException {
                            return Boolean.valueOf(value)
                        }
                    },
                    (Byte)        : new SimpleConfigurationItemConverter<Byte>() {
                        @Override
                        Byte fromString(String value) throws ConversionException {
                            return Byte.valueOf(value)
                        }
                    },
                    (Short)       : new SimpleConfigurationItemConverter<Short>() {
                        @Override
                        Short fromString(String value) throws ConversionException {
                            return Short.valueOf(value)
                        }
                    },
                    (Integer)     : new SimpleConfigurationItemConverter<Integer>() {
                        @Override
                        Integer fromString(String value) throws ConversionException {
                            return Integer.valueOf(value)
                        }
                    },
                    (Long)        : new SimpleConfigurationItemConverter<Long>() {
                        @Override
                        Long fromString(String value) throws ConversionException {
                            return Long.valueOf(value)
                        }
                    },
                    (CharSequence): new SimpleConfigurationItemConverter<CharSequence>() {
                        @Override
                        CharSequence fromString(String value) throws ConversionException {
                            return value
                        }
                    },
                    (String)      : new StringConfigurationItemConverter(),
                    (Class)       : new ClassConfigurationItemConverter(),
                    (File)        : new FileConfigurationItemConverter()
            ]

    static <T> ConfigurationItemConverter<T> getConverter(Class<T> type) {
        if (type.isArray()) {
            ConfigurationItemConverter<?> itemConverter = getConverter(type.getComponentType())
            return new ArrayConfigurationItemConverter<?>(itemConverter) as ConfigurationItemConverter<T>
        }

        ConfigurationItemConverter<T> converter = BUILTIN_CONVERTERS.get(type) as ConfigurationItemConverter<T>

        if (converter == null) {
            throw new RuntimeException("No suitable converter found for type: $type.")
        } else {
            return converter
        }
    }

    static class ArrayConfigurationItemConverter<CONVERTED_TYPE>
            implements ConfigurationItemConverter<CONVERTED_TYPE[]> {

        private final ConfigurationItemConverter<CONVERTED_TYPE> valueConverter

        ArrayConfigurationItemConverter(ConfigurationItemConverter<CONVERTED_TYPE> valueConverter) {
            this.valueConverter = valueConverter
        }

        @Override
        CONVERTED_TYPE[] fromString(String value) throws ConversionException {
            def array = new JsonParser().parse(value).getAsJsonArray()
            def output = []
            array.each {
                output.add(valueConverter.fromString(it.getAsString()))
            }
            return output.toArray() as CONVERTED_TYPE[]
        }

        @Override
        String toString(CONVERTED_TYPE[] value) {
            def jsonArray = new JsonArray()
            value.each { CONVERTED_TYPE element ->
                jsonArray.add(new JsonPrimitive(valueConverter.toString(element)))
            }
            return jsonArray.toString()
        }
    }

    static abstract class SimpleConfigurationItemConverter<CONVERTED_TYPE>
            implements ConfigurationItemConverter<CONVERTED_TYPE> {

        @Override
        String toString(CONVERTED_TYPE value) {
            return value.toString()
        }
    }

    static class StringConfigurationItemConverter implements ConfigurationItemConverter<String> {
        @Override
        String fromString(String value) throws ConversionException {
            return value
        }

        @Override
        String toString(String value) {
            return value
        }
    }

    static class ClassConfigurationItemConverter implements ConfigurationItemConverter<Class<?>> {

        @Override
        Class<?> fromString(String value) throws ConversionException {
            return Class.forName(value)
        }

        @Override
        String toString(Class<?> value) {
            return value.getName()
        }
    }

    static class FileConfigurationItemConverter implements ConfigurationItemConverter<File> {

        @Override
        File fromString(String value) throws ConversionException {
            return new File(value)
        }

        @Override
        String toString(File value) {
            return value.path
        }
    }

}
