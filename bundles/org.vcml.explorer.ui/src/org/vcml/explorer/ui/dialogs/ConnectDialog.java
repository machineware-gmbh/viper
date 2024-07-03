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

package org.vcml.explorer.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.vcml.explorer.ui.Resources;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Spinner;

public class ConnectDialog extends TitleAreaDialog {

    private String sessionURI;
    private boolean connect;

    private Text nameText;
    private Text hostText;
    private Spinner portSpinner;
    private Button connectButton;
    private Button okButton;

    private ModifyListener modifyListener = new ModifyListener() {
        @Override
        public void modifyText(ModifyEvent e) {
            checkInput();
        }
    };

    private void checkInput() {
        okButton.setEnabled(!nameText.getText().isEmpty() && !hostText.getText().isEmpty());
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        container.setLayout(new GridLayout(2, false));

        Label nameLabel = new Label(container, SWT.NONE);
        nameLabel.setText("Name");

        nameText = new Text(container, SWT.BORDER);
        nameText.addModifyListener(modifyListener);
        nameText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        Label hostLabel = new Label(container, SWT.NONE);
        hostLabel.setText("Host");

        hostText = new Text(container, SWT.BORDER);
        hostText.addModifyListener(modifyListener);
        hostText.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        Label portLabel = new Label(container, SWT.NONE);
        portLabel.setText("Port");

        portSpinner = new Spinner(container, SWT.BORDER);
        portSpinner.setMinimum(1);
        portSpinner.setMaximum(65535);
        portSpinner.setSelection(4444);
        portSpinner.setLayoutData(new GridData(SWT.FILL, SWT.NONE, true, false));

        new Label(container, SWT.NONE); // just to fill the grid layout

        connectButton = new Button(container, SWT.CHECK);
        connectButton.setSelection(true);
        connectButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
        connectButton.setText("Connect immediately");

        return area;
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        okButton.setEnabled(false);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    public ConnectDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    public void create() {
        super.create();
        setTitle("Connect to simulation session on remote host");
        setMessage("Please enter the connection information", IMessageProvider.INFORMATION);
        setTitleImage(Resources.getImage("icons/new2x.png"));
    }

    public String getURI() {
        return sessionURI;
    }

    public boolean connectImmediately() {
        return connect;
    }

    @Override
    protected void okPressed() {
        sessionURI = hostText.getText() + ":" + portSpinner.getSelection() + ":" + System.getProperty("user.name") + ":"
                + nameText.getText();
        connect = connectButton.getSelection();
        super.okPressed();
    }

}
