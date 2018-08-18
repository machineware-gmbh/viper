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

package org.evcml.explorer.ui.terminal;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author Jan
 *
 */
public class CommandTerminal extends IOTerminal {

    public static final String DEFAULT_PROMPT = "> ";

    private String prompt = DEFAULT_PROMPT;

    private Map<String, Consumer<String[]>> commands;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String newPrompt) {
        prompt = newPrompt;
    }

    public void addCommand(String command, Consumer<String[]> func) {
        commands.put(command, func);
    }

    public void removeCommand(String command) {
        commands.remove(command);
    }

    public CommandTerminal(String name, boolean echo) throws IOException {
        super(name, echo);

        commands = new HashMap<>();

        // for testing
        addCommand("echo", (String[] args) -> {
            for (int i = 1; i < args.length; i++)
                out.print(args[i] + " ");
            out.println();
        });

        addCommand("pwd", (String[] args) -> {
            out.println("/proc/interrupts");
        });

        addCommand("kill", (String[] args) -> {
            if (args.length != 2) {
                out.println("insufficient arguments: kill [command]");
                return;
            }

            if (!commands.containsKey(args[1])) {
                out.println("command not found: '" + args[1] + "'");
                return;
            }

            removeCommand(args[1]);
            out.println("OK");
        });
    }

    public CommandTerminal(String name) throws IOException {
        this(name, false);
    }

    @Override
    public void loop() {
        try {
            int input;
            String command = "";

            out.print(prompt);
            out.flush();
            while ((input = in.read()) != -1) {
                switch (input) {
                case '\n':
                    out.write('\n');
                    out.flush();
                    exec(command);
                    command = "";
                    out.print(prompt);
                    out.flush();
                    break;

                case '\b':
                    if (command.length() > 0) {
                        command = command.substring(0, command.length() - 1);
                        out.write('\b');
                        out.flush();
                    }
                    break;

                default:
                    command += (char) input;
                    if (!isEcho()) {
                        out.write(input);
                        out.flush();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exec(String command) {
        String[] argv = command.split(" ");
        if (argv.length == 0)
            return;

        Consumer<String[]> consumer = commands.get(argv[0]);
        if (consumer != null) {
            consumer.accept(argv);
        } else {
            out.println("no such command: '" + command + "'");
        }
    }

}
