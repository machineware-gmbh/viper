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

package org.vcml.explorer.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vcml.session.Module;
import org.vcml.session.SessionException;

public class Instruction {

    public static final String CMD_DISASSEMBLE = "disas";

    public static final String REGEX_HEX = "[0-9a-fA-F]*";

    public static final String REGEX_SYM = "\\[.*\\]";

    public static final String REGEX_DISAS = ".*";

    public static final String REGEX_VADDR = "[0-9a-fA-F]+";

    public static final String REGEX_PADDR = "[0-9a-fA-F]+";

    public static final String REGEX_INSN = "\\[[[0-9a-fA-F]{2}\\s]*[[0-9a-fA-F]{2}]{1}\\]";

    public static final String REGEX = "\\s[>|\\s]\\s(" + REGEX_SYM + ")?\\s?(" + REGEX_VADDR + ")?\\s?(" + REGEX_PADDR
            + "):\\s(" + REGEX_INSN + ")\\s(" + REGEX_DISAS + ")?";

    private long physAddress;

    private long virtAddress;

    private String instruction;

    private long size;

    private String disassembly;

    private String symbol;

    private static String getDescription(long address, Module processor) {
        try { // ToDo: do not run the command every time, better use caching!
            String arg0 = Long.toUnsignedString(address);
            String arg1 = Long.toUnsignedString(address + 1);
            String result = processor.execute(CMD_DISASSEMBLE, arg0, arg1);
            return result.split("\n")[1];
        } catch (SessionException e) {
            return e.getMessage();
        }
    }

    public long getPhysicalAddress() {
        return physAddress;
    }

    public long getVirtualAddress() {
        return virtAddress;
    }

    public long getAddress() {
        return virtAddress != 0 ? virtAddress : physAddress;
    }

    public String getInstruction() {
        return instruction;
    }

    public long getSize() {
        return size;
    }

    public String getDisassembly() {
        return disassembly;
    }

    public String getSymbol() {
        return symbol;
    }

    public Instruction(long address, String description) {
        physAddress = address;
        virtAddress = 0;
        instruction = "";
        size = 4;
        disassembly = description;

        if (description.contains("????????")) {
            // something went wrong with v->p translation
            // try to at least get the symbol info
            virtAddress = address;
            disassembly = "<page unmapped>";

            Pattern pattern = Pattern.compile("(" + REGEX_SYM + ")");
            Matcher matcher = pattern.matcher(description);
            if ((matcher.find()) && (matcher.groupCount() == 1))
                symbol = matcher.group(1);

            return;
        }

        Pattern pattern = Pattern.compile(REGEX);
        Matcher matcher = pattern.matcher(description);

        if (!matcher.find())
            return;

        if (matcher.groupCount() != 5)
            return;

        try {
            String sym = matcher.group(1);
            String virt = matcher.group(2);
            String phys = matcher.group(3);
            String insn = matcher.group(4);
            String disas = matcher.group(5);

            symbol = (sym != null) ? sym : "";
            physAddress = Long.parseUnsignedLong(phys, 16);
            virtAddress = (virt != null) ? Long.parseUnsignedLong(virt, 16) : physAddress;
            instruction = insn;
            size = (insn.length() - 1) / 3;
            disassembly = (disas != null) ? disas.trim() : "--";

        } catch (NumberFormatException e) {
            disassembly = e.getMessage();
        }
    }

    public Instruction(long address, Module processor) {
        this(address, getDescription(address, processor));
    }

    @Override
    public String toString() {
        return disassembly;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Instruction))
            return false;

        Instruction insn = (Instruction) other;
        if ((insn.physAddress != physAddress) || (insn.virtAddress != virtAddress) || (insn.instruction != instruction))
            return false;

        return true;
    }

}
