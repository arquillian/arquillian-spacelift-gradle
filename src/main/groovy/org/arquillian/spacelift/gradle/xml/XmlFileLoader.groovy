package org.arquillian.spacelift.gradle.xml

import org.arquillian.spacelift.task.Task

class XmlFileLoader extends Task<File, Object> {

    @Override
    protected Object process(File input) throws Exception {
        new XmlParser(false, false).parse(input)
    }
}
