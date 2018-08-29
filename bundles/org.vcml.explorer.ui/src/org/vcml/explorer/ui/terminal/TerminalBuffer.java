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

package org.vcml.explorer.ui.terminal;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;

public class TerminalBuffer {

    public static final String MSG_IO_ERROR = "I/O Error";

    public static final Pattern ESCREGEX = Pattern.compile("\\[\\?*(\\d+)*(;\\d+)*[a-zA-Z]");

    private Terminal terminal;

    private String buffer = "";

    private int cursor = 0;

    private boolean readingEscapeCode = false;

    private String escapeCode = "";

    private UIJob update;

    public Terminal getTerminal() {
        return terminal;
    }

    public String getBuffer() {
        return buffer;
    }

    public int getCursor() {
        return cursor;
    }

    public void clear() {
        buffer = "";
        cursor = 0;
    }

    private void readEscapeCode(int character) {
        escapeCode += (char) character;
        Matcher m = ESCREGEX.matcher(escapeCode);
        if (m.matches()) {
            handleEscapeCode(escapeCode);
            escapeCode = "";
            readingEscapeCode = false;
            return;
        } else if (escapeCode.length() > 10) {
            System.err.println(terminal.getName() + ": giving up reading escape code '" + escapeCode + "'");
            escapeCode = "";
            readingEscapeCode = false;
        }
    }

    private void handleEscapeCode(String code) {
        if (code.equals("[J")) {
            buffer = buffer.substring(0, cursor);
        } else if (code.equals("[H")) {
            cursor = 0;
        } else if (code.endsWith("m")) {
            // ignore color for now
        } else {
            System.out.println(terminal.getName() + ": got unknown escape code '" + code + "'");
        }
    }

    public TerminalBuffer(TerminalViewer viewer, Terminal terminal) {
        this.terminal = terminal;
        this.update = new UIJob(Display.getDefault(), "uiUpdate_" + terminal.getName()) {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if (!viewer.isDisposed())
                    viewer.refreshBuffer(TerminalBuffer.this);
                return Status.OK_STATUS;
            }
        };

        new Thread("ioThread_" + terminal.getName()) {
            public void run() {
                ioThread();
            }
        }.start();
    }

    public void transmit(int character) throws IOException {
        terminal.getTx().write(character);
        terminal.getTx().flush();

        if (getTerminal().isEcho())
            receive(character);
    }

    public void receive(int character) {
        if (readingEscapeCode) {
            readEscapeCode(character);
            return;
        }

        switch (character) {
        case -1:
            buffer += MSG_IO_ERROR;
            cursor += MSG_IO_ERROR.length();
            break;

        case '\b':
            if (!buffer.isEmpty()) {
                buffer = buffer.substring(0, buffer.length() - 1);
                cursor--;
            }
            break;

        case 0x1b: // escape
            readingEscapeCode = true;
            return; // wait with update until we read the entire code

        case 0x07: // bell
            return;

        default:
            buffer += (char) character;
            cursor++;
        }

        update.schedule();
    }

    public void ioThread() {
        try {
            while (true) {
                int input = terminal.getRx().read();
                synchronized (this) {
                    receive(input);
                }
                if (input == -1)
                    return;
            }
        } catch (IOException e) {
            System.out.println(terminal.getName() + ": I/O thread terminating (" + e.getMessage() + ")");
        }
    }

}
