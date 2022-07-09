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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Path;

public class Session {

    public final static String ANNOUNCE_DIR = System.getProperty("java.io.tmpdir");

    private String uri = "";

    private String host = "";

    private int port = 0;

    private String exec = "<unknown>";

    private String user = "<unknown>";

    private String name = "<unknown>";

    private Protocol protocol = null;

    private Module hierarchy = null;

    private LocalTime simTime = LocalTime.MIN;

    private Duration quantum;

    private long deltaCycle = -1;

    private String syscVersion = "<unknown>";

    private String vcmlVersion = "<unknown>";

    private boolean running = false;

    private String stopReason = "";

    public String getURI() {
        return uri;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public String getExecutable() {
        return exec;
    }

    public LocalTime getTime() {
        return simTime;
    }

    public String getTimeFormatted() {
        long hour = simTime.getHour();
        long minute = simTime.getMinute();
        long second = simTime.getSecond();
        long nanos = simTime.getNano();
        return String.format("%02d:%02d:%02d.%09d", hour, minute, second, nanos);
    }

    public Duration getQuantum() {
        return quantum;
    }

    public long getDeltaCycle() {
        return deltaCycle;
    }

    public String getSystemCVersion() {
        return syscVersion;
    }

    public String getVCMLVersion() {
        return vcmlVersion;
    }

    public Protocol getProtocol() {
        return protocol;
    }

    public boolean isConnected() {
        return protocol != null;
    }

    public boolean isRunning() {
        return running;
    }

    public String getStopReason() {
        return stopReason;
    }

    @Override
    public String toString() {
        return user + "/" + name + " at " + host + ":" + port;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;

        if (!(other instanceof Session))
            return false;

        Session session = (Session) other;
        return uri.equals(session.getURI());
    }

    private void updateVersion() throws SessionException {
        Response resp = protocol.command(Protocol.VERS);
        String version[] = resp.getValues();
        if (version.length != 2)
            throw new SessionException("received bogus response from session: " + resp.toString());

        syscVersion = version[0];
        vcmlVersion = version[1];
    }

    public void updateTime() throws SessionException {
        Response resp = null;
        if (isRunning()) {
            protocol.send_char('u');
            resp = new Response(Protocol.TIME, protocol.recv());
        } else {
            resp = protocol.command(Protocol.TIME);
        }

        String values[] = resp.getValues();
        if (values.length != 2)
            throw new SessionException("session returned invalid response: " + resp);

        deltaCycle = Long.parseLong(values[1]);
        long nanos = Long.parseLong(values[0]);
        simTime = LocalTime.ofNanoOfDay(nanos);
    }

    public void updateQuantum() throws SessionException {
        if (isRunning())
            return;

        Response resp = protocol.command(Protocol.GETQ);
        String values[] = resp.getValues();

        if (values.length != 1)
            throw new SessionException("session returned invalid response: " + resp);

        quantum = Duration.ofNanos(Long.parseLong(values[0]));
        System.out.println("quantum:" + quantum);
    }

    public Session(String uri) throws SessionException {
        this.uri = uri;

        String[] info = uri.split(":");
        if (info.length >= 2) {
            host = info[0];
            port = Integer.parseInt(info[1]);
            if (info.length > 2)
                user = info[2];
            if (info.length > 3) {
                exec = info[3];

                Path path = new Path(exec);
                name = path.segment(path.segmentCount() - 1);
            }
        }

        if (host.isEmpty() || port == 0)
            throw new SessionException("invalid URI: " + uri);
    }

    public void connect() throws SessionException {
        if (isConnected())
            return;

        protocol = new Protocol(host, port);

        updateVersion();
        updateTime();
        updateQuantum();
    }

    public void disconnect() throws SessionException {
        if (!isConnected())
            return;

        hierarchy = null;
        protocol.close();
        protocol = null;
    }

    public void refresh() throws SessionException {
        updateTime();
        hierarchy.refresh();
    }

    public Module[] getTopLevelObjects() throws SessionException {
        if (!isConnected())
            return null;

        if (hierarchy == null)
            hierarchy = new Module(this);
        return hierarchy.getChildren();
    }

    public Module findObject(String name) throws SessionException {
        if (hierarchy == null)
            hierarchy = new Module(this);
        return hierarchy.findChild(name);
    }

    public void continueSimulation() throws SessionException {
        if (!isConnected() || isRunning())
            return;

        protocol.send(Protocol.CONT);
        running = true;
    }

    public void stopSimulation() throws SessionException {
        if (!isConnected() || !isRunning())
            return;

        protocol.send_char('a');
        String resp = protocol.recv();
        if (!resp.startsWith("OK"))
            throw new SessionException("Simulator responded with error : " + resp);

        running = false;
        stopReason = "unknown";
        if (resp.length() > 3)
            stopReason = resp.substring(3);

        refresh();
    }

    public void stepSimulation() throws SessionException {
        if (!isConnected() || isRunning())
            return;

        String duration = String.format("%dns", quantum.toNanos());
        protocol.command(Protocol.CONT, duration);
        refresh();
    }

    public void quitSimulation() throws SessionException {
        if (!isConnected())
            return;

        protocol.send(Protocol.QUIT);
        running = false;
    }

    public static List<Session> getAvailableSessions() {
        List<Session> avail = new ArrayList<Session>();

        File directory = new File(ANNOUNCE_DIR);
        File[] files = directory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return Pattern.matches("vcml_session_[0-9]+", name);
            }
        });

        for (File it : files) {
            try {
                Scanner scanner = new Scanner(it);
                try {
                    String uri = scanner.nextLine();
                    Session session = new Session(uri);
                    if (!avail.contains(session))
                        avail.add(session);
                } catch (SessionException ex) {
                    System.err.println(ex.getMessage());
                } finally {
                    scanner.close();
                }
            } catch (FileNotFoundException e) {
                /* nothing to do */
            }
        }

        return avail;
    }

}
