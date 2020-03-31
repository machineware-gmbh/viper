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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

public class TerminalViewer extends Composite implements KeyListener, MouseListener, TraverseListener {

    private StyledText text;

    private TerminalBuffer current;

    private HashMap<Terminal, TerminalBuffer> buffers;

    void refreshBuffer(TerminalBuffer buffer) {
        if ((buffer == null) || (buffer != current))
            return;

        synchronized (buffer) {
            text.setText(buffer.getBuffer());
            text.setSelection(buffer.getCursor());
        }
    }

    void handleSpecialKeys(int keyCode) throws IOException {
        switch (keyCode) {
        case SWT.ARROW_UP:
            byte[] up = { 0x1b, '[', 'A' };
            current.transmit(up);
            break;

        case SWT.ARROW_DOWN:
            byte[] down = { 0x1b, '[', 'B' };
            current.transmit(down);
            break;

        case SWT.ARROW_RIGHT:
            byte[] right = { 0x1b, '[', 'C' };
            current.transmit(right);
            break;

        case SWT.ARROW_LEFT:
            byte[] left = { 0x1b, '[', 'D' };
            current.transmit(left);
            break;

        case SWT.HOME:
            byte[] home = { 0x1b, '[', '1', '~' };
            current.transmit(home);
            break;

        case SWT.INSERT:
            byte[] insert = { 0x1b, '[', '2', '~' };
            current.transmit(insert);
            break;

        case SWT.DEL:
            byte[] del = { 0x1b, '[', '3', '~' };
            current.transmit(del);
            break;

        case SWT.END:
            byte[] end = { 0x1b, '[', '4', '~' };
            current.transmit(end);
            break;

        case SWT.PAGE_UP:
            byte[] pgup = { 0x1b, '[', '5', '~' };
            current.transmit(pgup);
            break;

        case SWT.PAGE_DOWN:
            byte[] pgdn = { 0x1b, '[', '6', '~' };
            current.transmit(pgdn);
            break;

        default:
            break;
        }

        text.setSelection(text.getText().length());
    }

    public StyledText getText() {
        return text;
    }

    public TerminalViewer(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());

        text = new StyledText(this, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);

        text.addKeyListener(this);
        text.addMouseListener(this);
        text.addTraverseListener(this);

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
            case 0x7f:
                handleSpecialKeys(event.keyCode);
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

            case SWT.TAB:
                current.transmit(SWT.TAB);
                break;

            default:
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
        // nothing to do
    }

    @Override
    public void mouseDoubleClick(MouseEvent e) {
        // nothing to do
    }

    @Override
    public void mouseDown(MouseEvent e) {
        refreshBuffer(current);
    }

    @Override
    public void mouseUp(MouseEvent e) {
        refreshBuffer(current);
    }

    @Override
    public void keyTraversed(TraverseEvent e) {
        switch (e.detail) {
        case SWT.TRAVERSE_TAB_NEXT:
        case SWT.TRAVERSE_TAB_PREVIOUS:
            e.doit = false;
            break;

        default:
            break;
        }
    }

}
