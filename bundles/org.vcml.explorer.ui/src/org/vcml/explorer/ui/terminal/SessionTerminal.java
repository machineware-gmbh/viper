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

package org.vcml.explorer.ui.terminal;

import java.io.IOException;
import java.util.Arrays;

import org.eclipse.swt.widgets.Display;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.session.Command;
import org.vcml.session.Module;
import org.vcml.session.Session;
import org.vcml.session.SessionException;

public class SessionTerminal extends CommandTerminal {

    private Session session;

    private Module current;

    private Module[] getTopLevelObjects() {
        try {
            return session.getTopLevelObjects();
        } catch (SessionException e) {
            out.println(e);
            return new Module[0];
        }
    }

    private Module findChildModule(String name) {
        try {
            return current != null ? current.findChild(name) : session.findObject(name);
        } catch (SessionException e) {
            out.println(e.getMessage());
            return null;
        }
    }

    private void selectModule(Module newModule) {
        if (current != null) {
            for (Command c : current.getCommands())
                removeCommand(c.getName());
        }

        current = newModule;

        if (current != null) {
            for (Command c : current.getCommands()) {
                addCommand(c.getName(), (String[] args) -> {
                    try {
                        out.println(c.execute(Arrays.copyOfRange(args, 1, args.length)));
                    } catch (SessionException e) {
                        out.println(e.getMessage());
                    }
                });
            }
        }

        setPrompt((current == null ? "" : current.getName() + " ") + "> ");
    }

    public Session getSession() {
        return session;
    }

    public SessionTerminal(Session session, ISessionService service, boolean echo)
            throws IOException, SessionException {
        super(session.toString(), echo);

        this.session = session;
        this.current = null;

        setPrompt("> ");

        addCommand("ls", (String[] args) -> {
            Module[] modules = current != null ? current.getChildren() : getTopLevelObjects();
            for (Module module : modules)
                out.println(module.getBaseName());
        });

        addCommand("cd", (String[] args) -> {
            if (args.length < 2) {
                current = null;
                return;
            }

            String name = args[1];
            if (name.equals("..")) {
                selectModule(current.getParent());
                return;
            }

            Module next = findChildModule(name);
            if (next == null) {
                out.println("no such module '" + name + "'");
            } else {
                selectModule(next);
            }
        });

        addCommand("s", (String[] args) -> {
            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    service.stepSimulation(session);
                }
            });

        });

        addCommand("c", (String[] args) -> {

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    service.startSimulation(session);
                }
            });

            out.print("simulation running, hit any key to stop");
            out.flush();

            try {
                in.read();
                out.println();
            } catch (IOException e) {
                out.println(e.getMessage());
            }

            Display.getDefault().syncExec(new Runnable() {
                @Override
                public void run() {
                    service.stopSimulation(session);
                }
            });

        });

        addCommand("help", (String[] args) -> {
            out.println("Available commands:");
            out.println(String.format("%-10s : %s", "ls", "list modules"));
            out.println(String.format("%-10s : %s", "cd <name>", "select module <name>"));
            out.println(String.format("%-10s : %s", "cd ..", "select parent module"));
            out.println(String.format("%-10s : %s", "s", "single step simulation"));
            out.println(String.format("%-10s : %s", "c", "continue simulation"));
            if (current == null)
                return;
            for (Command c : current.getCommands())
                out.println(String.format("%-10s : %s", c.getName(), c.getDesc()));
        });
    }

    public SessionTerminal(Session session, ISessionService service) throws IOException, SessionException {
        this(session, service, false);
    }

}
