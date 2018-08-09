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

import java.util.Iterator;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;

import org.vcml.explorer.ui.Utils;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.session.Session;

public class SessionPart {

	private final Image CONNECTED    = Utils.getImage("session_alt.gif");
	private final Image DISCONNECTED = Utils.getImage("session.gif");
	private final Image IMAGE_RUN    = Utils.getImage("run.gif");
	private final Image IMAGE_STOP   = Utils.getImage("stop.gif");
	private final Image IMAGE_STEP   = Utils.getImage("step.gif");
	private final Image IMAGE_KILL   = Utils.getImage("terminate.gif");
	
	@Inject
	private ISessionService sessionService;
	
	@Inject
	IEventBroker eventBroker;
	
	private TableViewer viewer;
	
	private IPropertyChangeListener sessionListener = new IPropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
			String property = event.getProperty();
			if (property.equals(ISessionService.PROP_UPDATED))
				viewer.update(event.getNewValue(), null);
			else
				refresh();
		}
	};
	
	private IStructuredContentProvider contentProvider = new IStructuredContentProvider() {
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof ISessionService)
				return ((ISessionService)inputElement).getSessions().toArray();
			return null;
		}
	};
	
	private ISelectionChangedListener selectionListener = new ISelectionChangedListener() {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection selection = event.getSelection();
			if ((selection == null) || (selection.isEmpty()) || !(selection instanceof IStructuredSelection)) {
				sessionService.selectSession(null);
				return;
			}
			
			IStructuredSelection structured = (IStructuredSelection)selection;
			Iterator<?> it = structured.iterator();
			while (it.hasNext()) {
				Object selected = it.next();
				if (selected instanceof Session)
					sessionService.selectSession((Session)selected);
			}
		}
	};
	
	private IDoubleClickListener doubleClickListener = new IDoubleClickListener() {
		@Override
		public void doubleClick(DoubleClickEvent event) {
			ISelection selection = event.getSelection();
			if ((selection != null) && (!selection.isEmpty()) &&
			    (selection instanceof IStructuredSelection)) {
				IStructuredSelection structured = (IStructuredSelection)selection;
				Iterator<?> it = structured.iterator();
				while (it.hasNext()) {
					Object selected = it.next();
					if (selected instanceof Session) {
						Session session = (Session)selected;
						if (session.isConnected())
							sessionService.disconnectSession(session);
						else
							sessionService.connectSession(session);
					}
				}
			}
		}
	};

	private ColumnLabelProvider labelProvider = new ColumnLabelProvider() {
    	@Override
    	public String getText(Object element) {
    		Session session = (Session)element;
    		String desc = session.toString();
    		if (session.isRunning())
    			desc += " [running]";
    		else if (session.isConnected())
    			desc += " [connected]";
    		else
    			desc += " [not connected]";
    	    return  desc;
    	}

		@Override
		public Image getImage(Object element) {
			Session session = (Session)element;
			return session.isConnected() ? CONNECTED : DISCONNECTED;
		}
		
		@Override
		public String getToolTipText(Object element) {
			Session session = (Session)element;
			return session.getExecutable();
		}
	};
	
	private void constructMenu(Menu parent) {
		MenuItem[] items = parent.getItems();
		for (int i = 0; i < items.length; i++)
			items[i].dispose();
		
		Session current = sessionService.currentSession();
		
		MenuItem connectItem = new MenuItem(parent, SWT.NONE);
		connectItem.setText("Connect");
		connectItem.setEnabled(current != null && !current.isConnected());
		connectItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sessionService.connectSession(current);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to do
			}
		});
		
		MenuItem disconnectItem = new MenuItem(parent, SWT.NONE);
		disconnectItem.setText("Disconnect");
		disconnectItem.setEnabled(current != null && current.isConnected());
		disconnectItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sessionService.disconnectSession(current);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to do
			}
		});

		new MenuItem(parent, SWT.SEPARATOR);

		MenuItem runItem = new MenuItem(parent, SWT.NONE);
		runItem.setText("Run Simulation");
		runItem.setImage(IMAGE_RUN);
		runItem.setEnabled(current != null && !current.isRunning());
		runItem.setData(current);
		runItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MenuItem source = (MenuItem)e.getSource();
				Session session = (Session)source.getData();
				sessionService.startSimulation(session);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to do
			}
		});

		MenuItem stopItem = new MenuItem(parent, SWT.NONE);
		stopItem.setText("Stop Simulation");
		stopItem.setImage(IMAGE_STOP);
		stopItem.setEnabled(current != null && current.isRunning());
		stopItem.setData(current);
		stopItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MenuItem source = (MenuItem)e.getSource();
				Session session = (Session)source.getData();
				sessionService.stopSimulation(session);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to do
			}
		});

		MenuItem stepItem = new MenuItem(parent, SWT.NONE);
		stepItem.setText("Step Simulation");
		stepItem.setImage(IMAGE_STEP);
		stepItem.setEnabled(current != null && !current.isRunning());
		stepItem.setData(current);
		stepItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MenuItem source = (MenuItem)e.getSource();
				Session session = (Session)source.getData();
				sessionService.stepSimulation(session);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to do
			}
		});

		MenuItem killItem = new MenuItem(parent, SWT.CASCADE);
		killItem.setText("Kill Simulation");
		killItem.setImage(IMAGE_KILL);
		killItem.setEnabled(current != null);
		killItem.setData(current);
		killItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MenuItem source = (MenuItem)e.getSource();
				Session session = (Session)source.getData();
				sessionService.disconnectSession(session);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to do
			}
		});
		
		new MenuItem(parent, SWT.SEPARATOR);
		
		MenuItem refreshItem = new MenuItem(parent, SWT.NONE);
		refreshItem.setText("Refresh Servers");
		refreshItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sessionService.refreshSessions();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to do
			}
		});
		
		MenuItem addItem = new MenuItem(parent, SWT.NONE);
		addItem.setText("Add Server");
		addItem.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				sessionService.addRemoteSession();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to do
			}
		});
	}

	@PostConstruct
	public void createComposite(Composite parent) {
		viewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		viewer.setLabelProvider(new LabelProvider());
		viewer.setContentProvider(contentProvider);
		viewer.addSelectionChangedListener(selectionListener);
		viewer.addDoubleClickListener(doubleClickListener);

		TableViewerColumn nameColumn = new TableViewerColumn(viewer, SWT.LEFT);
        nameColumn.getColumn().setText("Name");
        nameColumn.getColumn().setWidth(200);
        nameColumn.setLabelProvider(labelProvider);

        GridLayout layout = new GridLayout();
        layout.makeColumnsEqualWidth = true;
        parent.setLayout(layout);
        
        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        data.horizontalAlignment = SWT.FILL;
        data.verticalAlignment = SWT.FILL;
        
        Table table = viewer.getTable();
        table.setLayoutData(data);
        table.setHeaderVisible(false);
        
        Menu menu = new Menu(parent);
        menu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				constructMenu(menu);
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
				// TODO Auto-generated method stub
			}
		});
        
        table.setMenu(menu);
        ColumnViewerToolTipSupport.enableFor(viewer);
        sessionService.addSessionChangeListener(sessionListener);
        refresh();
	}

	@Focus
	public void setFocus() {
		refresh();
		viewer.getTable().setFocus();
	}

	public void refresh() {
		viewer.setInput(sessionService);
		viewer.refresh();
	}

}
