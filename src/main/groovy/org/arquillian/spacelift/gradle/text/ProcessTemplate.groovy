package org.arquillian.spacelift.gradle.text

import groovy.text.GStringTemplateEngine
import org.arquillian.spacelift.task.Task

/**
 * Processor of GSTring templates. Takes a file as input and returns
 * processed file.
 *
 */
class ProcessTemplate extends Task<File, File> {

    static GStringTemplateEngine engine = new groovy.text.GStringTemplateEngine()

    private Map bindings = new LinkedHashMap()

    /**
     * Sets bindings for template evaluation. You can call this method multiple times.
     *
     * Latest bindings takes precedence
     *
     * @param bindings
     * @return
     */
    ProcessTemplate bindings(Map bindings) {

        Map nested = this.bindings

        bindings.each { key, value ->
            // create nested structure if key is using dot syntax so we can reference values via GStringImpl
            List<String> subkeys = key.toString().split('\\.')
            subkeys.eachWithIndex { subkey, i ->
                if(i==subkeys.size()-1) {
                    nested.put(subkey, value)
                }
                else if(nested.containsKey(subkey)) {
                    nested = nested.get(subkey)
                }
                else {
                    Map toNest = new LinkedHashMap()
                    nested.put(subkey, toNest)
                    nested = toNest

                }
            }
        }
        return this
    }

    @Override
    protected File process(File input) throws Exception {

        if(input==null || !input.exists() || !input.canRead()) {
            throw new IllegalArgumentException("Input file must exists and be readable")
        }

        def template = engine.createTemplate(input).make(bindings)

        File output = File.createTempFile("spacelift-template-eval", null)
        output.withWriter('UTF-8') { writer ->
            template.writeTo(writer)
        }

        return output
    }
}
