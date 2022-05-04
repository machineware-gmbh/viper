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

package org.vcml.explorer.ui.handlers;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.swt.widgets.Shell;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.session.Session;

public class ConnectHandler {

    @CanExecute
    public boolean canExecute(ISessionService service) {
        Session current = service.getSession();
        if ((current == null) || current.isConnected())
            return false;
        return true;
    }

    @Execute
    public void execute(Shell shell, ISessionService service, ECommandService commandService,
            EHandlerService handlerService) {
        Session session = service.getSession();
        service.connectSession(session);
        if (session.isConnected()) {
            ParameterizedCommand inspect = commandService.createCommand("org.vcml.explorer.ui.command.inspect", null);
            handlerService.executeHandler(inspect);
        }
    }
}
