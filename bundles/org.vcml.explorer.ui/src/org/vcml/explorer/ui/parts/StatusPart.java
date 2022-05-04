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

package org.vcml.explorer.ui.parts;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.vcml.explorer.ui.Resources;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.session.Attribute;
import org.vcml.session.Module;
import org.vcml.session.Session;

public class StatusPart {

    private Composite composite;

    private CLabel sessionLabel;

    private CLabel timeLabel;

    private CLabel cycleLabel;

    private CLabel selectionLabel;

    @Inject
    private ESelectionService selectionService;

    @Inject
    private ISessionService sessionService;

    private void updateSessionStatus() {
        Session session = sessionService.getSession();
        if (session == null) {
            sessionLabel.setText("not connected");
            sessionLabel.setImage(null);
        } else {
            sessionLabel.setText(session.toString());
            sessionLabel.setImage(Resources.getImageFor(session));
        }

        sessionLabel.pack();
    }

    private void updateTimeStatus() {
        Session session = sessionService.getSession();
        if (session == null || !session.isConnected()) {
            timeLabel.setVisible(false);
            cycleLabel.setVisible(false);
            return;
        }

        timeLabel.setVisible(true);
        cycleLabel.setVisible(true);

        timeLabel.setText(session.getTimeFormatted());
        cycleLabel.setText(Long.toString(session.getDeltaCycle()));

        //timeLabel.pack();
        //cycleLabel.pack();
    }

    private void updateSelectionStatus() {
        Object selection = selectionService.getSelection();
        if (selection instanceof Module) {
            selectionLabel.setVisible(true);
            selectionLabel.setText(((Module) selection).getName());
            selectionLabel.setImage(Resources.getImageFor(selection));
        } else if (selection instanceof Attribute) {
            selectionLabel.setVisible(true);
            selectionLabel.setText(((Attribute) selection).getName());
            selectionLabel.setImage(Resources.getImageFor(selection));
        } else {
            selectionLabel.setVisible(false);
        }

        selectionLabel.pack();
    }

    private void updateStatus() {
        updateSessionStatus();
        updateTimeStatus();
        updateSelectionStatus();
        composite.pack();
    }

    @PostConstruct
    public void createComposite(Composite parent) {
        composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout(4, false);
        layout.marginHeight = layout.marginWidth = 3;
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 1;
        composite.setLayout(layout);

        sessionLabel = new CLabel(composite, SWT.LEFT | SWT.BORDER);
        sessionLabel.setFont(Resources.getMonoSpaceFont());
        sessionLabel.setLeftMargin(10);
        sessionLabel.setRightMargin(20);
        sessionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        sessionLabel.setToolTipText("Active Session");

        timeLabel = new CLabel(composite, SWT.LEFT | SWT.BORDER);
        timeLabel.setFont(Resources.getMonoSpaceFont());
        timeLabel.setLeftMargin(10);
        timeLabel.setRightMargin(20);
        timeLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        timeLabel.setImage(Resources.getImage("icons/clock.png"));
        timeLabel.setToolTipText("Current SystemC Simulation Time");

        cycleLabel = new CLabel(composite, SWT.LEFT | SWT.BORDER);
        cycleLabel.setFont(Resources.getMonoSpaceFont());
        cycleLabel.setLeftMargin(10);
        cycleLabel.setRightMargin(20);
        cycleLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        cycleLabel.setImage(Resources.getImage("icons/refresh_alt.gif"));
        cycleLabel.setToolTipText("Current SystemC Delta Cycle");

        selectionLabel = new CLabel(composite, SWT.LEFT | SWT.BORDER);
        selectionLabel.setFont(Resources.getMonoSpaceFont());
        selectionLabel.setLeftMargin(10);
        selectionLabel.setRightMargin(20);
        selectionLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        selectionLabel.setImage(Resources.getImage("icons/attribute.gif"));
        selectionLabel.setToolTipText("Currently Selected Object");

        updateStatus();
    }

    @Inject
    @Optional
    public void sessionChanged(@UIEventTopic(ISessionService.TOPIC_SESSION_ANY) Session session) {
        updateStatus();
    }

    @Inject
    public void selectionChanged(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object selection) {
        if (composite != null && !composite.isDisposed())
            updateStatus();
    }

}
