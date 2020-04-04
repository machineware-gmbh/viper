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
import java.util.List;

public class Response {

    private String command;

    private String response;

    private List<String> entries;

    public Response(String cmd, String resp) throws SessionException {
        this.command = cmd;
        this.response = resp;
        this.entries = new ArrayList<String>();

        if (response.isEmpty())
            throw new SessionException("Command '" + command + "' not supported");
        if (response.startsWith("ERROR,"))
            throw new SessionException("Command '" + command + "' returned error: " + response.substring(6));
        if (response.startsWith("E,"))
            throw new SessionException("Command '" + command + "' returned error: " + response.substring(2));
        if (response.startsWith("OK"))
            response = response.substring(2);
        if (response.startsWith(","))
            response = response.substring(1);

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < response.length(); i++) {
            char c = response.charAt(i);
            switch  (c) {
            case '\\':
                builder.append(response.charAt(++i));
                break;

            case ',':
                entries.add(builder.toString());
                builder = new StringBuilder();
                break;

            default:
                builder.append(c);
                break;
            }

        }

        entries.add(builder.toString());
    }

    public String getRaw() {
        return response;
    }

    public String[] getValues() {
        return entries.toArray(new String[entries.size()]);
    }

    @Override
    public String toString() {
        return response.replace("\\,", ",");
    }

}
