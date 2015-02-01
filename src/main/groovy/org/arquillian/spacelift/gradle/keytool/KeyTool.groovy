/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arquillian.spacelift.gradle.keytool
import org.arquillian.spacelift.Spacelift
import org.arquillian.spacelift.execution.Execution
import org.arquillian.spacelift.execution.ExecutionException
import org.arquillian.spacelift.gradle.utils.*
import org.arquillian.spacelift.process.CommandBuilder
import org.arquillian.spacelift.process.ProcessResult
import org.arquillian.spacelift.task.os.CommandTool

/**
 * Keytool
 *
 * @author <a href="asaleh@redhat.com">Adam Saleh</a>
 *
 */
public class KeyTool extends CommandTool {

    protected String command;
    protected HashMap<String,String> opts = new HashMap<String,String>();

    KeyTool setAlias(String alias){
        this.opts.put("alias", alias)
        this
    }

    KeyTool setKeystore(String keystore){
        this.opts.put("keystore", keystore)
        this
    }

    KeyTool setKeypass(String keypass){
        this.opts.put("keypass", keypass)
        this
    }

    KeyTool setTrustcacerts(){
        this.opts.put("trustcacerts","")
        this
    }

    KeyTool setStorepass (String storepass){
        this.opts.put("storepass", storepass)
        this
    }

    KeyTool cmdDelete(){
        command = "delete"
        this
    }

    KeyTool copy() {
        KeyTool n = new KeyTool()
        n.command = this.command
        n.opts = (HashMap<String,String>) this.opts.clone()
        n
    }

    KeyTool cmdExport(String filepath){
        command = "export"
        this.opts.put("file", filepath)
        this
    }

    KeyTool cmdImport(String filepath){
        command = "import"
        this.opts.put("file", filepath)
        this.opts.put("noprompt", "")
        this
    }

    KeyTool cmdGenKeyPair(String keyalg, String validity, String dname){
        command = "genkeypair";
        this.opts.put("keyalg",keyalg);
        this.opts.put("validity",validity);
        this.opts.put("dname",dname);
        this
    }

    KeyTool cmdGenKeyPair(String keyalg, String validity, String dname, String ext){
        command = "genkeypair";
        this.opts.put("keyalg",keyalg);
        this.opts.put("validity",validity);
        this.opts.put("dname",dname);
        this.opts.put("ext",ext);
        this
    }

    KeyTool keytoolAsPreset(KeyTool preset){
        for(Map.Entry<String, String> entry: preset.opts.entrySet()) {
            this.opts.put(entry.getKey(), entry.getValue())
        }
        this
    }

    protected void buildKeytoolCommand(){
        CommandTool t =  Spacelift.task("keytool");
    	CommandBuilder builder = t.commandBuilder;

        builder.parameter("-"+command);
        for(Map.Entry<String, String> entry: opts.entrySet()) {
            builder.parameter("-"+entry.getKey());
            if(entry.getValue()!=null && entry.getValue()!=""){
                builder.parameter(entry.getValue());
            }
        }
       if(this.commandBuilder!=null){
    	   for(String param : commandBuilder.build().getParameters()){
    		   builder.parameter(param);
    	   }
       }
       this.commandBuilder = builder;
    }

    @Override
    public Execution<ProcessResult> execute() throws ExecutionException {
    	this.buildKeytoolCommand();
        return super.execute();
    }
}
