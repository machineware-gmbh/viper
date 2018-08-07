/******************************************************************************
 *                                                                            *
 * Copyright 2018 Jan Henrik Weinstock                                        *
 *                                                                            *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *     http://www.apache.org/licenses/LICENSE-2.0                             *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 *                                                                            *
 ******************************************************************************/

package org.vcml.session;

import java.util.ArrayList;

public class Command {
	
	private String name;
	
	private String desc;
	
	private int argc;
	
	private RemoteSerialProtocol protocol;
	
	private Module parent;
	
	public String getName() {
		return name;
	}
	
	public String getDesc() {
		return desc;
	}
	
	public int getArgc() {
		return argc;
	}
	
	public Command(RemoteSerialProtocol protocol, Module parent, String desc) {
		this.protocol = protocol;
		this.parent = parent;
		
		String[] info = desc.split(":", 3);
		this.name = info[0];
		this.argc = info.length > 1 ? Integer.parseInt(info[1]) : 0;
		this.desc = info.length > 2 ? info[2] : "no description available";
	}
	
	public String execute() throws SessionException {
		if (argc != 0)
			throw new SessionException("Not enough arguments");
		
		Response resp = protocol.command(RemoteSerialProtocol.EXEC, parent.getName(), getName());
		return resp.toString();
	}
	
	public String execute(String... args) throws SessionException {
		if (args.length < argc)
			throw new SessionException("Not enough arguments");
		
		ArrayList<String> fullArgs = new ArrayList<String>();
		fullArgs.add(RemoteSerialProtocol.EXEC);
		fullArgs.add(parent.getName());
		fullArgs.add(getName());
		for (String arg : args)
			fullArgs.add(arg);
		
		Response resp = protocol.command(fullArgs.toArray(new String[fullArgs.size()]));
		return resp.toString();
	}

}