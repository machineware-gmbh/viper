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

package org.vcml.explorer.ui.parts;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.explorer.ui.terminal.NetTerminal;
import org.vcml.explorer.ui.terminal.TerminalViewer;
import org.vcml.session.Module;
import org.vcml.session.Session;
import org.vcml.session.SessionException;

public class UartPart {

    private Session session = null;

    private Module uart = null;

    private NetTerminal terminal = null;

    private TerminalViewer viewer = null;

    private static final Color activeColor = Display.getDefault().getSystemColor(SWT.COLOR_WHITE);

    private static final Color inactiveColor = Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);

    private String backend = "";

    private int port = -1;

    public int getPort() {
        return port;
    }

    private void lookupPort() {
        port = -1;

        try {
            if (backend.isEmpty()) {
                String expected = "OK,created backend ";
                String resp = uart.execute("create_backend", "tcp");
                if (!resp.startsWith(expected))
                    return;
                backend = resp.substring(expected.length());
            }

            String expected = backend + ": tcp:";
            String resp = uart.execute("list_backends");

            int pos = resp.indexOf(expected);
            if (pos == -1)
                return;

            int end = resp.indexOf(",", pos);
            if (end == -1)
                end = resp.length();

            port = Integer.valueOf(resp.substring(pos + expected.length(), end));

        } catch (SessionException e) {
            e.printStackTrace();
        }
    }

    private void update() {
        // This updates colors immediately, CSS waits for focus
        StyledText text = viewer.getText();

        if (!session.isConnected() || !session.isRunning()) {
            if (!text.isDisposed()) {
                text.setEnabled(false);
                text.setForeground(inactiveColor);
            }
            return;
        }

        if (!text.isEnabled()) {
            text.setEnabled(true);
            text.setForeground(activeColor);
        }
    }

    @PostConstruct
    public void createComposite(Composite parent, ISessionService sessionService, ESelectionService selectionService) {
        session = sessionService.getSession();
        uart = (Module) selectionService.getSelection();

        viewer = new TerminalViewer(parent);
        viewer.getText().setData("org.eclipse.e4.ui.css.id", "UartViewer");

        Caret caret = new Caret(viewer.getText(), SWT.NONE);
        caret.setBounds(0, 0, 8, viewer.getText().getLineHeight());
        viewer.getText().setCaret(caret);

        String name = uart.getName();
        String host = session.getHost();

        lookupPort();

        if (port < 0) {
            MessageDialog.openError(parent.getShell(), uart.getName(),
                    "Unable to find a port for the TCP connection. Did you add a TCP backend?");
            return;
        }

        try {
            terminal = new NetTerminal(name, host, port);
            viewer.setTerminal(terminal);
        } catch (IOException e) {
            MessageDialog.openError(parent.getShell(), uart.getName(),
                    "Cannot connect to " + uart.getName() + ": " + e.getMessage());
        }

        update();
    }

    @PreDestroy
    public void preDestroy() {
        if (terminal != null)
            terminal.close();

        try {
            if (!backend.isEmpty() && uart.getSession().isConnected())
                uart.execute("destroy_backend", backend);
        } catch (SessionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Focus
    public void setFocus() {
        viewer.setFocus();
    }

    @Inject
    @Optional
    public void sessionChanged(@UIEventTopic(ISessionService.TOPIC_SESSION_ANY) Session current) {
        if (session == current)
            update();
    }

}
