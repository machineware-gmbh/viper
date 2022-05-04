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

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.vcml.session.Module;
import org.vcml.session.SessionException;

public class Symbol {

    public static final String CMD_LSYM = "lsym";

    // F c03c9de0 xprt_load_transport
    public static final String REGEX = "\\s([0-9a-fA-F]{16})\\s(\\w*)";

    private static Symbol[] findSymbols(Module module, String prefix) {
        ArrayList<Symbol> symbols = new ArrayList<Symbol>();
        Pattern pattern = Pattern.compile(prefix + REGEX);

        try {
            String response = module.execute(CMD_LSYM);
            String[] lines = response.split("\n");
            for (String line : lines) {
                Matcher matcher = pattern.matcher(line);

                if (!matcher.find() || matcher.groupCount() != 2)
                    continue;

                long address = Long.parseUnsignedLong(matcher.group(1), 16);
                String name = matcher.group(2);
                symbols.add(new Symbol(name, address, line.startsWith("F")));
            }
        } catch (SessionException e) {
            // ignore
        }

        Symbol[] array = new Symbol[symbols.size()];
        return symbols.toArray(array);
    }

    public static Symbol[] findSymbols(Module module) {
        return findSymbols(module, "[F|O]");
    }

    public static Symbol[] findObjects(Module module) {
        return findSymbols(module, "[O]");
    }

    public static Symbol[] findFunctions(Module module) {
        return findSymbols(module, "[F]");
    }

    private String name;

    private long address;

    private boolean func;

    public String getName() {
        return name;
    }

    public long getAddress() {
        return address;
    }

    public boolean isFunction() {
        return func;
    }

    public boolean isObject() {
        return !isFunction();
    }

    @Override
    public String toString() {
        String fmt = address > 0xffffffffl ? " @ 0x%016x" : " @ 0x%08x";
        return name + String.format(fmt, address);
    }

    public Symbol(String name, long address, boolean func) {
        this.name = name;
        this.address = address;
        this.func = func;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Symbol))
            return false;

        Symbol symbol = (Symbol) other;
        if (!name.equals(symbol.name))
            return false;
        if (address != symbol.address)
            return false;

        return true;
    }

}
