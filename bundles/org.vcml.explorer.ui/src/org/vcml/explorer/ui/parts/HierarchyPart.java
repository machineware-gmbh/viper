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

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.e4.ui.workbench.modeling.ISelectionListener;

import org.vcml.session.Command;
import org.vcml.session.Module;
import org.vcml.session.Session;
import org.vcml.session.SessionException;
import org.vcml.explorer.ui.Utils;
import org.vcml.explorer.ui.dialogs.CommandDialog;
//import org.vcml.vseui.CommandResponseDialog;
import org.vcml.explorer.ui.services.ISessionService;

public class HierarchyPart {

    private final Image IMAGE_GENERIC = Utils.getImage("chip.png");
    private final Image IMAGE_SC_OBJECT = Utils.getImage("chip.png");
    private final Image IMAGE_SC_MODULE = Utils.getImage("chip.png");
    private final Image IMAGE_SC_SIGNAL = Utils.getImage("signal.png");
    private final Image IMAGE_SC_PORT = Utils.getImage("out.png");
    private final Image IMAGE_SC_EXPORT = Utils.getImage("in.png");
    private final Image IMAGE_SC_OUT = Utils.getImage("out.png");
    private final Image IMAGE_SC_IN = Utils.getImage("in.png");
    private final Image IMAGE_SC_METHOD_PROCESS = Utils.getImage("method.gif");
    private final Image IMAGE_SC_THREAD_PROCESS = Utils.getImage("thread.gif");
    private final Image IMAGE_VCL_IN_PORT = Utils.getImage("in.png");
    private final Image IMAGE_VCL_OUT_PORT = Utils.getImage("out.png");
    private final Image IMAGE_VCL_MASTER_SOCKET = Utils.getImage("initiator.png");
    private final Image IMAGE_VCL_SLAVE_SOCKET = Utils.getImage("target.png");
    private final Image IMAGE_VCML_COMPONENT = Utils.getImage("chip.png");
    private final Image IMAGE_VCL_PROCESSOR = Utils.getImage("chip.png");
    private final Image IMAGE_VCL_BUS = Utils.getImage("bus.gif");
    private final Image IMAGE_VCL_ROM = Utils.getImage("chip.png");
    private final Image IMAGE_VCL_RAM = Utils.getImage("chip.png");
    private final Image IMAGE_VCL_PERIPHERAL = Utils.getImage("chip.png");
    private final Image IMAGE_VCL_UART = Utils.getImage("chip.png");
    private final Image IMAGE_VCL_ETHERNET = Utils.getImage("chip.png");
    private final Image IMAGE_VCL_XBAR = Utils.getImage("chip.png");

    @Inject
    private ISessionService sessionService;

    @Inject
    private ESelectionService selectionService;

    private TreeViewer viewer;

    private Module selectedModule;

    private ITreeContentProvider contentProvider = new ITreeContentProvider() {
        @Override
        public boolean hasChildren(Object element) {
            if (!(element instanceof Module))
                return false;
            return ((Module) element).getChildren().length > 0;
        }

        @Override
        public Object getParent(Object element) {
            if (!(element instanceof Module))
                return false;
            return ((Module) element).getParent();
        }

        @Override
        public Object[] getElements(Object inputElement) {
            if (!(inputElement instanceof Session))
                return new Object[0];

            Session session = (Session) inputElement;
            if (!session.isConnected())
                return new Object[0];

            try {
                Object[] tops = session.getTopLevelObjects();
                if (tops == null)
                    return new Object[0];
                return tops;
            } catch (SessionException e) {
                sessionService.reportSessionError(session, e);
                return new Object[0];
            }
        }

        @Override
        public Object[] getChildren(Object parentElement) {
            if (!(parentElement instanceof Module))
                return new Object[0];
            return ((Module) parentElement).getChildren();
        }
    };

    private CellLabelProvider labelProvider = new CellLabelProvider() {
        public String getText(Object element) {
            Module module = (Module) element;
            return module.getBaseName();
        }

        public Image getImage(Object element) {
            Module module = (Module) element;
            switch (module.getKind()) {
            case Module.KIND_SC_OBJECT:
                return IMAGE_SC_OBJECT;
            case Module.KIND_SC_MODULE:
                return IMAGE_SC_MODULE;
            case Module.KIND_SC_SIGNAL:
                return IMAGE_SC_SIGNAL;
            case Module.KIND_SC_PORT:
                return IMAGE_SC_PORT;
            case Module.KIND_SC_EXPORT:
                return IMAGE_SC_EXPORT;
            case Module.KIND_SC_OUT:
                return IMAGE_SC_OUT;
            case Module.KIND_SC_IN:
                return IMAGE_SC_IN;
            case Module.KIND_SC_METHOD_PROCESS:
                return IMAGE_SC_METHOD_PROCESS;
            case Module.KIND_SC_THREAD_PROCESS:
                return IMAGE_SC_THREAD_PROCESS;
            case Module.KIND_VCML_IN_PORT:
                return IMAGE_VCL_IN_PORT;
            case Module.KIND_VCML_OUT_PORT:
                return IMAGE_VCL_OUT_PORT;
            case Module.KIND_VCML_MASTER_SOCKET:
                return IMAGE_VCL_MASTER_SOCKET;
            case Module.KIND_VCML_SLAVE_SOCKET:
                return IMAGE_VCL_SLAVE_SOCKET;
            case Module.KIND_VCML_COMPONENT:
                return IMAGE_VCML_COMPONENT;
            case Module.KIND_VCML_PROCESSOR:
                return IMAGE_VCL_PROCESSOR;
            case Module.KIND_VCML_BUS:
                return IMAGE_VCL_BUS;
            case Module.KIND_VCML_ROM:
                return IMAGE_VCL_ROM;
            case Module.KIND_VCML_RAM:
                return IMAGE_VCL_RAM;
            case Module.KIND_VCML_XBAR:
                return IMAGE_VCL_XBAR;
            case Module.KIND_VCML_PERIPHERAL:
                return IMAGE_VCL_PERIPHERAL;
            case Module.KIND_VCML_UART:
                return IMAGE_VCL_UART;
            case Module.KIND_VCML_ETHERNET:
                return IMAGE_VCL_ETHERNET;
            default:
                return IMAGE_GENERIC;
            }
        }

        @Override
        public String getToolTipText(Object element) {
            Module module = (Module) element;
            return "Name: " + module.getName() + "\nKind: " + module.getKind();
        }

        @Override
        public Font getToolTipFont(Object element) {
            return Utils.getMonoSpaceFont();
        }

        @Override
        public void update(ViewerCell cell) {
            cell.setText(getText(cell.getElement()));
            cell.setImage(getImage(cell.getElement()));

        }
    };

    private ViewerComparator viewerComparator = new ViewerComparator() {
        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            Module m1 = (Module) e1;
            Module m2 = (Module) e2;
            return m1.getKind().compareTo(m2.getKind());
        }

    };

    private IPropertyChangeListener sessionListener = new IPropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent event) {
            // String property = event.getProperty();
            Session session = (Session) event.getNewValue();
            refresh(session);
        }
    };

    private ISelectionListener selectionListener = new ISelectionListener() {
        @Override
        public void selectionChanged(MPart part, Object selection) {
            if (selection instanceof Module)
                selectedModule = (Module) selection;
        }
    };

    private ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            IStructuredSelection selection = viewer.getStructuredSelection();
            selectionService.setSelection(selection.getFirstElement());
        }
    };

    private void executeCommand(Command command) {
        // BusyIndicator.showWhile(null, new Runnable() {
        // @Override
        // public void run() {
        Shell parent = Display.getDefault().getActiveShell();
        CommandDialog dialog = new CommandDialog(parent, selectedModule, command);
        dialog.open();
        // }
        // });
    }

    private Menu buildCommandMenu(MenuItem parent, Module module) {
        Menu menu = new Menu(parent);

        Command[] commands = module.getCommands();
        for (Command command : commands) {
            if (command.getName().equals("clist") || command.getName().equals("cinfo"))
                continue;

            MenuItem item = new MenuItem(menu, SWT.NONE);
            item.setText(command.getName());
            item.setData(command);
            item.setToolTipText(command.getDesc());
            item.addSelectionListener(new SelectionListener() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    MenuItem source = (MenuItem) e.getSource();
                    Command command = (Command) source.getData();
                    executeCommand(command);
                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                    // TODO Auto-generated method stub

                }
            });
        }

        return menu;
    }

    private Menu buildContextMenu(Tree tree) {
        Menu menu = new Menu(tree);
        menu.addMenuListener(new MenuListener() {

            @Override
            public void menuShown(MenuEvent e) {
                MenuItem[] items = menu.getItems();
                for (int i = 0; i < items.length; i++)
                    items[i].dispose();

                // ToDo rework hierarchy context menu
                if (selectedModule == null)
                    return;

                TreeItem selectedItem = null;
                if (tree.getSelection().length > 0)
                    selectedItem = tree.getSelection()[0];

                boolean active = selectedItem != null && selectedModule != null;

                MenuItem nameItem = new MenuItem(menu, SWT.NONE);
                nameItem.setEnabled(active);
                nameItem.setText(active ? selectedModule.getName() : "None Selected");

                new MenuItem(menu, SWT.SEPARATOR);

                MenuItem cmdsItem = new MenuItem(menu, SWT.CASCADE);
                cmdsItem.setEnabled(false);
                cmdsItem.setText("Execute...");
                if (active && selectedModule.getCommands().length > 2) {
                    cmdsItem.setEnabled(true);
                    cmdsItem.setMenu(buildCommandMenu(cmdsItem, selectedModule));
                }

                new MenuItem(menu, SWT.SEPARATOR);

                MenuItem showChildren = new MenuItem(menu, SWT.NONE);
                showChildren.setEnabled(active && !selectedItem.getExpanded());
                showChildren.setText("Show Children");
                showChildren.setData(selectedItem);
                showChildren.setEnabled(selectedModule.getChildren().length > 0);
                showChildren.addSelectionListener(new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        MenuItem source = (MenuItem) e.getSource();
                        TreeItem item = (TreeItem) source.getData();
                        item.setExpanded(true);
                        viewer.refresh();
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                        // TODO Auto-generated method stub
                    }
                });

                MenuItem hideChildren = new MenuItem(menu, SWT.NONE);
                hideChildren.setEnabled(active && selectedItem.getExpanded());
                hideChildren.setText("Hide Children");
                hideChildren.setData(selectedItem);
                hideChildren.addSelectionListener(new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        MenuItem source = (MenuItem) e.getSource();
                        TreeItem item = (TreeItem) source.getData();
                        item.setExpanded(false);
                        tree.update();
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                        // Nothing to do
                    }
                });
            }

            @Override
            public void menuHidden(MenuEvent e) {
                // Nothing to do
            }
        });

        return menu;
    }

    @PostConstruct
    public void createComposite(Composite parent) {
        sessionService.addSessionChangeListener(sessionListener);
        selectionService.addSelectionListener(selectionListener);

        viewer = new TreeViewer(parent, SWT.BORDER | SWT.MULTI);
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(labelProvider);
        viewer.setComparator(viewerComparator);
        viewer.addSelectionChangedListener(selectionChangedListener);

        GridLayout layout = new GridLayout();
        layout.makeColumnsEqualWidth = true;
        parent.setLayout(layout);

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        data.verticalAlignment = SWT.FILL;

        Tree tree = viewer.getTree();
        tree.setLayoutData(data);
        tree.setMenu(buildContextMenu(tree));
        ColumnViewerToolTipSupport.enableFor(viewer);
    }

    @Focus
    public void setFocus() {
        viewer.getTree().setFocus();
    }

    private void refresh(Session session) {
        // if (session != null)
        viewer.setInput(session);
    }

    public void dispose() {
        sessionService.removeSessionChangeListener(sessionListener);
        selectionService.removeSelectionListener(selectionListener);
    }

}
