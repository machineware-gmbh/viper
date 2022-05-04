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

import java.util.List;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.vcml.session.Session;

public class InspectSessionHandler {

    public static final String STACK_ID = "org.vcml.explorer.ui.partstack.middle";

    public static final String PART_URI = "bundleclass://org.vcml.explorer.ui/org.vcml.explorer.ui.parts.SystemCPart";

    public static final String ICON_URI = "platform:/plugin/org.vcml.explorer.ui/icons/session_alt.gif";

    @CanExecute
    public boolean canExecute(ESelectionService selectionService) {
        Object selection = selectionService.getSelection();
        if (!(selection instanceof Session))
            return false;
        return ((Session) selection).isConnected();
    }

    @Execute
    public void execute(EPartService partService, EModelService modelService, ESelectionService selectionService,
            MApplication application) {
        Session selection = (Session) selectionService.getSelection();
        String selectionId = selection.toString();

        MPart part = partService.findPart(selectionId);
        if (part == null) {
            part = MBasicFactory.INSTANCE.createPart();
            part.setLabel(selection.getName());
            part.setContributionURI(PART_URI);
            part.setCloseable(true);
            part.setElementId(selectionId);
            part.setIconURI(ICON_URI);
        }

        List<MPartStack> stacks = modelService.findElements(application, STACK_ID, MPartStack.class, null);
        stacks.get(0).getChildren().add(part);
        partService.showPart(part, PartState.ACTIVATE);
    }

}
