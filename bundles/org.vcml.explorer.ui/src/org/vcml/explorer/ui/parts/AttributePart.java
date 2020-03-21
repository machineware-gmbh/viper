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
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import org.vcml.explorer.ui.Resources;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.session.Attribute;
import org.vcml.session.Module;
import org.vcml.session.Session;
import org.vcml.session.SessionException;

public class AttributePart {

    @Inject
    private ISessionService sessionService;

    @Inject
    private ESelectionService selectionService;

    private String selectedModuleName = "";

    private Text filter;
    private TableViewer viewer;
    private TableViewerColumn attrColumn;
    private TableViewerColumn valueColumn;

    @Inject
    @Optional
    public void sessionChanged(@UIEventTopic(ISessionService.TOPIC_SESSION_ANY) Session session) {
        if (session == null || !session.isConnected() || session.isRunning() || selectedModuleName.isEmpty()) {
            viewer.setInput(null);
            return;
        }

        try {
            viewer.setInput(session.findObject(selectedModuleName));
        } catch (SessionException e) {
            selectedModuleName = "";
            viewer.setInput(null);
        }
    }

    @Inject
    public void selectionChanged(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Module selection) {
        if (selection != null) {
            selectedModuleName = selection.getName();
            viewer.setInput(selection);
        }
    }

    private ISelectionChangedListener viewerSelectionListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            IStructuredSelection selection = viewer.getStructuredSelection();
            selectionService.setSelection(selection.getFirstElement());
        }
    };

    private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
        @Override
        public Object[] getElements(Object inputElement) {
            if (inputElement instanceof Module)
                return ((Module) inputElement).getAttributes();
            return null;
        }
    };

    private ViewerFilter viewerFilter = new ViewerFilter() {
        @Override
        public boolean select(Viewer viewer, Object parentElement, Object element) {
            String match = ".*" + filter.getText() + ".*";
            Attribute attr = (Attribute) element;
            if (attr.getName().matches(match) || attr.getValuePretty().matches(match))
                return true;
            return false;
        }
    };

    private KeyListener keyListener = new KeyListener() {
        @Override
        public void keyReleased(KeyEvent e) {
            viewer.refresh();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            // Nothing to do
        }
    };

    private ColumnLabelProvider columnNameProvider = new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
            return ((Attribute) element).getBaseName();
        }

        @Override
        public Font getFont(Object element) {
            return Resources.getMonoSpaceFont();
        }

        @Override
        public String getToolTipText(Object element) {
            return ((Attribute) element).getName();
        }
    };

    private ColumnLabelProvider columnValueProvider = new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
            return ((Attribute) element).getValuePretty();
        }

        @Override
        public Font getFont(Object element) {
            return Resources.getMonoSpaceFont();
        }

        @Override
        public String getToolTipText(Object element) {
            return ((Attribute) element).getName();
        }
    };

    @PostConstruct
    public void createComposite(Composite parent) {
        parent.setLayout(new GridLayout());
        filter = new Text(parent, SWT.BORDER | SWT.SEARCH);
        filter.setMessage("search");
        filter.addKeyListener(keyListener);
        filter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        viewer = new TableViewer(composite, SWT.BORDER);
        viewer.setContentProvider(contentProvider);
        viewer.addFilter(viewerFilter);
        viewer.addSelectionChangedListener(viewerSelectionListener);

        attrColumn = new TableViewerColumn(viewer, SWT.NONE);
        attrColumn.getColumn().setText("Attribute");
        attrColumn.setLabelProvider(columnNameProvider);

        valueColumn = new TableViewerColumn(viewer, SWT.NONE);
        valueColumn.getColumn().setText("Value");
        valueColumn.setLabelProvider(columnValueProvider);
        valueColumn.setEditingSupport(new EditingSupport(viewer) {
            private TextCellEditor editor = new TextCellEditor(viewer.getTable());

            @Override
            protected void setValue(Object element, Object value) {
                try {
                    ((Attribute) element).setValue(value.toString());
                    viewer.update(element, null);
                } catch (SessionException e) {
                    sessionService.reportSessionError(sessionService.getSession(), e);
                }
            }

            @Override
            protected Object getValue(Object element) {
                return ((Attribute) element).getValue();
            }

            @Override
            protected CellEditor getCellEditor(Object element) {
                return editor;
            }

            @Override
            protected boolean canEdit(Object element) {
                return ((Attribute) element).isEditable();
            }
        });

        TableColumnLayout columnLayout = new TableColumnLayout();
        columnLayout.setColumnData(attrColumn.getColumn(), new ColumnWeightData(1, 100, false));
        columnLayout.setColumnData(valueColumn.getColumn(), new ColumnWeightData(1, 100, false));
        composite.setLayout(columnLayout);

        Table table = viewer.getTable();
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        ColumnViewerToolTipSupport.enableFor(viewer);
    }

    @Focus
    public void setFoucs() {
        viewer.getControl().setFocus();
    }

}
