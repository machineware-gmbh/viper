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

package org.vcml.explorer.ui.handlers;

import java.util.List;

import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.vcml.explorer.ui.dialogs.ConnectDialog;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.session.Session;

public class NewHandler {
    @Execute
    public void execute(Shell shell, ISessionService service, ECommandService command, EHandlerService handler,
            EPartService partService, EModelService modelService, MApplication application) {
        ConnectDialog dialog = new ConnectDialog(shell);
        if (dialog.open() == Window.OK) {
            Session session = service.addRemoteSession(dialog.getURI());
            if (session == null)
                return;
            service.setSession(session);
            if (dialog.connectImmediately())
                service.connectSession(session);
            if (!session.isConnected())
                return;

            String selectionId = session.toString();
            MPart part = partService.findPart(selectionId);
            if (part == null) {
                part = MBasicFactory.INSTANCE.createPart();
                part.setLabel(session.getName());
                part.setContributionURI(InspectSessionHandler.PART_URI);
                part.setCloseable(true);
                part.setElementId(selectionId);
                part.setIconURI(InspectSessionHandler.ICON_URI);
            }

            List<MPartStack> stacks = modelService.findElements(application, InspectSessionHandler.STACK_ID,
                    MPartStack.class, null);
            stacks.get(0).getChildren().add(part);
            partService.showPart(part, PartState.ACTIVATE);

        }
    }
}
