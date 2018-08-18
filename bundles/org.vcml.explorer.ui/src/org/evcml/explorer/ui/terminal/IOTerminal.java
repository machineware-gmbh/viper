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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;

public abstract class IOTerminal extends Terminal {

    private PipedInputStream rx;

    private PipedOutputStream tx;

    protected PrintStream out;

    protected InputStream in;

    @Override
    public InputStream getRx() {
        return rx;
    }

    @Override
    public OutputStream getTx() {
        return tx;
    }

    public IOTerminal(String name, boolean echo) throws IOException {
        super(name, echo);

        rx = new PipedInputStream();
        out = new PrintStream(new PipedOutputStream(rx));

        tx = new PipedOutputStream();
        in = new PipedInputStream(tx);

        new Thread() {
            public void run() {
                loop();
            }
        }.start();
    }

    public IOTerminal(String name) throws IOException {
        this(name, false);
    }

    /**
     * Implement this and then use a loop to read from 'in' and write to 'out'
     */
    public abstract void loop();

}
