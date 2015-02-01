package org.arquillian.spacelift.gradle.xml

import org.arquillian.spacelift.gradle.GradleSpaceliftDelegate
import org.arquillian.spacelift.task.Task
import org.gradle.api.AntBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class XmlUpdater extends Task<Object, File>{

    protected static final Logger log = LoggerFactory.getLogger('Xml')

    def static backupCounter = 0

    private File file
    private AntBuilder ant

    XmlUpdater() {
        this.ant = new GradleSpaceliftDelegate().project().ant
    }

    XmlUpdater file(File file) {
        this.file = file
        this
    }

    @Override
    protected File process(Object xml) throws Exception {
        // backup previous configuration
        ant.copy(file: "${file}", tofile: "${file}.backup${++backupCounter}")

        file.withPrintWriter("UTF-8") { writer ->
            def printer = new XmlNodePrinter(writer, '    ');
            printer.setPreserveWhitespace(true)
            printer.print(xml)
        }

        return file
    }
}
