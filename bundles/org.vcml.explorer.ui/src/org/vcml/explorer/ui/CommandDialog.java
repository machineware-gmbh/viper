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

import java.util.Arrays;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.vcml.session.Command;
import org.vcml.session.Module;
import org.vcml.session.SessionException;

public class CommandDialog extends Dialog {
	
	private Module module;
	private Command command;
	private String help;
	
	private Text cmdText;
	private Label cmdStatus;
	private Text descText;
	private Text respText;
	private Button execBtn;
	
	private String buildStatusString(String input) {
		String[] s = input.split(" ");
		int argc = s.length - 1;
		if (argc < command.getArgc())
			return "not enough arguments specified (" + argc + " given, " + command.getArgc() + " required)";
		else
			return "";
	}
	
	private static String buildCommandList(Module module) {
		String list = "Module " + module.getName() + " supports the following commands:\n";
	    for (Command c : module.getCommands())
	    	list += "  " + c.getName() + ": " + c.getDesc() + "\n";
	    return list;
	}
	
	private void execute() {
		try {
			String args[] = cmdText.getText().split(" ");
			String resp = command.execute(Arrays.copyOfRange(args, 1, args.length));
			respText.setText(resp);
		} catch (SessionException e) {
			respText.setText(e.getMessage());
		}
	}

	private void update() {
		String[] args = cmdText.getText().split(" ");
		String name = args[0];
		command = module.findCommand(name);
		execBtn.setEnabled(false);
		if (command == null) {
			cmdStatus.setText("invalid command '" + cmdText.getText() + "'");
			descText.setText(help);
			respText.setText("");
		} else {
			cmdStatus.setText(buildStatusString(cmdText.getText()));
			descText.setText(command.getDesc());
			respText.setText("");
			if (args.length >= (command.getArgc() + 1))
				execBtn.setEnabled(true);
		}
	}
	
	private ModifyListener modifyListener = new ModifyListener() {
		@Override
		public void modifyText(ModifyEvent e) {
			update();
		}
	};

	public CommandDialog(Shell parentShell, Module module, Command command) {
		super(parentShell);
		this.module = module;
		this.command = command;
		this.help = buildCommandList(module);
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Control control = super.createContents(parent);
		update();
		if (command.getArgc() == 0)
			execute();
		return control;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Display display = Display.getDefault();
		Composite container = (Composite)super.createDialogArea(parent);
		container.setLayout(new FillLayout(SWT.VERTICAL));
		
		Group cmdGroup = new Group(container, SWT.SHADOW_ETCHED_IN);
		cmdGroup.setText("Command String");
		cmdGroup.setLayout(new FillLayout(SWT.VERTICAL));
		cmdText = new Text(cmdGroup, SWT.SINGLE | SWT.BORDER);
		cmdText.setFont(Utils.getMonoSpaceFont());
		cmdText.setText(command.getName());
		cmdText.addModifyListener(modifyListener);
		cmdStatus = new Label(cmdGroup, SWT.NONE);
		cmdStatus.setForeground(display.getSystemColor(SWT.COLOR_RED));

		Group descGroup = new Group(container, SWT.NONE);
		descGroup.setText("Command Description");
		descGroup.setLayout(new FillLayout());
		descText = new Text(descGroup, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL);
		descText.setFont(Utils.getMonoSpaceFont());
		descText.setBackground(display.getSystemColor(SWT.COLOR_GRAY));

		Group respGroup = new Group(container, SWT.NONE);
		respGroup.setText("Command Response");
		respGroup.setLayout(new FillLayout());
		respText = new Text(respGroup, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL);
		respText.setFont(Utils.getMonoSpaceFont());
		respText.setBackground(display.getSystemColor(SWT.COLOR_GRAY));

		return container;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		execBtn = createButton(parent, IDialogConstants.OK_ID, "Execute", true);
		execBtn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, "Close", false);
	}

	@Override
	protected void okPressed() {
		execute();
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Execute module command");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(600, 400);
	}

}
