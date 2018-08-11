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

import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.e4.ui.workbench.modeling.ISelectionListener;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.vcml.explorer.ui.Utils;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.session.Attribute;
import org.vcml.session.Module;
import org.vcml.session.SessionException;

public class AttributePart {

    @Inject
    private ISessionService sessionService;

    @Inject
    private ESelectionService selectionService;

    private TableViewer viewer;
    private TableViewerColumn attrColumn;
    private TableViewerColumn valueColumn;

    private IPropertyChangeListener sessionListener = new IPropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            String property = event.getProperty();
            if (property.equals(ISessionService.PROP_UPDATED) || property.equals(ISessionService.PROP_REMOVED)) {
                refresh(null);
            }
        }
    };

    private ISelectionListener selectionListener = new ISelectionListener() {
        @Override
        public void selectionChanged(MPart part, Object selection) {
            if (selection instanceof Module)
                refresh((Module) selection);
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

    private ColumnLabelProvider columnNameProvider = new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
            return ((Attribute) element).getBaseName();
        }

        @Override
        public Font getFont(Object element) {
            return Utils.getMonoSpaceFont(); // JFaceResources.getFont?
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
            return Utils.getMonoSpaceFont(); // JFaceResources.getFont?
        }

        @Override
        public String getToolTipText(Object element) {
            return ((Attribute) element).getName();
        }
    };

    private void refresh(Module module) {
        viewer.setInput(module);
        attrColumn.getColumn().pack();
        valueColumn.getColumn().pack();
        viewer.refresh();
    }

    public AttributePart() {

    }

    public void dispose() {
        sessionService.removeSessionChangeListener(sessionListener);
        selectionService.removeSelectionListener(selectionListener);
    }

    @PostConstruct
    public void createComposite(Composite parent) {
        sessionService.addSessionChangeListener(sessionListener);
        selectionService.addSelectionListener(selectionListener);

        viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        viewer.setContentProvider(contentProvider);

        attrColumn = new TableViewerColumn(viewer, SWT.LEAD);
        attrColumn.getColumn().setText("Attribute");
        attrColumn.setLabelProvider(columnNameProvider);

        valueColumn = new TableViewerColumn(viewer, SWT.LEAD);
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
                    sessionService.reportSessionError(sessionService.currentSession(), e);
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

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        data.verticalAlignment = SWT.FILL;

        GridLayout layout = new GridLayout();
        // layout.m
        parent.setLayout(layout);

        Table table = viewer.getTable();
        table.setLayoutData(data);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);

        ColumnViewerToolTipSupport.enableFor(viewer);
        parent.pack();
        viewer.refresh();
    }

    @Focus
    public void setFoucs() {
        viewer.getControl().setFocus();
    }

}
