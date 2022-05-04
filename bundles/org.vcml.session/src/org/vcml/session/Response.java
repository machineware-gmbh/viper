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

import java.util.ArrayList;
import java.util.List;

public class Response {

    private String command;

    private String response;

    private String[] values;

    public boolean isError() {
        return !response.startsWith("OK");
    }

    public String getCommand() {
        return command;
    }

    public String getRaw() {
        return response;
    }

    public String getValue(int idx) {
        if (idx < 0 || idx >= values.length)
            return null;
        return values[idx];
    }

    public String[] getValues() {
        return values;
    }

    public int countValues() {
        return values.length;
    }

    public Response(String cmd, String resp) throws SessionException {
        if (resp.isEmpty())
            throw new SessionException("Command '" + command + "' not supported");

        this.command = cmd;
        this.response = resp;
        this.values = parseResponse(resp);
    }

    @Override
    public String toString() {
        return getRaw().replace("\\,", ",");
    }

    private static String[] parseResponse(String response) {
        List<String> entries = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();

        int idx = response.indexOf(',');
        if (idx == -1)
            return new String[0];

        for (int i = idx + 1; i < response.length(); i++) {
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
        return entries.toArray(new String[entries.size()]);
    }

}
