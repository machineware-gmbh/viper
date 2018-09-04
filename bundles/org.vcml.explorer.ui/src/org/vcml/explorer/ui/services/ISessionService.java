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

import java.util.Collection;

import org.vcml.session.Module;
import org.vcml.session.Session;
import org.vcml.session.SessionException;

public interface ISessionService {

    /**
     * Base topic to receive all session related event broadcasts
     */
    public static final String TOPIC_SESSION_BASE = "org/vcml/session";

    /**
     * Broadcasted whenever a new session has been added or found
     */
    public static final String TOPIC_SESSION_ADDED = TOPIC_SESSION_BASE + "/added";

    /**
     * Broadcasted whenever a session has been removed (e.g. terminated)
     */
    public static final String TOPIC_SESSION_REMOVED = TOPIC_SESSION_BASE + "/removed";

    /**
     * Broadcasted whenever the state of a session changes (e.g. connected ->
     * disconnected or running -> stopped).
     */
    public static final String TOPIC_SESSION_UPDATED = TOPIC_SESSION_BASE + "/updated";

    /**
     * Broadcasted whenever a new session has been selected as active session.
     */
    public static final String TOPIC_SESSION_SELECTED = TOPIC_SESSION_BASE + "/selected";

    /**
     * Topic to receive all session related event broadcasts
     */
    public static final String TOPIC_SESSION_ANY = TOPIC_SESSION_BASE + "/*";

    /**
     * Currently active (i.e. selected) session
     */
    public static final String ACTIVE_SESSION = "org.vcml.session.active";

    /**
     * The collection of sessions we are currently connected to.
     * 
     * @return an unmodifiable Collection. For looking, not touching. Will not be
     *         <code>null</code>.
     */
    public Collection<Session> getSessions();

    /**
     * Adds a remote session.
     * 
     * @param URI Specifies session location as a string value.
     */
    public void addRemoteSession(String URI, boolean connect);

    /**
     * Checks for new sessions.
     */
    public void refreshSessions();

    /**
     * Refreshes the specified session.
     */
    public void refreshSession(Session session);

    /**
     * Returns the currently connected session or <code>null</code> if not
     * connected.
     * 
     * @return currently connected session or <code>null</code>.
     */
    public Session getSession();

    /**
     * Sets the currently active session
     * 
     * @param session session to set active
     */
    public void setSession(Session session);

    /**
     * Creates and connects to a new session.
     */
    public void connectSession(Session session);

    /**
     * Disconnects from the current session.
     */
    public void disconnectSession(Session session);

    /**
     * Starts simulation of the currently connected session.
     */
    public void startSimulation(Session session);

    /**
     * Stops simulation of the currently connected session.
     */
    public void stopSimulation(Session session);

    /**
     * Steps the simulation for a single quantum.
     */
    public void stepSimulation(Session session);

    /**
     * Quits simulation and disconnects the session.
     */
    public void quitSimulation(Session session);

    /**
     * Finds a module within the module hierarchy.
     */
    public Module findModule(Session session, String name);

    /**
     * Reports an error for the given session.
     * 
     * @param session Session that has received an error
     * @param e       Description of the error
     */
    public void reportSessionError(Session session, SessionException e);

}
