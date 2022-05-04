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

package org.vcml.explorer.ui;

import org.vcml.session.Module;

public class MemoryRow {

    public static final int SIZE = 16;

    private boolean error = false;

    private int address;

    private byte[] bytes = new byte[SIZE];

    public boolean isError() {
        return error;
    }

    public int getAddress() {
        return address;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public MemoryRow(int address, Module module) {
        this.address = address;

        try {
            String arg0 = Integer.toString(address);
            String arg1 = Integer.toString(address + SIZE);
            String result = module.execute("show", arg0, arg1);
            String clean = result.substring(result.indexOf(':') + 1);
            String[] values = clean.split(" ");

            int idx = 0;
            for (int i = 0; i < values.length; i++)
                if (values[i].length() == 2)
                    bytes[idx++] = (byte) Integer.parseInt(values[i], 16);
        } catch (Exception e) {
            for (int i = 0; i < SIZE; i++)
                bytes[i] = (byte) 0xee;
            error = true;
        }
    }

    @Override
    public String toString() {
        String desc = String.format("0x%08x:", address);
        for (int i = 0; i < SIZE; i++)
            desc += String.format(" %02x", bytes[i]);
        return desc;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MemoryRow))
            return false;

        MemoryRow row = (MemoryRow) other;
        if (row.getAddress() != address)
            return false;

        for (int i = 0; i < SIZE; i++) {
            if (row.getBytes()[i] != bytes[i])
                return false;
        }

        return true;
    }

}