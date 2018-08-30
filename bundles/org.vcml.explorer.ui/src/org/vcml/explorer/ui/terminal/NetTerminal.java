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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class NetTerminal extends Terminal {

    public static final int TIMEOUT_MS = 1000;

    private Socket socket;

    private InputStream rx;

    private OutputStream tx;

    public NetTerminal(String name, String host, int port) throws IOException {
        super(name, false);
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
        rx = socket.getInputStream();
        tx = socket.getOutputStream();
    }

    public NetTerminal(String host, int port) throws IOException {
        this(host + ":" + port, host, port);
    }

    public InputStream getRx() {
        return rx;
    }

    public OutputStream getTx() {
        return tx;
    }

}
