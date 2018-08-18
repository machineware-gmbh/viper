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

package org.evcml.explorer.ui.terminal;

import java.io.IOException;
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class TerminalViewer extends Composite implements KeyListener {

    private Text text;

    private TerminalBuffer current;

    private HashMap<String, TerminalBuffer> buffers;

    void refreshBuffer(TerminalBuffer buffer) {
        if ((buffer == null) || (buffer != current))
            return;

        text.setText(buffer.getBuffer());
        text.setSelection(buffer.getCursor());

        // Adjust cursor for newlines being two bytes on Windows (CR+LF, 0xd+0xa)
        if (text.getText().indexOf('\r') != -1 && buffer.getCursor() > 0) {
            String beforeCursor = buffer.getBuffer().substring(0, buffer.getCursor());
            int numCR = 0;
            for (char c : beforeCursor.toCharArray())
                if (c == '\n')
                    numCR++;
            text.setSelection(buffer.getCursor() + numCR);
        }
    }

    public TerminalViewer(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());

        text = new Text(this, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL);
        text.addKeyListener(this);
        text.setText("");
        text.setData("org.eclipse.e4.ui.css.id", "TerminalViewer");

        current = null;
        buffers = new HashMap<>();
    }

    public void setTerminal(Terminal terminal) {
        if (terminal == null) {
            current = null;
            text.setText("");
            text.setSelection(0);
            return;
        }

        TerminalBuffer buffer = buffers.get(terminal.getName());
        if (buffer == null) {
            buffer = new TerminalBuffer(this, terminal);
            buffers.put(terminal.getName(), buffer);
        }

        current = buffer;
        refreshBuffer(buffer);

    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (current == null)
            return;

        try {
            switch (event.character) {
            case SWT.CR:
                current.transmit('\n');
                break;

            default:
                current.transmit(event.character);
                break;
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        //
    }

}
