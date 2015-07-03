package org.arquillian.spacelift.gradle.utils

import java.util.logging.Logger

import org.apache.commons.lang.SystemUtils
import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.task.Task
import org.arquillian.spacelift.task.os.CommandTool

/**
 * Helper class that will clean up environment by killing all processes that might collide with test execution
 */
class KillJavas extends Task<Object, Void>{

    def static final logger = Logger.getLogger(KillJavas.class.getName())

    // definition of what would be killed
    def processNames = [
        "org.jboss.Main",
        "org.jboss.as",
        "jboss-modules.jar",
        "surefire",
        "Selenium",
        "selenium-server-standalone"
    ]

    def processPorts = [4444, 14444, 8080, 9999]

    KillJavas addProcessNames(List<String> processNames) {
        if (processNames && !processNames.isEmpty()) {
            this.processNames.addAll(processNames)
        }
        this
    }

    KillJavas addProcessName(String processName) {
        if (processName) {
            processNames.add(processName)
        }
        this
    }

    KillJavas addProcessPort(int port) {
        if (port > 1024 && port < 65535) {
            processPorts.add(port)
        }
        this
    }

    KillJavas addProcessPort(String port) {
        try {
            addProcessPort(Integer.parseInt(port))
        } catch (NumberFormatException ex) {
            logger.warn(String.format("Unable to parse port %s to Integer", port))
        }
        this
    }

    KillJavas addProcessPorts(List<String> ports) {
        if (ports && !ports.isEmpty()) {
            ports.each { port -> addProcessPort(port) }
        }
        this
    }

    @Override
    protected Void process(Object input) throws Exception {
        try {
            ["-SIGTERM", "-9"].each { signal ->
                def totalKilled = 0

                logger.info("Sending kill ${signal} to all java processes named ${processNames}")

                processNames.each { proc ->
                    totalKilled += jpsKill(proc, signal)
                }

                logger.info("Sending kill ${signal} to all processes listening on ports ${processPorts}")
                processPorts.each { port ->
                    totalKilled += netstatKill(port, signal)
                }

                // wait for process to finish if not forced kill
                if(signal == "-SIGTERM" && totalKilled) {
                    sleep 10000
                }
            }
        }
        catch (Throwable e) {
            logger.warn("Cleanup environment failed with ${e.getMessage()}")
            e.printStackTrace()
        }

        return null
    }

    def executeBash(String command) {
        Spacelift.task(CommandTool).programName("bash").parameters("-c").splitToParameters(command).execute().await().output()
    }

    def executeCmd(String command) {
        Spacelift.task(CommandTool).programName("cmd").parameters("/C").splitToParameters(command).execute().await().output()
    }

    def jpsKill(String name, String signal) {
        def JPS_PATTERN = java.util.regex.Pattern.compile('^([0-9]+).*?' + name + '.*$')

        def pids = executeBash("jps -l").inject(new ArrayList()) { list, line ->
            def m = line =~ JPS_PATTERN
            if(m) {
                list << m[0][1]
            }
            list
        }

        def totalKilled = pids.inject(0) {total, pid ->
            kill(pid, signal)
            total++
        }

        totalKilled
    }

    def netstatKill(int port, String signal) {
        def NETSTAT_PATTERN = java.util.regex.Pattern.compile('^.*:' + port + ' .*?([0-9]+)/.*$')

        // for some reason injecting [] does not work
        def pids = executeBash("netstat -a -n -p").inject(new ArrayList()) { list, line ->
            def m = line =~ NETSTAT_PATTERN
            if(m) {
                list << m[0][1]
            }
            list
        }

        def totalKilled = pids.inject(0) {total, pid ->
            kill(pid, signal)
            total++
        }

        totalKilled
    }

    def kill(String pid, String signal) {

        if (!SystemUtils.IS_OS_WINDOWS) {
            executeBash("kill ${signal} ${pid}")
        }
        else {
            executeCmd("taskkill /F /T /PID ${pid}")
        }

        logger.info("Killed ${pid}")
    }
}
