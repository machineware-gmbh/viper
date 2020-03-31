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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.progress.UIJob;

public class TerminalBuffer {

    public static final String MSG_IO_ERROR = "I/O Error";

    private Terminal terminal;

    private StringBuilder buffer = new StringBuilder();

    private int cursor = 0;

    private boolean readingEscapeCode = false;

    private String escapeCode = "";

    private UIJob update;

    public Terminal getTerminal() {
        return terminal;
    }

    public String getBuffer() {
        return buffer.toString();
    }

    public int getCursor() {
        return cursor;
    }

    public void clear() {
        buffer.setLength(0);
        cursor = 0;
    }

    private boolean readEscapeCode(int character) {
        escapeCode += (char) character;

        if (escapeCode.equals("c")) { // reset
            buffer.delete(0, buffer.length());
            cursor = 0;
            return true;
        }

        if (escapeCode.equals("[J")) { // clear screen
            buffer.delete(cursor, buffer.length());
            return true;
        }

        if (escapeCode.equals("[H")) { // move to upper left corner
            cursor = 0;
            return true;
        }

        if (escapeCode.equals("[A") || escapeCode.equals("[B")) { // up and down arrows
            cursor = buffer.length();
            return true;
        }

        if (escapeCode.startsWith("[") && escapeCode.endsWith("C")) { // right arrow
            String count = escapeCode.substring(1, escapeCode.length() - 2);
            cursor += count.isEmpty() ? 1 : Integer.valueOf(count);
            return true;
        }

        if (escapeCode.startsWith("[") && escapeCode.endsWith("D")) { // left arrow
            String count = escapeCode.substring(1, escapeCode.length() - 2);
            cursor -= count.isEmpty() ? 1 : Integer.valueOf(count);
            return true;
        }

        if (escapeCode.endsWith("m"))  // color (ignored)
            return true;

        if (escapeCode.endsWith("h")) // options (ignored)
            return true;

        if (escapeCode.equals("(B")) // keyboard layout (ignored)
            return true;


        if (escapeCode.length() > 20) { // stop if we have read too much
            System.err.println(terminal.getName() + ": giving up reading escape code '" + escapeCode + "'");
            return true;
        }

        // continue reading
        return false;
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
            @Override
            public void run() {
                ioThread();
            }
        }.start();
    }

    public void transmit(byte str[]) throws IOException {
        terminal.getTx().write(str);
        terminal.getTx().flush();

        if (getTerminal().isEcho()) {
            for (byte s : str)
                receive(s);
        }
    }

    public void transmit(int character) throws IOException {
        terminal.getTx().write(character);
        terminal.getTx().flush();

        if (getTerminal().isEcho())
            receive(character);
    }

    public void receive(int character) {
        if (readingEscapeCode) {
            if (readEscapeCode(character)) {
                readingEscapeCode = false;
                update.schedule();
            }

            return;
        }

        switch (character) {
        case -1:
            buffer.append(MSG_IO_ERROR);
            cursor += MSG_IO_ERROR.length();
            break;

        case '\b':
            if (buffer.length() > 0)
                cursor--;
            break;

        case '\r':
            cursor = buffer.lastIndexOf("\n") + 1;
            break;

        case '\n':
            cursor = buffer.length();
            buffer.insert(cursor, (char)character);
            cursor++;
            break;

        case 0x1a: // ctrl+z ignored
            break;

        case 0x1b: // escape
            readingEscapeCode = true;
            escapeCode = "";
            return; // wait with update until we read the entire code

        case 0x07: // bell
            return;

        default:
            if (cursor < buffer.length())
                buffer.delete(cursor, buffer.length());
            buffer.insert(cursor, (char)character);
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
