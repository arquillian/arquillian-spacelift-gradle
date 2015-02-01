package org.arquillian.spacelift.gradle;

import groovy.lang.Closure;

import org.arquillian.spacelift.Spacelift
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.arquillian.spacelift.gradle.android.AndroidSdkInstallation;
import org.arquillian.spacelift.gradle.android.AndroidSdkOptForStats
import org.arquillian.spacelift.gradle.android.AndroidSdkUpdater
import org.arquillian.spacelift.gradle.arquillian.ArquillianXmlUpdater
import org.junit.Ignore;
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

public class SpaceliftToolFromInstallationTest {

    @Test
    void "android android sdk and provide adroid tool"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.ext.set("androidTargets", [
            "19",
            "android-19",
            "Google Inc.:Google APIs (x86 System Image):21"
        ])

        project.spacelift {
            workspace = new File(System.getProperty("user.dir"), "workspace")
            installations {
                androidSdk {
                    product 'aerogear'
                    version '1.0.0'
                    remoteUrl ([ linux:"http://dl.google.com/android/android-sdk_r24.0.2-linux.tgz",
                        windows:"http://dl.google.com/android/android-sdk_r24.0.2-windows.zip",
                        mac:"http://dl.google.com/android/android-sdk_r24.0.2-macosx.zip"
                    ])
                    fileName ([ linux:"android-sdk_r24.0.2-linux.tgz",
                        windows:"android-sdk_r24.0.2-windows.zip",
                        mac:"android-sdk_r24.0.2-macosx.zip"
                    ])
                    home ([ linux:"android-sdk-linux",
                        windows:"android-sdk-windows",
                        mac:"android-sdk-macosx"
                    ])
                    // tools provided by installation
                    tools {
                        android {
                            command ([
                                linux: ["${home}/tools/android"],
                                windows: [
                                    "cmd.exe",
                                    "/C",
                                    "${home}/tools/android.bat"
                                ]
                            ])
                        }
                    }
                    // actions performed after extraction
                    postActions {
                        // fix executable flags
                        project.ant.chmod(dir: "${home}/tools", perm:"a+x", includes:"*", excludes:"*.txt")

                        // update Android SDK, download / update each specified Android SDK version
                        // This is disabled by default as it takes too long to be executed
                        //project.androidTargets.each { v ->
                        //    Spacelift.task(AndroidSdkUpdater).target(v).execute().await()
                        //}

                        // opt out for stats
                        Spacelift.task(AndroidSdkOptForStats).execute().await()

                        // update arquillian.xml files with Android homes
                        Spacelift.task([
                            androidHome: "${home}",
                            androidSdkHome: "${project.spacelift.workspace}"
                        ], ArquillianXmlUpdater).dir(project.spacelift.workspace).container('android').execute().await()
                    }
                }
            }
        }

        project.spacelift.installations.each { installation ->  installation.install(project.logger) }

        // find android tool
        def androidTool = Spacelift.task("android")
        assertThat "Android tool is available after installation", androidTool, is(notNullValue())

        assertThat "Android tool location is installed into workspace, which is in local directory", androidTool.toString(), containsString("${project.spacelift.workspace}")
    }

    @Test
    void "install from android sdk installation definition"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            workspace = new File(System.getProperty("user.dir"), "workspace")
            installations {
                androidSdk(from:AndroidSdkInstallation) {
                    // define AndroidTargets, alternative syntax is just list of name and use default abi
                    androidTargets ([
                        [name:"19", abi:"default/x86"],
                        [name:"android-19"],
                        [name:"Google Inc.:Google APIs (x86 System Image):21"]
                    ])
                    tools {
                        monitor {
                            // additional tool provided by installation
                            command "${home}/tools/monitor"
                        }
                    }
                    createAvds { false }
                }
            }
        }

        // on purpose, we are not installing here as this installation will download zillion of data
        // from internet, just verify that previous manual definition installed the SDK and tools are
        // properly registered
        project.spacelift.installations.each { Installation installation ->
            if(!installation.isInstalled()) {
                installation.install(project.logger)
            }
            else {
                installation.registerTools(Spacelift.registry())
            }
        }

        assertThat project.spacelift.installations['androidSdk'].androidTargets, is(notNullValue())
        assertThat project.spacelift.installations['androidSdk'].androidTargets.size(), is(3)
        assertThat project.spacelift.installations['androidSdk'].androidTargets[0].toString(), containsString("AndroidTarget: 19 - default/x86")

        // find android and adb tools
        def androidTool = Spacelift.task("android")
        assertThat "Android tool is available after installation", androidTool, is(notNullValue())
        assertThat "Android tool location is installed into workspace, which is in local directory", androidTool.toString(), containsString("${project.spacelift.workspace}")
        // find adb tool
        def adbTool = Spacelift.task("adb")
        assertThat "Adb tool is available after installation", adbTool, is(notNullValue())
        assertThat "Adb tool location is installed into workspace, which is in local directory", adbTool.toString(), containsString("${project.spacelift.workspace}")
        // find emulator tool
        def emulatorTool = Spacelift.task("emulator")
        assertThat "Emulator tool is available after installation", emulatorTool, is(notNullValue())
        assertThat "Emulator tool location is installed into workspace, which is in local directory", emulatorTool.toString(), containsString("${project.spacelift.workspace}")

        def monitorTool = Spacelift.task("monitor")
        assertThat "Monitor tool is available after installation", monitorTool, is(notNullValue())
        assertThat "Monitor tool location is installed into workspace, which is in local directory", monitorTool.toString(), containsString("${project.spacelift.workspace}")

    }

    @Test
    void "android targets are always collection"() {
        Project project = ProjectBuilder.builder().build()

        project.apply plugin: 'spacelift'

        project.spacelift {
            workspace = new File(System.getProperty("user.dir"), "workspace")
            installations {
                androidSdk(from:AndroidSdkInstallation) {
                    // define AndroidTargets
                    androidTargets "19"
                }
            }
        }

        // on purpose, we are not installing here as this installation will download zillion of data
        // from internet, just verify that previous manual definition installed the SDK and tools are
        // properly registered
        project.spacelift.installations.each { Installation installation ->
            if(!installation.isInstalled()) {
                installation.install(project.logger)
            }
            else {
                installation.registerTools(Spacelift.registry())
            }
        }

        assertThat project.spacelift.installations['androidSdk'].androidTargets, is(notNullValue())
        assertThat project.spacelift.installations['androidSdk'].androidTargets.size(), is(1)
        assertThat project.spacelift.installations['androidSdk'].androidTargets[0].toString(), containsString("AndroidTarget: 19")

        // find android and adb tools
        def androidTool = Spacelift.task("android")
        assertThat "Android tool is available after installation", androidTool, is(notNullValue())
        assertThat "Android tool location is installed into workspace, which is in local directory", androidTool.toString(), containsString("${project.spacelift.workspace}")

    }
}
