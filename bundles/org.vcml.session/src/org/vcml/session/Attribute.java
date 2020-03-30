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

public class Attribute {

    private String name;

    private String value;

    private int size;

    private int num;

    private Session session;

    private RemoteSerialProtocol protocol;

    public Attribute(String name, String value) {
        this.name = name;
        this.value = value;
        this.session = null;
        this.protocol = null;
    }

    public Attribute(Session session, String name) throws SessionException {
        this.name = name;
        this.value = "unknown";
        this.session = session;
        this.protocol = session.getProtocol();

        refresh();
    }

    public void refresh() throws SessionException {
        if (protocol == null)
            return;

        Response resp = protocol.command(RemoteSerialProtocol.GETA, name);
        String[] values = resp.getValues();
        if (values.length == 0)
            throw new SessionException("Failed to read attribute " + name);

        name = values[0];
        value = values.length > 1 ? values[1] : "";
        size = values.length > 2 ? Integer.parseInt(values[2]) : -1;
        num = values.length > 3 ? Integer.parseInt(values[3]) : 1;
    }

    public boolean isEditable() {
        return protocol != null;
    }

    public Session getSession() {
        return session;
    }

    public String getName() {
        return name;
    }

    public String getBaseName() {
        String[] temp = name.split("\\.");
        return temp[temp.length - 1];
    }

    public String getValue() {
        return value;
    }

    public int getSize() {
        return size;
    }

    public int getNumValues() {
        return num;
    }

    public int getArraySize() {
        return getNumValues() * getSize();
    }

    public boolean isArray() {
        return getNumValues() > 1;
    }

    public String getValuePretty() {
        if (value.isEmpty())
            return "<empty>";

        if (isArray())
            return value;

        try {
            long val = Long.parseLong(value);
            switch (size) {
            case 1  : return String.format("0x%02x",  val);
            case 2  : return String.format("0x%04x",  val);
            case 4  : return String.format("0x%08x",  val);
            case 8  : return String.format("0x%016x", val);
            default : return String.format("0x%x",    val);
            }
        } catch (NumberFormatException e) {
            // return "\"" + value + "\"";
            return value;
        }
    }

    public void setValue(String newValue) throws SessionException {
        if (!isEditable() || (newValue == value))
            return;

        protocol.command(RemoteSerialProtocol.SETA, name, newValue);
        refresh();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Attribute))
            return false;
        return getName().equals(((Attribute) other).getName());
    }

    @Override
    public String toString() {
        return getName();
    }

}
