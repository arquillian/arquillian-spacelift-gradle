package org.arquillian.spacelift.gradle.configuration

/**
 * Property converter has to be stateless!
 *
 * @param < CONVERTED_TYPE >
 */
interface ConfigurationItemConverter<CONVERTED_TYPE> {

    /**
     * Tries to convert the string value into the type.
     * @param value String representation of the value.
     * @return
     *
     * @throws ConversionException If the string is malformed, {@link ConversionException} is thrown.
     */
    CONVERTED_TYPE fromString(String value) throws ConversionException;

    /**
     * Converts the type instance into string. This method has to be consistent with {@link #fromString(java.lang.String)} and calling:
     *
     * <code>
     *     CONVERTED_TYPE instance1 = << instance of the type with some state >>;
     *     CONVERTED_TYPE instance2 = fromString(toString(instance1));
     * </code>
     *
     * will result in instance2 having the same state as instance1.
     *
     * @param value
     * @return
     */
    String toString(CONVERTED_TYPE value);
}