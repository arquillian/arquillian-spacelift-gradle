package org.arquillian.spacelift.gradle;

import org.arquillian.spacelift.execution.Tasks
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.arquillian.spacelift.gradle.android.AndroidSdkOptForStats
import org.arquillian.spacelift.gradle.android.AndroidSdkUpdater
import org.arquillian.spacelift.gradle.arquillian.ArquillianXmlUpdater
import org.junit.Test

import static org.hamcrest.CoreMatchers.*
import static org.junit.Assert.assertThat

public class SpaceliftToolFromInstallationTest {

    @Test
    public void installAndroidSDKAndProvideTool() {
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
                    remoteUrl ([ linux:"http://dl.google.com/android/android-sdk_r23.0.2-linux.tgz",
                        windows:"http://dl.google.com/android/android-sdk_r23.0.2-windows.zip",
                        mac:"http://dl.google.com/android/android-sdk_r23.0.2-macosx.zip"
                    ])
                    fileName ([ linux:"android-sdk_r23.0.2-linux.tgz",
                        windows:"android-sdk_r23.0.2-windows.zip",
                        mac:"android-sdk_r23.0.2-macosx.zip"
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
                        //    Tasks.prepare(AndroidSdkUpdater).target(v).execute().await()
                        //}

                        // opt out for stats
                        Tasks.prepare(AndroidSdkOptForStats).execute().await()

                        // update arquillian.xml files with Android homes
                        Tasks.chain([
                            androidHome: "${home}",
                            androidSdkHome: "${project.spacelift.workspace}"
                        ], ArquillianXmlUpdater).dir(project.spacelift.workspace).container('android').execute().await()
                    }
                }
            }
        }

        project.spacelift.installations.each { installation ->  installation.install(project.logger) }

        // find android tool
        def androidTool = GradleSpacelift.tools("android")
        assertThat "Android tool is available after installation", androidTool, is(notNullValue())

        assertThat "Android tool location is installed into workspace, which is in local directory", androidTool.toString(), containsString("${project.spacelift.workspace}")
    }
}
