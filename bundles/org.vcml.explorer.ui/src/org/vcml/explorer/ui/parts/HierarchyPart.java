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

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;

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
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;

import org.vcml.session.Command;
import org.vcml.session.Module;
import org.vcml.session.Session;
import org.vcml.session.SessionException;
import org.vcml.explorer.ui.Resources;
import org.vcml.explorer.ui.dialogs.CommandDialog;
import org.vcml.explorer.ui.services.IInspectionService;
import org.vcml.explorer.ui.services.ISessionService;

@SuppressWarnings("restriction")
public class HierarchyPart {

    @Inject
    private ISessionService sessionService;

    @Inject
    private IInspectionService inspectionService;

    @Inject
    private ESelectionService selectionService;

    @Inject
    private ECommandService commandService;

    @Inject
    private EHandlerService handlerService;

    private TreeViewer viewer;

    private Module selectedModule;

    private ITreeContentProvider contentProvider = new ITreeContentProvider() {
        @Override
        public boolean hasChildren(Object element) {
            return ((Module) element).getChildren().length > 0;
        }

        @Override
        public Object getParent(Object element) {
            return ((Module) element).getParent();
        }

        @Override
        public Object[] getElements(Object inputElement) {
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
            return ((Module) parentElement).getChildren();
        }
    };

    private CellLabelProvider labelProvider = new CellLabelProvider() {
        public String getText(Object element) {
            Module module = (Module) element;
            return module.getBaseName();
        }

        public Image getImage(Object element) {
            return Resources.getImageFor(element);
        }

        @Override
        public String getToolTipText(Object element) {
            Module module = (Module) element;
            return "Name: " + module.getName() + "\nKind: " + module.getKind();
        }

        @Override
        public Font getToolTipFont(Object element) {
            return Resources.getMonoSpaceFont();
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

    @Inject
    @Optional
    public void sessionChanged(@UIEventTopic(ISessionService.TOPIC_SESSION_ANY) Session session) {
        if (viewer != null && !viewer.getControl().isDisposed())
            viewer.setInput(session);
    }

    @Inject
    public void selectionChanged(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object selection) {
        if (selection instanceof Module)
            selectedModule = (Module) selection;
    }

    private ISelectionChangedListener viewerSelectionListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            IStructuredSelection selection = viewer.getStructuredSelection();
            selectionService.setSelection(selection.getFirstElement());
        }
    };

    private IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
        @Override
        public void doubleClick(DoubleClickEvent event) {
            ParameterizedCommand inspect = commandService.createCommand("org.vcml.explorer.ui.command.inspect", null);
            handlerService.executeHandler(inspect);
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
            item.setImage(Resources.getImage("icons/consoles.png"));
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
                nameItem.setEnabled(false);
                nameItem.setText(active ? selectedModule.getName() : "None Selected");
                nameItem.setImage(Resources.getImageFor(selectedModule));

                new MenuItem(menu, SWT.SEPARATOR);

                MenuItem cmdsItem = new MenuItem(menu, SWT.CASCADE);
                cmdsItem.setEnabled(false);
                cmdsItem.setText("Execute...");
                cmdsItem.setImage(Resources.getImage("icons/consoles.png"));
                if (active && selectedModule.getCommands().length > 2) {
                    cmdsItem.setEnabled(true);
                    cmdsItem.setMenu(buildCommandMenu(cmdsItem, selectedModule));
                }

                new MenuItem(menu, SWT.SEPARATOR);

                MenuItem showChildren = new MenuItem(menu, SWT.NONE);
                showChildren.setEnabled(active && !selectedItem.getExpanded());
                showChildren.setText("Show Children");
                showChildren.setData(selectedItem);
                showChildren.setImage(Resources.getImage("icons/expand.gif"));
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
                hideChildren.setImage(Resources.getImage("icons/collapse.gif"));
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

                new MenuItem(menu, SWT.SEPARATOR);

                MenuItem inspectItem = new MenuItem(menu, SWT.NONE);
                inspectItem.setEnabled(inspectionService.isInspectable(selectedModule));
                inspectItem.setText("Inspect");
                inspectItem.setData(selectedItem);
                inspectItem.setImage(Resources.getImage("icons/inspect.gif"));
                inspectItem.addSelectionListener(new SelectionListener() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        ParameterizedCommand inspect = commandService
                                .createCommand("org.vcml.explorer.ui.command.inspect", null);
                        handlerService.executeHandler(inspect);
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
        viewer = new TreeViewer(parent, SWT.BORDER | SWT.MULTI);
        viewer.setContentProvider(contentProvider);
        viewer.setLabelProvider(labelProvider);
        viewer.setComparator(viewerComparator);
        viewer.addSelectionChangedListener(viewerSelectionListener);
        viewer.addDoubleClickListener(doubleClickListener);

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

    public void collapseAll() {
        viewer.collapseAll();
    }

    public void expandAll() {
        viewer.expandAll();
    }

}
