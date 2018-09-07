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
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.explorer.ui.terminal.SessionTerminal;
import org.vcml.explorer.ui.terminal.Terminal;
import org.vcml.explorer.ui.terminal.TerminalViewer;
import org.vcml.session.Session;
import org.vcml.session.SessionException;

public class TerminalPart {

    private ComboViewer comboViewer;

    private TerminalViewer terminalViewer;

    private List<Terminal> terminals;

    @Inject
    private ISessionService sessionService;

    private Terminal findTerminalForSession(Session session) {
        for (Terminal terminal : terminals)
            if (terminal instanceof SessionTerminal)
                if (((SessionTerminal) terminal).getSession() == session)
                    return terminal;
        return null;
    }

    private void selectTerminalForSession(Session session) {
        Terminal terminal = findTerminalForSession(session);
        if (terminal == null) {
            try {
                terminal = new SessionTerminal(session, sessionService);
                terminals.add(terminal);
                comboViewer.refresh();
            } catch (IOException | SessionException e) {
                System.out.println("unable to create session terminal: " + e.getMessage());
                return;
            }
        }

        comboViewer.setSelection(new StructuredSelection(terminal), true);
    }

    private void removeTerminalOfSession(Session session) {
        Terminal terminal = findTerminalForSession(session);
        if (terminal == null)
            return;

        terminalViewer.removeBuffer(terminal);
        terminals.remove(terminal);
        terminal.close();

        comboViewer.refresh();
    }

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

    @Focus
    public void setFocus() {
        terminalViewer.setFocus();
    }

    @Inject
    public void selectionChanged(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Session session) {
        if (session != null) {
            Terminal term = findTerminalForSession(session);
            if (term != null)
                comboViewer.setSelection(new StructuredSelection(term));
        }
    }

    @Inject
    @Optional
    public void sessionChanged(@UIEventTopic(ISessionService.TOPIC_SESSION_ANY) Session session) {
        if (session != null) {
            if (session.isConnected())
                selectTerminalForSession(session);
            else
                removeTerminalOfSession(session);
        }
    }

    public TerminalPart() {
        terminals = new ArrayList<>();
    }

    public Terminal activeTerminal() {
        if (comboViewer.getStructuredSelection() == null)
            return null;
        return (Terminal) comboViewer.getStructuredSelection().getFirstElement();
    }

    public void clearTerminal(Terminal term) {
        terminalViewer.clearBuffer(term);
    }

    public boolean getWordWrap() {
        return terminalViewer.getText().getWordWrap();
    }

    public void setWordWrap(boolean wrap) {
        terminalViewer.getText().setWordWrap(wrap);
    }
}
