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

package org.vcml.session;

import java.util.ArrayList;
import java.util.Arrays;

public class Module {

    public static final String KIND_SC_OBJECT = "sc_object";
    public static final String KIND_SC_MODULE = "sc_module";
    public static final String KIND_SC_SIGNAL = "sc_signal";
    public static final String KIND_SC_PORT = "sc_port";
    public static final String KIND_SC_EXPORT = "sc_export";
    public static final String KIND_SC_OUT = "sc_out";
    public static final String KIND_SC_IN = "sc_in";
    public static final String KIND_SC_METHOD_PROCESS = "sc_method_process";
    public static final String KIND_SC_THREAD_PROCESS = "sc_thread_process";
    public static final String KIND_TLM_INITIATOR_SOCKET = "tlm_initiator_socket";
    public static final String KIND_TLM_TARGET_SOCKET = "tlm_target_socket";

    public static final String KIND_VCML_IN_PORT = "vcml::in_port";
    public static final String KIND_VCML_OUT_PORT = "vcml::out_port";
    public static final String KIND_VCML_MASTER_SOCKET = "vcml::master_socket";
    public static final String KIND_VCML_SLAVE_SOCKET = "vcml::slave_socket";
    public static final String KIND_VCML_COMPONENT = "vcml::component";
    public static final String KIND_VCML_PROCESSOR = "vcml::processor";
    public static final String KIND_VCML_BUS = "vcml::bus";
    public static final String KIND_VCML_MEMORY = "vcml::memory";
    public static final String KIND_VCML_XBAR = "vcml::xbar";
    public static final String KIND_VCML_PERIPHERAL = "vcml::peripheral";
    public static final String KIND_VCML_UART8250 = "vcml::uart8250";
    public static final String KIND_VCML_ETHERNET = "vcml::ethernet";
    public static final String KIND_VCML_ARM_PL011UART = "vcml::arm::pl011uart";

    private RemoteSerialProtocol protocol;

    private String name;

    private String kind;

    private Module parent;

    private ArrayList<Module> children;

    private ArrayList<Attribute> attributes;

    private ArrayList<Command> commands;

    private void addChild(String name) {
        try {
            children.add(new Module(protocol, this, name));
        } catch (SessionException e) {
            System.err.println(name + ": " + e.getMessage());
        }
    }

    private void addAttribute(String name) {
        try {
            attributes.add(new Attribute(protocol, name));
        } catch (SessionException e) {
            System.err.println(name + ": " + e.getMessage());
        }
    }

    private void addCommand(String name) {
        commands.add(new Command(protocol, this, name));
    }

    private Response readObjectInfo() throws SessionException {
        String fullName = getName();
        if (fullName.isEmpty())
            return protocol.command(RemoteSerialProtocol.INFO);
        return protocol.command(RemoteSerialProtocol.INFO, fullName);

    }

    public boolean isRoot() {
        return parent == null;
    }

    public String getBaseName() {
        return name;
    }

    public String getName() {
        if ((parent == null) || parent.isRoot())
            return getBaseName();
        return parent.getName() + "." + getBaseName();
    }

    public String getKind() {
        return kind;
    }

    public Module getParent() {
        return parent;
    }

    public Module[] getChildren() {
        return children.toArray(new Module[children.size()]);
    }

    public Attribute[] getAttributes() {
        return attributes.toArray(new Attribute[attributes.size()]);
    }

    public Command[] getCommands() {
        return commands.toArray(new Command[commands.size()]);
    }

    public Module(RemoteSerialProtocol protocol, Module parent, String name) throws SessionException {
        this.protocol = protocol;
        this.parent = parent;
        this.name = name;
        this.children = new ArrayList<Module>();
        this.attributes = new ArrayList<Attribute>();
        this.commands = new ArrayList<Command>();

        Response info = readObjectInfo();

        String[] kindInfo = info.getValues("kind");
        this.kind = (kindInfo.length != 0) ? kindInfo[0] : "unknown";

        attributes.add(new Attribute("name", name));
        attributes.add(new Attribute("kind", kind));

        if (parent != null && !parent.isRoot())
            attributes.add(new Attribute("parent", parent.getName()));

        String[] childInfo = info.getValues("child");
        for (String child : childInfo)
            addChild(child);

        String[] attrInfo = info.getValues("attr");
        for (String attr : attrInfo)
            addAttribute(attr);

        String[] commands = info.getValues("cmd");
        for (String command : commands)
            addCommand(command);
    }

    public Module findChild(String name) {
        Module found = null;
        String[] names = name.split("\\.", 2);
        for (Module child : children) {
            if (child.getBaseName().equals(names[0])) {
                found = child;
                break;
            }
        }

        if (found == null)
            return null;
        return (names.length == 1) ? found : found.findChild(names[1]);
    }

    public Command findCommand(String name) {
        for (Command c : commands)
            if (c.getName().equals(name))
                return c;
        return null;
    }

    public String execute(String... args) throws SessionException {
        Command c = findCommand(args[0]);
        if (c == null)
            throw new SessionException("No such command: " + args[0]);

        if (args.length == 1)
            return c.execute();
        return c.execute(Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Module))
            return false;
        return getName().equals(((Module) other).getName());
    }

    @Override
    public String toString() {
        return getName();
    }

}
