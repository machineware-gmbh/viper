/******************************************************************************
 *                                                                            *
 * Copyright 2022 MachineWare GmbH                                            *
 * All Rights Reserved                                                        *
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

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;
import org.vcml.session.Module;
import org.vcml.session.Session;
import org.vcml.session.SessionException;

public class SessionService implements ISessionService {

    private IEclipseContext context;

    private IEventBroker broker;

    private List<Session> sessions = new ArrayList<Session>();

    public static final long UPDATE_INTERVAL = 200; // milliseconds

    private UIJob updateJob = new UIJob(Display.getDefault(), "sessionStatusUpdate") {
        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            for (Session session : sessions) {
                if (session.isConnected()) {
                    try {
                        session.updateStatus();
                        updateSession(session, TOPIC_SESSION_UPDATED);
                        if (session.isRunning())
                            schedule(UPDATE_INTERVAL);
                    } catch (SessionException e) {
                        reportSessionError(session, e);
                    }
                }
            }

            return Status.OK_STATUS;
        }
    };

    @Inject
    public SessionService(IEclipseContext eclipseContext, IEventBroker eventBroker) {
        System.out.println("session service created");
        context = eclipseContext;
        broker = eventBroker;
        refreshSessions();
    }

    private void updateSession(Session session, String topic) {
        broker.post(topic, session);
        broker.post(UIEvents.REQUEST_ENABLEMENT_UPDATE_TOPIC, UIEvents.ALL_ELEMENT_ID);
    }

    private void addSession(Session session) {
        if (session == null || sessions.contains(session))
            return;

        sessions.add(session);
        updateSession(session, TOPIC_SESSION_ADDED);
    }

    private void removeSession(Session session) {
        if (session == null || !sessions.contains(session))
            return;

        sessions.remove(session);
        updateSession(session, TOPIC_SESSION_REMOVED);
    }

    @Override
    public Collection<Session> getSessions() {
        return Collections.unmodifiableCollection(sessions);
    }

    @Override
    public Session getSession() {
        return (Session) context.get(ACTIVE_SESSION);
    }

    @Override
    public void setSession(Session session) {
        if (session != getSession()) {
            context.set(ACTIVE_SESSION, session);
            updateSession(session, TOPIC_SESSION_SELECTED);
        }
    }

    @Override
    public void refreshSession(Session session) {
        try {
            if (session == null || !session.isConnected() || session.isRunning())
                return;

            session.refresh();
            updateSession(session, TOPIC_SESSION_UPDATED);
        } catch (SessionException e) {
            reportSessionError(session, e);
        }
    }

    @Override
    public void connectSession(Session session) {
        try {
            if (session.isConnected())
                return;
            session.connect();
            updateSession(session, TOPIC_SESSION_UPDATED);
        } catch (SessionException e) {
            reportSessionError(session, e);
        }
    }

    @Override
    public void disconnectSession(Session session) {
        try {
            if (session == null)
                return;
            if (session.isRunning())
                stopSimulation(session);
            session.disconnect();
            updateSession(session, TOPIC_SESSION_UPDATED);
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
            updateSession(session, TOPIC_SESSION_UPDATED);
            updateJob.schedule(UPDATE_INTERVAL);
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
            updateSession(session, TOPIC_SESSION_UPDATED);
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
            updateSession(session, TOPIC_SESSION_UPDATED);
            updateJob.schedule(UPDATE_INTERVAL);
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
            if (session == null || !session.isConnected() || session.isRunning())
                return null;
            return session.findObject(name);
        } catch (SessionException e) {
            reportSessionError(session, e);
            return null;
        }
    }

    @Override
    public void reportSessionError(Session session, SessionException e) {
        try {
            if (session.isConnected())
                session.disconnect();
        } catch (SessionException ex) {
            // ignore, session is already erroneous
        } finally {
            removeSession(session);
            String message = e.getMessage();
            Throwable cause = e.getCause();
            if (cause != null)
                message += ": " + cause.getMessage();
            System.err.println(message);
            MessageDialog.openError(Display.getDefault().getActiveShell(), "Session Error", message);
        }
    }

    @Override
    public void refreshSessions() {
        List<Session> available = Session.getAvailableSessions();
        for (Session session : available)
            addSession(session);
    }

    @Override
    public Session addRemoteSession(String URI) {
        try {
            for (Session s : sessions)
                if (URI.equals(s.getURI()))
                    return s;
            Session session = new Session(URI);
            addSession(session);
            return session;
        } catch (SessionException e) {
            MessageDialog.openError(null, "Session management", e.getMessage());
            return null;
        }
    }

}
