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
import java.util.List;

public class Response {
	
	String response;
	
	private class KeyValuePair {
		public String key;
		public String val;
		
		public KeyValuePair(String key, String val) {
			this.key = key;
			this.val = val;
		}
	}
	
	List<KeyValuePair> entries;
	
	public Response(String resp) throws SessionException {
		this.response = resp;
		this.entries = new ArrayList<KeyValuePair>();
		
		if (response.isEmpty())
			throw new SessionException("Command not supported");
		if (response.startsWith("ERROR,"))
			throw new SessionException(response.substring(6));
		if (response.startsWith("OK"))
			response = response.substring(2);
		if (response.startsWith(","))
			response = response.substring(1);
		
		String[] token = response.split(",");
		for (String entry : token) {
			String[] data = entry.split(":", 2);
			String value = data.length > 1 ? data[1] : "";
			entries.add(new KeyValuePair(data[0], value));
		}
	}

	public String toString() {
		return response;
	}

	public String[] getValues(String key) {
		ArrayList<String> list = new ArrayList<String>();
		for (KeyValuePair pair : entries) {
			if (pair.key.equals(key))
				list.add(pair.val);
		}
		
		return list.toArray(new String[list.size()]);
	}

}
