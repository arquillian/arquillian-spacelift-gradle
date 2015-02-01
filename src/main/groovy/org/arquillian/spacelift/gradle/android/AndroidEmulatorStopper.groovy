package org.arquillian.spacelift.gradle.android

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import org.arquillian.spacelift.task.Task

class AndroidEmulatorStopper extends Task<Object, Boolean> {

    private String device = "emulator-5554"

    private int timeout = 10 // in seconds

    def device(String device) {
        if (device && device.length() != 0) {
            this.device = device
        }
        this
    }

    def timeout(int timeout) {
        if (timeout > 0) {
            this.timeout = timeout
        }
        this
    }

    def timeout(String timeout) {
        this.timeout(timeout.toInteger())
    }

    @Override
    protected Boolean process(Object input) throws Exception {
        getExecutionService().execute(sendEmulatorCommand(extractPortFromDevice(device), "kill")).awaitAtMost(timeout, TimeUnit.SECONDS)
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
