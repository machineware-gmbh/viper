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

package org.vcml.explorer.ui;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessAdditions;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessRemovals;
import org.eclipse.jface.window.Window;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.vcml.explorer.ui.dialogs.ConnectDialog;
import org.vcml.explorer.ui.services.ISessionService;

@SuppressWarnings("restriction")
public class LifeCycle {

    private IEclipseContext context;

    @PostContextCreate
    void postContextCreate(IEclipseContext workbenchContext, IEventBroker eventBroker) {
        context = workbenchContext;
        System.out.println("viper life cycle: postContextCreate");
        eventBroker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE, new EventHandler() {
            @Override
            public void handleEvent(Event event) {
                eventBroker.unsubscribe(this);
                appStartupComplete();
            }
        });
    }

    @PreSave
    void preSave(IEclipseContext workbenchContext) {
        // Nothing to do
    }

    @ProcessAdditions
    void processAdditions(IEclipseContext workbenchContext) {
        // Nothing to do
    }

    @ProcessRemovals
    void processRemovals(IEclipseContext workbenchContext) {
        // Nothing to do
    }

    void appStartupComplete() {
        ISessionService service = context.get(ISessionService.class);
        if (service.getSessions().isEmpty()) {
            ConnectDialog dialog = new ConnectDialog(null);
            if (dialog.open() == Window.OK) {
                service.addRemoteSession(dialog.getURI(), dialog.connectImmediately());
            }
        }
    }

}
