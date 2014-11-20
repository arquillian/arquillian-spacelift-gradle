package org.arquillian.spacelift.gradle.android

import java.util.Collection
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import org.arquillian.spacelift.tool.Tool

class AndroidEmulatorStopper extends Tool<Object, Void> {

    private String device = "emulator-5554"

    @Override
    protected Collection<String> aliases() {
        ["android_emulator_stopper"]
    }

    def device(String device) {
        if (device && device.length() != 0) {
            this.device = device
        }
        this
    }

    @Override
    protected Void process(Object input) throws Exception {

        getExecutionService().execute(sendEmulatorCommand(extractPortFromDevice(device), "kill")).awaitAtMost(10, TimeUnit.SECONDS)

        null
    }

    private Callable<Boolean> sendEmulatorCommand(final int port, final String command) {
        return new Callable<Boolean>() {
            @Override
            public Boolean call() throws IOException {
                Socket socket = null
                BufferedReader input = null
                PrintWriter output = null

                try {
                    socket = new Socket("127.0.0.1", port)
                    output = new PrintWriter(socket.getOutputStream(), true)
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()))

                    String telnetOutputString = null

                    while ((telnetOutputString = input.readLine()) != null) {
                        if (telnetOutputString.equals("OK")) {
                            break
                        }
                    }

                    output.write(command)
                    output.write("\n")
                    output.flush()
                } finally {
                    try {
                        output.close()
                        input.close()
                        socket.close()
                    } catch (Exception e) {
                        // ignore
                    }
                }

                return true
            }
        }
    }

    private int extractPortFromDevice(String device) {
        if (device && device.length() != 0 && device.contains("-")) {
            return device.substring(device.lastIndexOf("-") + 1).toInteger()
        }

        throw new IllegalArgumentException("It seems the description of a device to parse the port from is of the bad format: " + device)
    }
}
