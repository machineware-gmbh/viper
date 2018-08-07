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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.services.IDisposable;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.services.ISourceProviderService;
import org.vcml.session.Module;
import org.vcml.session.Session;
import org.vcml.session.SessionException;

public class SessionService implements ISessionService, IDisposable {
	
	private SessionProvider provider;

	private List<Session> sessions = new ArrayList<Session>();
	
	private ListenerList<IPropertyChangeListener> listeners = new ListenerList<IPropertyChangeListener>(ListenerList.IDENTITY);
	
	private void updateSession(String property, Session session) {
		if (session == provider.getCurrentSession())
			provider.setCurrentSession(session);

		if (!listeners.isEmpty()) {
			PropertyChangeEvent event = new PropertyChangeEvent(this, property, null, session);
			Object[] array = listeners.getListeners();
			for (int i = 0; i < array.length; i++)
				((IPropertyChangeListener)array[i]).propertyChange(event);
		}
	}
	
	private void addSession(Session session) {
		if (session == null || sessions.contains(session))
			return;
		
		sessions.add(session);
		updateSession(PROP_ADDED, session);
	}
	
	private void removeSession(Session session) {
		if (session == null || !sessions.contains(session))
			return;
		
		sessions.remove(session);
		updateSession(PROP_REMOVED, session);
	}
	
	public SessionService(IServiceLocator locator) {
		ISourceProviderService service = locator.getService(ISourceProviderService.class);
		provider = (SessionProvider)service.getSourceProvider(SessionProvider.SESSION);
		refreshSessions();
	}

	@Override
	public Collection<Session> getSessions() {
		return Collections.unmodifiableCollection(sessions);
	}

	@Override
	public Session currentSession() {
		return provider.getCurrentSession();
	}
	
	@Override
	public void selectSession(Session session) {
		provider.setCurrentSession(session);
	}

	@Override
	public void connectSession(Session session) {
		try {
			if (session.isConnected())
				return;
			session.connect();
			selectSession(session);
			updateSession(PROP_UPDATED, session);
		} catch (SessionException e) {
			reportSessionError(session, e);
		}
	}

	@Override
	public void disconnectSession(Session session) {
		try {
			if (session == null)
				return;	
			session.disconnect();
		    updateSession(PROP_UPDATED, session);
		} catch (SessionException e) {
			reportSessionError(session, e);
		}
	}

	@Override
	public void startSimulation(Session session) {
		try {
            if (session == null || session.isRunning())
            	return;
        	if (!session.isConnected())
        		connectSession(session);
        	session.continueSimulation();
        	updateSession(PROP_UPDATED, session);
		} catch (SessionException e) {
			reportSessionError(session, e);
		}
	}

	@Override
	public void stopSimulation(Session session) {
		try {
            if (session == null || !session.isRunning() || !session.isConnected())
            	return;
        	session.stopSimulation();
        	updateSession(PROP_UPDATED, session);
		} catch (SessionException e) {
			reportSessionError(session, e);
		}
	}

	@Override
	public void stepSimulation(Session session) {
		try {
            if (session == null || session.isRunning())
            	return;
            if (!session.isConnected())
            	connectSession(session);
        	session.stepSimulation();
        	updateSession(PROP_UPDATED, session);
		} catch (SessionException e) {
			reportSessionError(session, e);
		}
	}
	
	@Override
	public void quitSimulation(Session session) {
		try {
			if (session == null)
				return;
			if (!session.isConnected())
				connectSession(session);
			if (session.isRunning())
				stopSimulation(session);
			session.quitSimulation();
			session.disconnect();
			removeSession(session);
		} catch (SessionException e) {
			reportSessionError(session, e);
		}
	}
	
	@Override
	public Module findModule(Session session, String name) {
		try {
			if (session == null)
				return null;
			if (!session.isConnected())
				connectSession(session);
			return session.findObject(name);
		} catch (SessionException e) {
			reportSessionError(session, e);
			return null;
		}
	}
	
	@Override
	public void reportSessionError(Session session, SessionException e) {
		if (session.isConnected())
			disconnectSession(session);
		removeSession(session);
		System.err.println(e.getMessage());
		MessageDialog.openError(null, "Session management", e.getMessage());
	}
	

	@Override
	public void addSessionChangeListener(IPropertyChangeListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeSessionChangeListener(IPropertyChangeListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void dispose() {
		sessions.clear();
		listeners.clear();
	}
	
	@Override
	public void refreshSessions() {
		List<Session> available = Session.getAvailableSessions();
		for (Session session : available)
			addSession(session);
	}
	
	@Override
	public void addRemoteSession(String URI) {
		try {
			Session session = new Session(URI);
			addSession(session);
		} catch (SessionException e) {
			MessageDialog.openError(null, "Session management", e.getMessage());
		}
	}
	
	@Override
	public void addRemoteSession() {
		final String TITLE = "Add session...";
		final String INPUT = "<host>:<port>[:<user>:<name>]";
		final String QUERY = "Specify session URI, e.g.\n\t" + INPUT;
		
		InputDialog dialog = new InputDialog(null, TITLE, QUERY, "", new IInputValidator() {
			@Override
			public String isValid(final String s) {
				String[] parts = s.split(":");
				if (parts.length < 2)
					return "Invalid input";
				return null;
			}
		});

		if (dialog.open() == Window.OK)
			addRemoteSession(dialog.getValue());
	}

}
