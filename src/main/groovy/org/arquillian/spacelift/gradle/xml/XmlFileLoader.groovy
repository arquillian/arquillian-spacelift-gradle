package org.arquillian.spacelift.gradle.xml

import groovy.util.Node;

import java.io.File;

import org.arquillian.spacelift.execution.Task

class XmlFileLoader extends Task<File, Object> {

    @Override
    protected Object process(File input) throws Exception {
        new XmlParser(false, false).parse(input)
    }
}
