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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;

public class CommandDialog extends Dialog {
	
	private Module module;
	private Command command;
	private String help;

	private Text commandBoxInput;
	private Label commandBoxLabel;
	private Text descriptionBoxText;
	private Text responseBoxText;
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
			String args[] = commandBoxInput.getText().split(" ");
			String resp = command.execute(Arrays.copyOfRange(args, 1, args.length));
			responseBoxText.setText(resp);
		} catch (SessionException e) {
			responseBoxText.setText(e.getMessage());
		}
	}

	private void update() {
		String[] args = commandBoxInput.getText().split(" ");
		String name = args[0];
		command = module.findCommand(name);
		execBtn.setEnabled(false);
		if (command == null) {
			commandBoxLabel.setText("invalid command '" + commandBoxInput.getText() + "'");
			descriptionBoxText.setText(help);
			responseBoxText.setText("");
		} else {
			commandBoxLabel.setText(buildStatusString(commandBoxInput.getText()));
			descriptionBoxText.setText(command.getDesc());
			responseBoxText.setText("");
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
		setShellStyle(SWT.BORDER | SWT.CLOSE | SWT.RESIZE);
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
		container.setLayout(new GridLayout());
		
		Group commandBox = new Group(container, SWT.SHADOW_ETCHED_IN);
		commandBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		commandBox.setText("Command String");
		FillLayout commandBoxLayout = new FillLayout(SWT.VERTICAL);
		commandBoxLayout.marginWidth = 5;
		commandBoxLayout.marginHeight = 5;
		commandBox.setLayout(commandBoxLayout);
		commandBoxInput = new Text(commandBox, SWT.SINGLE | SWT.BORDER);
		commandBoxInput.setFont(Utils.getMonoSpaceFont());
		commandBoxInput.setText(command.getName());
		commandBoxInput.setSelection(command.getName().length());
		commandBoxInput.addModifyListener(modifyListener);
		commandBoxLabel = new Label(commandBox, SWT.NONE);
		commandBoxLabel.setForeground(display.getSystemColor(SWT.COLOR_RED));

		Group descriptionBox = new Group(container, SWT.NONE);
		descriptionBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		descriptionBox.setText("Command Description");
		FillLayout descriptionBoxLayout = new FillLayout();
		descriptionBoxLayout.marginHeight = 5;
		descriptionBoxLayout.marginWidth = 5;
		descriptionBox.setLayout(descriptionBoxLayout);
		descriptionBoxText = new Text(descriptionBox, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL);
		descriptionBoxText.setFont(Utils.getMonoSpaceFont());
		descriptionBoxText.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		Group responseBox = new Group(container, SWT.NONE);
		responseBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		responseBox.setText("Command Response");
		FillLayout responseBoxLayout = new FillLayout();
		responseBoxLayout.marginWidth = 5;
		responseBoxLayout.marginHeight = 5;
		responseBox.setLayout(responseBoxLayout);
		responseBoxText = new Text(responseBox, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL);
		responseBoxText.setFont(Utils.getMonoSpaceFont());
		responseBoxText.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

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
		newShell.setImage(Utils.getImage("chip.png"));
		newShell.setMinimumSize(new Point(500, 400));
		super.configureShell(newShell);
		newShell.setText("Execute module command");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(738, 588);
	}

}
