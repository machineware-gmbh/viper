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

public class Attribute {
	
	private String name;
	
	private String value;
	
	private RemoteSerialProtocol protocol;
	
	private void refresh() throws SessionException {
		Response resp = protocol.command(RemoteSerialProtocol.GETA, name);
		String[] val = resp.getValues("value");
		if (val.length == 0)
			throw new SessionException("Failed to read attribute " + name);
		this.value = val[0];
	}
	
	public Attribute(String name, String value) {
		this.name = name;
		this.value = value;
		this.protocol = null;
	}
	
	public Attribute(RemoteSerialProtocol protocol, String name) throws SessionException {
		this.name = name;
		this.protocol = protocol;
		
		refresh();
	}
	
	public boolean isEditable() {
		return protocol != null;
	}
	
	public String getName() {
		return name;
	}
	
	public String getBaseName() {
		String[] temp = name.split("\\.");
		return temp[temp.length - 1];
	}
	
	public String getValue() {
		return value;
	}
	
	public String getValuePretty() {
		if (value.isEmpty())
			return "<empty>";

		try {
			long val = Long.parseLong(value);
			String base = getBaseName();
			if (base.contains("addr")   ||
		        base.contains("size")   ||
		        base.contains("offset") ||
		        base.contains("R")      ||
		        base.contains("NPC")    ||
		        base.contains("PPC")    ||
		        base.contains("SR")     ||
		        base.contains("CID"))
				return String.format("0x%08x", val);
			return value;
		} catch (NumberFormatException e) {
			//return "\"" + value + "\"";
			return value;
		}
	}
	
	public void setValue(String newValue) throws SessionException {
		if (!isEditable() || (newValue == value))
			return;
		
		protocol.command(RemoteSerialProtocol.SETA, name, newValue);
		refresh();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Attribute))
			return false;
		return getName().equals(((Attribute)other).getName());
	}
	
	@Override
	public String toString() {
		return getName();
	}

}
