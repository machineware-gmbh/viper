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

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.vcml.explorer.ui.parts.TerminalPart;

public class TerminalWordWrapHandler {
    private static final String PART_ID = "org.vcml.explorer.ui.part.terminals";

    @CanExecute
    public boolean canExecute(MApplication application, EModelService service) {
        List<MPart> parts = service.findElements(application, PART_ID, MPart.class, null);
        if (parts.isEmpty())
            return false;
        TerminalPart part = (TerminalPart) parts.get(0).getObject();
        return part.activeTerminal() != null;
    }

    @Execute
    public void execute(MApplication application, EModelService service) {
        List<MPart> parts = service.findElements(application, PART_ID, MPart.class, null);
        TerminalPart part = (TerminalPart) parts.get(0).getObject();
        part.toggleWordWrap();
    }

}
