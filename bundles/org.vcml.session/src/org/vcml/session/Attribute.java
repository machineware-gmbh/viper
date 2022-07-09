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

public class Attribute {

    private Module parent;

    private String base;

    private String name;

    private String type;

    private long count;

    private String[] values;

    private Session session;

    public Attribute(Module parent, String base, String type, long count) throws SessionException {
        assert count >= 0 : "count must not be negative";
        assert count <= Integer.MAX_VALUE : "attribute count limit exceeded";

        this.parent = parent;
        this.base = base;
        this.name = parent.getName() + Module.HIERARCHY_CHAR + getBaseName();
        this.type = type;
        this.count = count;
        this.values = count > 0 ? new String [(int)count] : null;
        this.session = parent.getSession();

        refresh();
    }

    public void refresh() throws SessionException {
        Protocol protocol = session.getProtocol();
        if (protocol == null)
            return;

        Response resp = protocol.command(Protocol.GETA, name);
        String[] values = resp.getValues();

        if (values.length > 1)
            System.err.println("Property " + name + "has multiple initializers");

        if (this.count > 1)
            values = values[0].split("\\s+");

        if (values.length == 0 || values.length != count)
            throw new SessionException("Failed to update attribute " + name);

        for (int i = 0; i < values.length; i++)
            this.values[i] = values[i];
    }

    public boolean isEditable() {
        return count > 0;
    }

    public Session getSession() {
        return session;
    }

    public Module getParent() {
        return parent;
    }

    public String getName() {
        return name;
    }

    public String getBaseName() {
        return base;
    }

    public String getType() {
        return type;
    }

    public long getCount() {
        return count;
    }

    public String getValue(int idx) {
        if (idx < 0 || idx >= count)
            return null;
        return values[idx];
    }

    public String getValue() {
        StringBuilder builder = new StringBuilder(getValue(0));

        for (int idx = 1; idx < values.length; idx++) {
            builder.append(", ");
            builder.append(getValue(idx));
        }

        return builder.toString();
    }

    public String getValuePretty(int idx) {
        String val = getValue(idx);
        if (val.isEmpty())
            return "<empty>";

        try {
            // ugly hack until we have something better
            if (name.contains("port") || name.contains("clock") || name.contains("size") ||
                name.contains("latency") || base.equals("session") || base.equals("nrcpu"))
                return String.format("%d", Long.valueOf(val));

            switch (type) {
            case  "i8": return String.format("%d", Long.parseUnsignedLong(val));
            case "i16": return String.format("%d", Long.parseUnsignedLong(val));
            case "i32": return String.format("%d", Long.parseUnsignedLong(val));
            case "i64": return String.format("%d", Long.parseUnsignedLong(val));
            case  "u8": return String.format("0x%02x",  Long.parseUnsignedLong(val));
            case "u16": return String.format("0x%04x",  Long.parseUnsignedLong(val));
            case "u32": return String.format("0x%08x",  Long.parseUnsignedLong(val));
            case "u64": return String.format("0x%016x", Long.parseUnsignedLong(val));
            default:
                return val;
            }
        } catch (Exception e) {
            return val; // we tried :>
        }
    }

    public String getValuePretty() {
        StringBuilder builder = new StringBuilder(getValuePretty(0));

        for (int idx = 1; idx < values.length; idx++) {
            builder.append(", ");
            builder.append(getValuePretty(idx));
        }

        return builder.toString();
    }

    public void setValue(String newValue, int idx) throws SessionException {
        if (idx < 0 || idx >= values.length)
            return;

        values[idx] = newValue;
        String vals = String.join(",", values);

        Protocol protocol = session.getProtocol();
        protocol.command(Protocol.SETA, name, vals);
        refresh();
    }

    public void setValue(String newValue) throws SessionException {
        if (!isEditable())
            return;

        String values[] = newValue.split(",", -1);
        if (values.length != count)
            return;

        Protocol protocol = session.getProtocol();
        if (protocol != null) {
            protocol.command(Protocol.SETA, name, newValue);
            refresh();
        }
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
