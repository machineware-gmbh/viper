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

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

import org.vcml.explorer.ui.Resources;
import org.vcml.explorer.ui.services.ISessionService;

import org.vcml.session.Session;

@SuppressWarnings("restriction")
public class SessionPart {

    public static final String MENU_ID = "org.vcml.explorer.ui.popupmenu.sessions";

    @Inject
    private ISessionService sessionService;

    @Inject
    private ESelectionService selectionService;

    @Inject
    private EMenuService menuService;

    @Inject
    private ECommandService commandService;

    @Inject
    private EHandlerService handlerService;

    private TableViewer viewer;

    private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
        @Override
        public Object[] getElements(Object inputElement) {
            return sessionService.getSessions().toArray();
        }
    };

    private ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            Object selection = viewer.getStructuredSelection().getFirstElement();
            sessionService.setSession((Session) selection);
            selectionService.setSelection(selection);
        }
    };

    private IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
        @Override
        public void doubleClick(DoubleClickEvent event) {
            Object selection = selectionService.getSelection();
            if (selection instanceof Session) {
                Session session = (Session) selection;
                String commandId = "org.vcml.explorer.ui.command." + (session.isConnected() ? "disconnect" : "connect");
                ParameterizedCommand command = commandService.createCommand(commandId, null);
                handlerService.executeHandler(command);
            }
        }
    };

    private ColumnLabelProvider labelProvider = new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
            Session session = (Session) element;
            String desc = session.toString();
            if (session.isRunning())
                desc += " [running]";
            else if (session.isConnected())
                desc += " [connected]";
            else
                desc += " [not connected]";
            return desc;
        }

        @Override
        public Image getImage(Object element) {
            return Resources.getImageFor(element);
        }

        @Override
        public String getToolTipText(Object element) {
            Session session = (Session) element;
            return session.getExecutable();
        }
    };

    @PostConstruct
    public void createComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new TableViewer(composite, SWT.BORDER);
        viewer.setLabelProvider(new LabelProvider());
        viewer.setContentProvider(contentProvider);
        viewer.addSelectionChangedListener(selectionListener);
        viewer.addDoubleClickListener(doubleClickListener);

        TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.LEFT);
        nameColumn.getColumn().setText("Available Sessions");
        nameColumn.setLabelProvider(labelProvider);

        TableColumnLayout columnLayout = new TableColumnLayout();
        columnLayout.setColumnData(nameColumn.getColumn(), new ColumnWeightData(1, 100, false));
        composite.setLayout(columnLayout);

        viewer.setInput(sessionService);
        ColumnViewerToolTipSupport.enableFor(viewer);
        menuService.registerContextMenu(viewer.getControl(), MENU_ID);
    }

    @Focus
    public void setFocus() {
        viewer.getTable().setFocus();
    }

    @Inject
    @Optional
    public void sessionChanged(@UIEventTopic(ISessionService.TOPIC_SESSION_ANY) Session session) {
        viewer.refresh();
    }

    @Inject
    @Optional
    public void sessionSelectionChanged(@UIEventTopic(ISessionService.TOPIC_SESSION_SELECTED) Session session) {
        Session selected = (Session) viewer.getStructuredSelection().getFirstElement();
        if (session != selected) {
            viewer.setSelection(new StructuredSelection(session));
            viewer.refresh();
        }
    }

}
