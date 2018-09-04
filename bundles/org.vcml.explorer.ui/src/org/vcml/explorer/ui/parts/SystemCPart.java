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

package org.vcml.explorer.ui.parts;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;

import org.vcml.explorer.ui.Resources;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.session.Session;

public class SystemCPart {

    private Session session;

    private TableViewer viewer;

    private TableViewerColumn propColumn;

    private TableViewerColumn descColumn;

    private final String attributes[] = { "Name", "Host", "Port", "User", "Path", "SystemC Version", "VCML Version",
            "Time", "Delta Cycle" };

    private ColumnLabelProvider descriptionProvider = new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
            switch ((String) element) {
            case "Name":
                return session.getName();
            case "Host":
                return session.getHost();
            case "Port":
                return Integer.toString(session.getPort());
            case "User":
                return session.getUser();
            case "Path":
                return session.getExecutable();
            case "SystemC Version":
                return session.getSystemCVersion();
            case "VCML Version":
                return session.getVCMLVersion();
            case "Time":
                return String.format("%.9fs", session.getTime());
            case "Delta Cycle":
                return Integer.toString(session.getDeltaCycle());

            default:
                return "unknown";
            }
        }

        @Override
        public Font getFont(Object element) {
            return Resources.getMonoSpaceFont();
        }
    };

    @PostConstruct
    public void createComposite(Composite parent, ISessionService service) {
        session = service.getSession();
        parent.setLayout(new GridLayout());
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new TableViewer(composite, SWT.BORDER);
        viewer.setContentProvider(ArrayContentProvider.getInstance());

        propColumn = new TableViewerColumn(viewer, SWT.NONE);
        propColumn.getColumn().setText("Attribute");
        propColumn.setLabelProvider(new ColumnLabelProvider());

        descColumn = new TableViewerColumn(viewer, SWT.NONE);
        descColumn.getColumn().setText("Description");
        descColumn.setLabelProvider(descriptionProvider);

        TableColumnLayout columnLayout = new TableColumnLayout();
        columnLayout.setColumnData(propColumn.getColumn(), new ColumnWeightData(1, 100, false));
        columnLayout.setColumnData(descColumn.getColumn(), new ColumnWeightData(3, 100, false));
        composite.setLayout(columnLayout);

        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        viewer.setInput(attributes);
    }

    @Focus
    public void setFocus() {
        viewer.getControl().setFocus();
    }

    @Inject
    @Optional
    public void sessionChanged(@UIEventTopic(ISessionService.TOPIC_SESSION_ANY) Session current) {
        if (session == current) {
            viewer.refresh();
            viewer.getTable().setEnabled(session.isConnected() && !session.isRunning());
        }
    }
}
