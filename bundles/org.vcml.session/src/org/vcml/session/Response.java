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

    private List<KeyValuePair> entries;

    private class KeyValuePair {
        public String key;
        public String val;

        public KeyValuePair(String key, String val) {
            this.key = key;
            this.val = val;
        }
    }

    public Response(String cmd, String resp) throws SessionException {
        this.command = cmd;
        this.response = resp;
        this.entries = new ArrayList<KeyValuePair>();

        if (response.isEmpty())
            throw new SessionException("Command '" + command + "' not supported");
        if (response.startsWith("ERROR,"))
            throw new SessionException("Command '" + command + "' returned error: " + response.substring(6));
        if (response.startsWith("OK"))
            response = response.substring(2);
        if (response.startsWith(","))
            response = response.substring(1);

        //String token[] = response.split("(?<!\\\\),"); // just 1 char look-back :(
        ArrayList<String> token = new ArrayList<String>();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < response.length(); i++) {
            char c = response.charAt(i);
            switch  (c) {
            case '\\':
                builder.append(response.charAt(++i));
                break;

            case ',':
                token.add(builder.toString());
                builder = new StringBuilder();
                break;

            default:
                builder.append(c);
                break;
            }

        }

        if (builder.length() > 0)
            token.add(builder.toString());

        for (String entry : token) {
            String[] data = entry.split(":", 2);
            String key = data.length > 1 ? data[0] : "";
            String val = data.length > 1 ? data[1] : data[0];
            entries.add(new KeyValuePair(key, val));
        }
    }

    @Override
    public String toString() {
        return response.replaceAll("\\\\,", ",");
    }

    public String[] getValues(String key) {
        ArrayList<String> list = new ArrayList<String>();
        for (KeyValuePair pair : entries) {
            if (pair.key.equals(key))
                list.add(pair.val);
        }

        return list.toArray(new String[list.size()]);
    }

    public String[] getValues() {
        return getValues("");
    }

}
