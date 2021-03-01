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

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.fieldassist.AutoCompleteField;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.vcml.explorer.ui.Instruction;
import org.vcml.explorer.ui.Resources;
import org.vcml.explorer.ui.Symbol;
import org.vcml.explorer.ui.services.ISessionService;
import org.vcml.session.Module;
import org.vcml.session.Session;

public class ProcessorPart {

    public static final String PROGRAM_COUNTER = "Program Counter";

    public static final String ERROR_CELL = "--"; //$NON-NLS-1$

    public static final int SCROLL_COUNT = 100;

    public static final long SCROLL_BYTES = SCROLL_COUNT * 4l;

    public static long getProgramCounter(Module processor) {
        try {
            String result = processor.execute("dump");
            Pattern pattern = Pattern.compile("PC 0x([0-9a-f]{16})");
            Matcher matcher = pattern.matcher(result);

            if (matcher.find())
                return Long.parseUnsignedLong(matcher.group(1), 16);

            System.err.println("Error fetching program counter");
            return 0;
        } catch (Exception e) {
            System.err.println("Error fetching program counter: " + e.getMessage());
            return 0;
        }
    }

    private ISessionService service;
    private Session session;
    private Module processor;
    private String name;

    private long topAddress;
    private long programCounter;
    private HashMap<Long, Instruction> instructions;
    private Symbol[] symbols;
    private Symbol symbolPC;
    private String[] symbolNames;

    private ComboViewer symbolViewer;
    private AutoCompleteField symbolAutoComplete;

    private TableViewer instructionViewer;
    private TableViewerColumn iconColumn;
    private TableViewerColumn physColumn;
    private TableViewerColumn virtColumn;
    private TableViewerColumn insnColumn;
    private TableViewerColumn dissColumn;
    private TableViewerColumn symbColumn;

    private long addrOf(int idx) {
        long addr = topAddress;
        for (int i = 0; i < idx; i++)
            addr += fetchInstruction(addr).getSize();
        return addr;
    }

    private int indexOf(long addr) {
        int idx = 0;
        if (addr - topAddress < 0) // if addr < topAddr (workaround for no unsigned long in Java)
            return -1;

        long curr = topAddress;
        while (curr - addr < 0) { // while (curr < addr) ...
            curr += fetchInstruction(curr).getSize();
            idx++;
        }

        return idx;
    }

    private Instruction fetchInstruction(long addr) {
        Instruction insn = instructions.get(addr);
        if (insn != null)
            return insn;

        insn = new Instruction(addr, processor);
        if (insn.getVirtualAddress() != addr) {
            System.err.println(String.format("Disassembling address 0x%016x returned instruction at address 0x%016x",
                                             addr, insn.getVirtualAddress()));
        }

        instructions.put(insn.getVirtualAddress(), insn);
        return insn;
    }

    private void scrollDown() {
        int count = instructionViewer.getTable().getItemCount();
        instructionViewer.setItemCount(count + SCROLL_COUNT);
    }

    private void scrollUp() {
        if (topAddress == 0)
            return;

        Table table = instructionViewer.getTable();
        int selected = table.getSelectionIndex();
        long selectedAddr = addrOf(selected);

        long oldTop = topAddress;
        topAddress -= SCROLL_BYTES;
        if ((oldTop > 0 && topAddress < 0) || (oldTop < 0 && topAddress > 0))
            topAddress = 0;

        int added = indexOf(oldTop) + 1;
        int count = instructionViewer.getTable().getItemCount();
        instructionViewer.setItemCount(count + added);
        instructionViewer.refresh();

        selected = indexOf(selectedAddr);
        table.setSelection(selected);
        table.setTopIndex(SCROLL_COUNT);
    }

    private Listener scrollListener = new Listener() {
        private int lastIndex = -1;

        @Override
        public void handleEvent(Event e) {
            Table table = instructionViewer.getTable();
            int index = table.getTopIndex();
            int count = table.getItemCount();

            int viewerHeight = table.getSize().y;
            int elementHeight = table.getItemHeight();
            int visibleElements = viewerHeight / elementHeight + 1;

            if (index != lastIndex) {
                lastIndex = index;
                if (index > (count - visibleElements))
                    scrollDown();
                if (index == 0)
                    scrollUp();
            }
        }
    };

    private Symbol lookupSymbol(String name) {
        if (name.equals(PROGRAM_COUNTER))
            return symbolPC;
        for (Symbol sym : symbols)
            if (sym.getName().equals(name))
                return sym;
        return null;
    }

    private void showRange(long address, long size) {
        topAddress = address & ~0xffl;
        instructions = new HashMap<Long, Instruction>();

        instructionViewer.setInput(processor);
        instructionViewer.setItemCount(SCROLL_COUNT);

        int selected = 0;
        long addr = topAddress;
        while (addr < address) {
            addr += fetchInstruction(addr).getSize();
            selected++;
        }

        Table table = instructionViewer.getTable();
        table.setSelection(selected);
        table.setTopIndex(selected);
    }

    private void showRange(long address) {
        long size = SCROLL_COUNT * 4;
        showRange(address, size);
    }

    private void createSymbolComboViewer(Composite parent) {
        symbolViewer = new ComboViewer(parent, SWT.NONE);
        symbolViewer.getCombo().setText("enter target address...");
        symbolViewer.getCombo().add(PROGRAM_COUNTER);
        symbolViewer.getCombo().addTraverseListener(new TraverseListener() {
            @Override
            public void keyTraversed(TraverseEvent event) {
                if (event.detail == SWT.TRAVERSE_RETURN) {
                    Combo combo = symbolViewer.getCombo();
                    String target = combo.getText();
                    Symbol sym = lookupSymbol(target);

                    try {
                        if (sym == null)
                            Long.parseUnsignedLong(target, 16);
                        if (combo.indexOf(target) == -1)
                            combo.add(target);
                    } catch (NumberFormatException e) {
                        // ignore
                    }
                }
            }
        });

        symbolViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                String target = symbolViewer.getCombo().getText();
                if (target.startsWith("0x")) {
                    try {
                        showRange(Long.parseUnsignedLong(target.substring(2), 16));
                    } catch (NumberFormatException ex) {
                        // nothing to do
                    }
                } else {
                    Symbol sym = lookupSymbol(target);
                    if (sym != null)
                        showRange(sym.getAddress());
                }
            }
        });

        symbolViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        symbolAutoComplete = new AutoCompleteField(symbolViewer.getControl(), new ComboContentAdapter(), null);
    }

    public void createInstructionTableViewer(Composite parent) {
        instructionViewer = new TableViewer(parent, SWT.BORDER | SWT.VIRTUAL);
        instructionViewer.setUseHashlookup(true);
        instructionViewer.setContentProvider(new ILazyContentProvider() {
            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                // Nothing to do
            }

            @Override
            public void dispose() {
                // Nothing to do
            }

            @Override
            public void updateElement(int index) {
                if (instructionViewer.isBusy())
                    return;

                long address = addrOf(index);
                Instruction insn = fetchInstruction(address);
                instructionViewer.replace(insn, index);
            }
        });

        iconColumn = new TableViewerColumn(instructionViewer, SWT.CENTER);
        iconColumn.getColumn().setText("");
        iconColumn.getColumn().setWidth(50);
        iconColumn.setLabelProvider(new ColumnLabelProvider() {
            private final Image PROGRAM_COUNTER = Resources.getImage("icons/run.gif");

            @Override
            public String getText(Object element) {
                return "";
            }

            @Override
            public Image getImage(Object element) {
                if (((Instruction) element).getAddress() == programCounter)
                    return PROGRAM_COUNTER;
                return null;
            }

            @Override
            public Color getBackground(Object element) {
                if (((Instruction) element).getAddress() == programCounter)
                    return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
                return null;
            }
        });

        virtColumn = new TableViewerColumn(instructionViewer, SWT.CENTER);
        virtColumn.getColumn().setText("Virtual");
        virtColumn.getColumn().setWidth(128);
        virtColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                long addr = ((Instruction) element).getVirtualAddress();
                String fmt = addr > 0xffffffffl ? "%016x" : "%08x";
                return String.format(fmt, addr);
            }

            @Override
            public Font getFont(Object element) {
                return Resources.getMonoSpaceFont();
            }

            @Override
            public Color getForeground(Object element) {
                return Display.getDefault().getSystemColor(SWT.COLOR_DARK_YELLOW);
            }

            @Override
            public Color getBackground(Object element) {
                if (((Instruction) element).getAddress() == programCounter)
                    return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
                return null;
            }
        });

        physColumn = new TableViewerColumn(instructionViewer, SWT.CENTER);
        physColumn.getColumn().setText("Address");
        physColumn.getColumn().setWidth(128);
        physColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                long addr = ((Instruction) element).getPhysicalAddress();
                String fmt = addr > 0xffffffffl ? "%016x" : "%08x";
                return String.format(fmt, addr);
            }

            @Override
            public Font getFont(Object element) {
                return Resources.getMonoSpaceFont();
            }

            @Override
            public Color getForeground(Object element) {
                return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GREEN);
            }

            @Override
            public Color getBackground(Object element) {
                if (((Instruction) element).getAddress() == programCounter)
                    return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
                return null;
            }
        });

        insnColumn = new TableViewerColumn(instructionViewer, SWT.CENTER);
        insnColumn.getColumn().setText("Instruction");
        insnColumn.getColumn().setWidth(140);
        insnColumn.getColumn().setAlignment(SWT.LEFT);
        insnColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                Instruction insn = (Instruction)element;
                return insn.getInstruction();
            }

            @Override
            public Font getFont(Object element) {
                return Resources.getMonoSpaceFont();
            }

            @Override
            public Color getForeground(Object element) {
                return Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED);
            }

            @Override
            public Color getBackground(Object element) {
                if (((Instruction) element).getAddress() == programCounter)
                    return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
                return null;
            }
        });

        dissColumn = new TableViewerColumn(instructionViewer, SWT.LEFT);
        dissColumn.getColumn().setText("Disassembly");
        dissColumn.getColumn().setWidth(250);
        dissColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((Instruction) element).getDisassembly();
            }

            @Override
            public Font getFont(Object element) {
                return Resources.getMonoSpaceFont();
            }

            @Override
            public Color getForeground(Object element) {
                return Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
            }

            @Override
            public Color getBackground(Object element) {
                if (((Instruction) element).getAddress() == programCounter)
                    return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
                return null;
            }
        });

        symbColumn = new TableViewerColumn(instructionViewer, SWT.LEFT);
        symbColumn.getColumn().setText("Symbols");
        symbColumn.getColumn().setWidth(250);
        symbColumn.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                return ((Instruction) element).getSymbol();
            }

            @Override
            public Font getFont(Object element) {
                return Resources.getMonoSpaceFont();
            }

            @Override
            public Color getForeground(Object element) {
                return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
            }

            @Override
            public Color getBackground(Object element) {
                if (((Instruction) element).getAddress() == programCounter)
                    return Display.getDefault().getSystemColor(SWT.COLOR_YELLOW);
                return null;
            }
        });

        Table table = instructionViewer.getTable();
        table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        table.setFont(Resources.getMonoSpaceFont());
        table.setHeaderVisible(true);
        table.setLinesVisible(false);
        table.addListener(SWT.MouseDown, scrollListener);
        table.addListener(SWT.MouseUp, scrollListener);
        table.addListener(SWT.KeyDown, scrollListener);
        table.addListener(SWT.KeyUp, scrollListener);
        table.getVerticalBar().addListener(SWT.Selection, scrollListener);
    }

    @Inject
    public ProcessorPart(ISessionService sessionService, ESelectionService selectionService) {
        service = sessionService;
        session = sessionService.getSession();
        processor = (Module) selectionService.getSelection();
        name = processor.getName();
    }

    @PostConstruct
    public void createComposite(Composite parent) {
        parent.setLayout(new GridLayout());

        Composite composite = new Composite(parent, SWT.BORDER);
        composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        composite.setLayout(new GridLayout());

        createSymbolComboViewer(composite);
        createInstructionTableViewer(composite);

        refresh();
    }

    @Focus
    public void setFocus() {
        instructionViewer.getTable().setFocus();
    }

    public void refresh() {
        Table table = instructionViewer.getTable();
        if (!session.isConnected() || session.isRunning()) {
            table.setEnabled(false);
            return;
        }

        table.setEnabled(true);
        processor = service.findModule(session, name);
        programCounter = getProgramCounter(processor);
        showRange(programCounter);

        symbols = Symbol.findFunctions(processor);
        symbolPC = new Symbol(PROGRAM_COUNTER, programCounter, true);
        symbolNames = new String[symbols.length];
        for (int i = 0; i < symbols.length; i++)
            symbolNames[i] = symbols[i].getName();
        symbolAutoComplete.setProposals(symbolNames);
    }

    @Inject
    @Optional
    public void sessionChanged(@UIEventTopic(ISessionService.TOPIC_SESSION_ANY) Session current) {
        if (session == current) {
            refresh();
        }
    }
}
