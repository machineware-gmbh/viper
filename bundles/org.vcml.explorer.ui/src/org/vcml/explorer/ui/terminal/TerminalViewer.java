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
import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

public class TerminalViewer extends Composite implements KeyListener {

    private StyledText text;

    private TerminalBuffer current;

    private HashMap<Terminal, TerminalBuffer> buffers;

    void refreshBuffer(TerminalBuffer buffer) {
        if ((buffer == null) || (buffer != current))
            return;

        int cursor;
        synchronized (buffer) {
            text.setText(buffer.getBuffer());
            cursor = buffer.getCursor();
        }

        // Adjust cursor for newlines being two bytes on Windows (CR+LF, 0xd+0xa)
        if (text.getText().indexOf('\r') != -1 && cursor > 0) {
            String beforeCursor = buffer.getBuffer().substring(0, buffer.getCursor());
            for (char c : beforeCursor.toCharArray())
                if (c == '\n')
                    cursor++;
        }

        text.setSelection(cursor);
    }

    public TerminalViewer(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());

        text = new StyledText(this, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
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

        TerminalBuffer buffer = buffers.get(terminal);
        if (buffer == null) {
            buffer = new TerminalBuffer(this, terminal);
            buffers.put(terminal, buffer);
        }

        current = buffer;
        refreshBuffer(buffer);

    }

    public void removeBuffer(Terminal terminal) {
        if (terminal == current.getTerminal())
            setTerminal(null);

        buffers.remove(terminal);
    }

    public void clearBuffer(Terminal terminal) {
        TerminalBuffer buffer = buffers.get(terminal);
        if (buffer != null) {
            buffer.clear();
            refreshBuffer(buffer);
        }
    }

    @Override
    public void keyPressed(KeyEvent event) {
        if (current == null)
            return;

        try {
            switch (event.character) {
            case 0:
                break;

            case 0x3: // ctrl+c
                current.transmit(0x3);
                break;

            case SWT.ESC:
                current.transmit(SWT.ESC);
                break;

            case SWT.BS:
                current.transmit(SWT.BS);
                break;

            case SWT.CR:
                current.transmit(SWT.LF);
                break;

            default:
                if (Character.isISOControl(event.character))
                    System.out.println("dropping control character 0x" + Integer.toHexString(event.character));
                else
                    current.transmit(event.character);
                break;
            }

        } catch (IOException e) {
            text.setText(text.getText() + "\n" + e.getMessage() + "\n");
            text.setSelection(text.getText().length());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        //
    }

    public StyledText getText() {
        return text;
    }
}
