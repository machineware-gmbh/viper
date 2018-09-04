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
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;

import org.vcml.explorer.ui.MemoryRow;
import org.vcml.explorer.ui.Resources;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.session.Module;
import org.vcml.session.Session;

public class MemoryPart {

    public static final int COLUMN_WIDTH_ADDRESS = 120;

    public static final int COLUMN_WIDTH_DATA = 35;

    public static final int COLUMN_WIDTH_SPACER = 10;

    public static final int PAGE_SIZE = 4096;

    public static final int DEFAULT_INCREMENT = PAGE_SIZE / MemoryRow.SIZE;

    public static final String ERROR_CELL = "--";

    private ISessionService service;

    private Session session;

    private Module memory;

    private String name;

    private TableViewer viewer;

    private TableViewerColumn address;

    private Listener scrollListener = new Listener() {
        private int lastIndex = 0;

        public void handleEvent(Event e) {
            Table table = viewer.getTable();
            int index = table.getTopIndex();
            int count = table.getItemCount();

            int viewerHeight = table.getSize().y;
            int elementHeight = table.getItemHeight();
            int visibleElements = viewerHeight / elementHeight + 1;

            if (index != lastIndex) {
                lastIndex = index;
                if (index > (count - visibleElements))
                    viewer.setItemCount(count + DEFAULT_INCREMENT);
            }
        }
    };

    private IContentProvider contentProvider = new ILazyContentProvider() {
        @Override
        public void updateElement(int index) {
            viewer.replace(new MemoryRow(index * MemoryRow.SIZE, memory), index);
        }
    };

    private ColumnLabelProvider addressLabelProvider = new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
            return String.format("%08x", ((MemoryRow) element).getAddress());
        }

        @Override
        public Font getFont(Object element) {
            return Resources.getMonoSpaceFont();
        }

        @Override
        public Color getBackground(Object element) {
            return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
        }
    };

    private class DataColumnLabelProvider extends ColumnLabelProvider {

        private int column = -1;

        public DataColumnLabelProvider(int column) {
            this.column = column;
        }

        @Override
        public String getText(Object element) {
            if (!(element instanceof MemoryRow))
                return ERROR_CELL;

            MemoryRow row = (MemoryRow) element;
            if (row.isError())
                return ERROR_CELL;

            return String.format("%02x", row.getBytes()[column]);
        }

        @Override
        public Font getFont(Object element) {
            return Resources.getMonoSpaceFont();
        }
    };

    private static void addSpacerColumn(TableViewer viewer) {
        TableViewerColumn spacer = new TableViewerColumn(viewer, SWT.NONE);
        spacer.getColumn().setWidth(COLUMN_WIDTH_SPACER);
        spacer.setLabelProvider(new CellLabelProvider() {
            @Override
            public void update(ViewerCell cell) {
                cell.setText(" ");
            }
        });
    }

    @Inject
    public MemoryPart(ISessionService sessionService, ESelectionService selectionService) {
        service = sessionService;
        session = sessionService.getSession();
        memory = (Module) selectionService.getSelection();
        name = memory.getName();
    }

    @PostConstruct
    public void createComposite(Composite parent, ISessionService service) {
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new FillLayout());

        viewer = new TableViewer(composite, SWT.VIRTUAL);
        viewer.setUseHashlookup(true);
        viewer.setContentProvider(contentProvider);

        address = new TableViewerColumn(viewer, SWT.CENTER);
        address.getColumn().setText("Address");
        address.getColumn().setWidth(COLUMN_WIDTH_ADDRESS);
        address.setLabelProvider(addressLabelProvider);

        for (int i = 0; i < MemoryRow.SIZE; i++) {
            if ((i % 4) == 0)
                addSpacerColumn(viewer);

            TableViewerColumn column = new TableViewerColumn(viewer, SWT.CENTER);
            column.getColumn().setText(String.format("+%01X", i));
            column.getColumn().setWidth(COLUMN_WIDTH_DATA);
            column.setLabelProvider(new DataColumnLabelProvider(i));
        }

        addSpacerColumn(viewer);

        viewer.setInput(memory);
        viewer.setItemCount(DEFAULT_INCREMENT);

        Table table = viewer.getTable();
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        table.setHeaderVisible(true);
        table.setLinesVisible(false);

        table.addListener(SWT.MouseDown, scrollListener);
        table.addListener(SWT.MouseUp, scrollListener);
        table.addListener(SWT.KeyDown, scrollListener);
        table.addListener(SWT.KeyUp, scrollListener);
        table.addListener(SWT.Resize, scrollListener);
        table.getVerticalBar().addListener(SWT.Selection, scrollListener);
    }

    public void update() {
        memory = service.findModule(session, name);
        viewer.setInput(memory);
        viewer.getControl().setEnabled(memory != null);
    }

    @Inject
    @Optional
    public void sessionChanged(@UIEventTopic(ISessionService.TOPIC_SESSION_ANY) Session session) {
        if (session == this.session) {
            update();
        }
    }

}
