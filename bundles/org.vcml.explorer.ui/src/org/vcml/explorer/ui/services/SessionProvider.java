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

package org.vcml.explorer.ui.services;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.AbstractSourceProvider;
import org.vcml.session.Session;

public class SessionProvider extends AbstractSourceProvider {
	
	public static final String SESSION = "org.vcml.session";
	
	private Session current = null;
	
	private static SessionProvider instance = null;
	
	public static SessionProvider getInstance() {
		return instance;
	}
	
	public SessionProvider() {
		instance = this;
	}

	@Override
	public void dispose() {
		current = null;
		instance = null;
	}

	@Override
	public Map<String, Session> getCurrentState() {
		Map<String, Session> map = new HashMap<String, Session>();
		if (hasSession())
		    map.put(SESSION, getCurrentSession());
		return map;
	}

	@Override
	public String[] getProvidedSourceNames() {
		return new String[] { SESSION };
	}
	
	public boolean hasSession() {
		return current != null;
	}
	
	public Session getCurrentSession() {
		return current;
	}
	
	public void setCurrentSession(Session session) {
		if (session == current)
			fireSourceChanged(0, SESSION, null);
		
		current = session;
		fireSourceChanged(0, SESSION, current);
	}

}
