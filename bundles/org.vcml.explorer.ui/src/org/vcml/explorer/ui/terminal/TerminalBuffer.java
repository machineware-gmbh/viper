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

import org.eclipse.swt.widgets.Display;

public class TerminalBuffer {

    public static final String MSG_IO_ERROR = "I/O Error";

    public static final String TOPIC_CONSOLE_UPDATE = "VCML/CONSOLE/UPDATE";

    private Terminal console;

    private String buffer;

    private int cursor;

    private Runnable update;

    public Terminal getConsole() {
        return console;
    }

    public String getBuffer() {
        return buffer;
    }

    public int getCursor() {
        return cursor;
    }

    public TerminalBuffer(TerminalViewer viewer, Terminal console) {
        this.console = console;
        this.buffer = "";
        this.cursor = 0;
        this.update = new Runnable() {
            @Override
            public void run() {
                viewer.refreshBuffer(TerminalBuffer.this);
            }
        };

        new Thread("ioThread_" + console.getName()) {
            public void run() {
                receive();
            }
        }.start();
    }

    public void transmit(int character) throws IOException {
        console.getTx().write(character);
        console.getTx().flush();

        if (getConsole().isEcho()) {
            buffer += (char) character;
            Display.getDefault().syncExec(update);
        }
    }

    public void receive() {
        try {
            while (true) {
                int input = console.getRx().read();
                switch (input) {
                case -1:
                    buffer += MSG_IO_ERROR;
                    cursor += MSG_IO_ERROR.length();
                    break;

                case '\r':
                    break;

                case '\b':
                    if (!buffer.isEmpty()) {
                        buffer = buffer.substring(0, buffer.length() - 1);
                        cursor--;
                    }
                    break;

                default:
                    buffer += (char) input;
                    cursor++;
                }

                Display.getDefault().syncExec(update);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
