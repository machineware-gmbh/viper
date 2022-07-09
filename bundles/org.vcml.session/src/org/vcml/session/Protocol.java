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

package org.vcml.session;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Protocol {

    private static boolean needsEscape(char c) {
        return c == '$' || c == '#' || c == '*' || c == '}';
    }

    private static String escape(String s) {
        String esc = "";
        for (char c : s.toCharArray()) {
            if (needsEscape(c))
                esc = esc + '}' + (char)(c ^ 0x20);
            else
                esc = esc + c;
        }
        return esc;
    }

    private static int calcChecksum(String str) {
        int result = 0;
        for (int i = 0; i < str.length(); i++)
            result += str.charAt(i);
        return result & 0xff;
    }

    private static int calcChecksum(int chr1, int chr2) {
        String txt = "" + (char) chr1 + (char) chr2;
        return Integer.parseInt(txt, 16);
    }

    private Socket socket;

    public Protocol(String host, int port) throws SessionException {
        try {
            socket = new Socket();
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(host, port), 1000);
        } catch (IOException e) {
            throw new SessionException("Failed to connect to session", e);
        }
    }

    public static final String VERSION = "version";
    public static final String STATUS = "status";
    public static final String RESUME = "resume";
    public static final String STOP = "stop";
    public static final String QUIT = "quit";
    public static final String LIST = "list";
    public static final String EXEC = "exec";
    public static final String GETQ = "getq";
    public static final String SETQ = "setq";
    public static final String GETA = "geta";
    public static final String SETA = "seta";

    public void send_char(int val) throws SessionException {
        try {
            socket.getOutputStream().write(val);
            socket.getOutputStream().flush();
        } catch (IOException e) {
            throw new SessionException("Failed to contact session", e);
        }
    }

    public void send(String message) throws SessionException {
        try {
            // Packet format: $<message>#<8bit-checksum>
            String payload = escape(message);
            int checksum = calcChecksum(payload);
            String packet = "$" + payload + "#" + String.format("%02X", checksum);
            socket.getOutputStream().write(packet.getBytes());
            socket.getOutputStream().flush();

            int response = socket.getInputStream().read();
            switch (response) {
            case '+':
                break; // all good
            case '-':
                throw new SessionException("Checksum error");
            case -1:
                throw new SessionException("Disconnected");
            default:
                throw new SessionException("Invalid response (" + response + ")");
            }
        } catch (IOException e) {
            throw new SessionException("Failed to contact session", e);
        }
    }

    public String recv() throws SessionException {
        try {
            InputStreamReader ireader = new InputStreamReader(socket.getInputStream());
            BufferedReader breader = new BufferedReader(ireader);
            StringBuilder builder = new StringBuilder();

            int checksum = 0;
            boolean inside = false;
            int ch;

            while ((ch = breader.read()) != -1) {
                if (ch == '$') {
                    inside = true;
                    checksum = 0;
                } else if (ch == '#') {
                    inside = false;
                    String message = builder.toString();

                    int check1 = breader.read();
                    int check2 = breader.read();

                    // Verify checksum. This should never fail since the transport layer assures
                    // correct transmission.
                    boolean match = (calcChecksum(check1, check2) == checksum);

                    String resp = match ? "+" : "-";
                    socket.getOutputStream().write(resp.getBytes());

                    if (!match)
                        throw new SessionException("Checksum mismatch");
                    return message;
                } else if (inside) {
                    if (ch == '}') {
                        checksum = (checksum + ch) & 0xFF;
                        ch = breader.read();
                        checksum = (checksum + ch) & 0xFF;
                        builder.append((char) (ch ^ 0x20));
                    } else {
                        checksum = (checksum + ch) & 0xFF;
                        builder.append((char) ch);
                    }
                } else {
                    // just drop characters until we read '$' again
                }
            }
        } catch (IOException e) {
            throw new SessionException("Failed to contact session", e);
        }

        // Unexpected end of file
        throw new SessionException("Lost session connection");
    }

    public Response command(String... args) throws SessionException {
        StringBuilder command = new StringBuilder();
        for (int i = 0; i < args.length - 1; i++)
            command.append(args[i] + ",");
        command.append(args[args.length - 1]);

        send(command.toString());
        Response resp = new Response(command.toString(), recv());

        if (resp.isError())
            throw new SessionException(resp.getValue(0));

        return resp;
    }

    public void close() throws SessionException {
        try {
            socket.close();
        } catch (IOException e) {
            // silently ignore this, we were disconnecting anyway
        }
    }

}
