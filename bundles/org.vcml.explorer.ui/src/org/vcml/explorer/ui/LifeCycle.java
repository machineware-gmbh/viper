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

package org.vcml.explorer.ui;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.lifecycle.PostContextCreate;
import org.eclipse.e4.ui.workbench.lifecycle.PreSave;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessAdditions;
import org.eclipse.e4.ui.workbench.lifecycle.ProcessRemovals;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import org.vcml.explorer.ui.services.ISessionService;

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
        ECommandService command = context.get(ECommandService.class);
        EHandlerService handler = context.get(EHandlerService.class);

        if (service.getSessions().isEmpty()) {
            ParameterizedCommand newsession = command.createCommand("org.vcml.explorer.ui.command.new", null);
            handler.executeHandler(newsession);
        }
    }

}
