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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

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

    public static final String HIERARCHY_CHAR = ".";

    private Session session;

    private String name;

    private String kind;

    private Module parent;

    private ArrayList<Module> children;

    private ArrayList<Attribute> attributes;

    private ArrayList<Command> commands;

    private void parseXML(String xml) throws XMLStreamException, SessionException {
        InputStream in = new ByteArrayInputStream(xml.getBytes());
        XMLInputFactory inputFactory = XMLInputFactory.newInstance();
        XMLStreamReader streamReader = inputFactory.createXMLStreamReader(in);

        Module current = this;
        while (streamReader.hasNext()) {
            streamReader.next();
            switch (streamReader.getEventType()) {
            case XMLStreamReader.START_ELEMENT: {
                if (streamReader.getLocalName().equalsIgnoreCase("object")) {
                    String name = streamReader.getAttributeValue(null, "name");
                    String kind = streamReader.getAttributeValue(null, "kind");
                    current = new Module(current, name, kind);
                }

                if (streamReader.getLocalName().equalsIgnoreCase("attribute")) {
                    String name = streamReader.getAttributeValue(null, "name");
                    String type = streamReader.getAttributeValue(null, "type");
                    Long count = Long.parseLong(streamReader.getAttributeValue(null, "count"));
                    current.attributes.add(new Attribute(current, name, type, count));
                }

                if (streamReader.getLocalName().equalsIgnoreCase("command")) {
                    String name = streamReader.getAttributeValue(null, "name");
                    String desc = streamReader.getAttributeValue(null, "type");
                    int argc = Integer.parseInt(streamReader.getAttributeValue(null, "argc"));
                    current.commands.add(new Command(current, name, desc, argc));
                }

                break;
            }

            case XMLStreamReader.END_ELEMENT: {
                if (streamReader.getLocalName().equalsIgnoreCase("object"))
                    current = current.getParent();

                break;
            }

            default:
                break;
            }
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public Session getSession() {
        return session;
    }

    public String getBaseName() {
        return name;
    }

    public String getName() {
        if ((parent == null) || parent.isRoot())
            return getBaseName();
        return parent.getName() + HIERARCHY_CHAR + getBaseName();
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

    private Module(Module parent, String name, String kind) {
        this.parent = parent;
        this.session = parent.getSession();
        this.name = name;
        this.kind = kind;
        this.children = new ArrayList<Module>();
        this.attributes = new ArrayList<Attribute>();
        this.commands = new ArrayList<Command>();
        this.parent.children.add(this);
    }

    public Module(Session session) throws SessionException {
        this.session = session;
        this.parent = null;
        this.name = "root";
        this.children = new ArrayList<Module>();
        this.attributes = new ArrayList<Attribute>();
        this.commands = new ArrayList<Command>();

        RemoteSerialProtocol protocol = session.getProtocol();
        Response resp = protocol.command(RemoteSerialProtocol.LIST, "xml");

        try {
            parseXML(resp.getValue(0));
        } catch (XMLStreamException e) {
            throw new SessionException("failed to parse object hierarchy", e);
        }
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

    public void refresh() throws SessionException {
        for (Attribute attr : attributes)
            attr.refresh();
        for (Module child : children)
            child.refresh();
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
