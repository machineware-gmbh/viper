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

package org.vcml.explorer.ui.parts;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.vcml.explorer.ui.terminal.CommandTerminal;
import org.vcml.explorer.ui.terminal.Terminal;
import org.vcml.explorer.ui.terminal.TerminalViewer;

public class TerminalPart {

    private ComboViewer comboViewer;

    private TerminalViewer terminalViewer;

    private Terminal[] terminals;

    private LabelProvider labelProvider = new LabelProvider() {
        @Override
        public String getText(Object element) {
            return ((Terminal) element).getName();
        }
    };

    private ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            Terminal term = (Terminal) event.getStructuredSelection().getFirstElement();
            terminalViewer.setTerminal(term);
            terminalViewer.setFocus();
        }
    };

    @PostConstruct
    public void createComposite(Composite parent, IEventBroker broker, IEclipseContext ctx) throws IOException {
        terminals = new Terminal[3];
        terminals[0] = new CommandTerminal("terminal 1");
        terminals[1] = new CommandTerminal("terminal 2");
        terminals[2] = new CommandTerminal("terminal 3");

        parent.setLayout(new GridLayout());

        comboViewer = new ComboViewer(parent, SWT.DROP_DOWN);
        comboViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        comboViewer.setLabelProvider(labelProvider);
        comboViewer.addSelectionChangedListener(selectionChangedListener);
        comboViewer.setContentProvider(ArrayContentProvider.getInstance());
        comboViewer.setInput(terminals);

        terminalViewer = new TerminalViewer(parent);
        terminalViewer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    }
}
