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
import java.io.InputStream;
import java.io.OutputStream;

public abstract class Terminal {

    private String name;

    private boolean echo;

    public String getName() {
        return name;
    }

    public boolean isEcho() {
        return echo;
    }

    public void setEcho(boolean set) {
        echo = set;
    }

    public Terminal(String name, boolean echo) {
        this.name = name;
        this.echo = echo;
    }

    public Terminal(String name) {
        this(name, false);
    }

    public abstract InputStream getRx();

    public abstract OutputStream getTx();

    public void close() {
        try {
            getRx().close();
            getTx().close();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println(name + ": failed to close terminal rx/tx (" + e.getMessage() + ")");
        }
    }

    @Override
    public String toString() {
        return name;
    }

}
