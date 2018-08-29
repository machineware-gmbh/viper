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

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.explorer.ui.terminal.NetTerminal;
import org.vcml.explorer.ui.terminal.Terminal;
import org.vcml.explorer.ui.terminal.TerminalViewer;
import org.vcml.session.Attribute;
import org.vcml.session.Module;
import org.vcml.session.Session;

public class UartPart {

    private Session session;

    private Module uart;

    private Terminal terminal;

    private TerminalViewer viewer;

    private int getPort() {
        for (Module child : uart.getChildren())
            if (child.getKind().equals("vcml::backend_tcp"))
                for (Attribute attr : child.getAttributes())
                    if (attr.getBaseName().equals("port"))
                        return Integer.parseInt(attr.getValue());
        return -1;
    }

    private void update() {
        viewer.getText().setEnabled(session.isConnected() && session.isRunning());
        viewer.getText().setFocus();
    }

    @PostConstruct
    public void createComposite(Composite parent, ISessionService sessionService, ESelectionService selectionService)
            throws Exception {
        session = sessionService.currentSession();
        uart = (Module) selectionService.getSelection();

        String name = uart.getName();
        String host = session.getHost();
        int port = getPort();
        if (port == -1)
            throw new Exception("failed to lookup port for UART connection");
        terminal = new NetTerminal(name, host, port);

        viewer = new TerminalViewer(parent);
        viewer.setTerminal(terminal);
        viewer.getText().setData("org.eclipse.e4.ui.css.id", "UartViewer");

        Caret caret = new Caret(viewer.getText(), SWT.NONE);
        caret.setBounds(0, 0, 8, viewer.getText().getLineHeight());
        viewer.getText().setCaret(caret);

        update();
    }

    @Focus
    public void setFocus() {
        viewer.setFocus();
    }

    @Inject
    @Optional
    public void sessionChanged(@UIEventTopic(ISessionService.SESSION_TOPIC) Session current) {
        if (session == current)
            update();
    }

}
