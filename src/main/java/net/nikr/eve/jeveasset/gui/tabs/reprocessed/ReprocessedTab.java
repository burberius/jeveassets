/*
 * Copyright 2009-2022 Contributors (see credits.txt)
 *
 * This file is part of jEveAssets.
 *
 * jEveAssets is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * jEveAssets is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jEveAssets; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package net.nikr.eve.jeveasset.gui.tabs.reprocessed;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ListSelection;
import ca.odell.glazedlists.SeparatorList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.TableComparatorChooser;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import net.nikr.eve.jeveasset.Program;
import net.nikr.eve.jeveasset.data.sde.Item;
import net.nikr.eve.jeveasset.data.sde.StaticData;
import net.nikr.eve.jeveasset.data.settings.types.LocationType;
import net.nikr.eve.jeveasset.gui.images.Images;
import net.nikr.eve.jeveasset.gui.shared.components.JFixedToolBar;
import net.nikr.eve.jeveasset.gui.shared.components.JMainTabSecondary;
import net.nikr.eve.jeveasset.gui.shared.filter.FilterControl;
import net.nikr.eve.jeveasset.gui.shared.menu.JMenuColumns;
import net.nikr.eve.jeveasset.gui.shared.menu.MenuData;
import net.nikr.eve.jeveasset.gui.shared.menu.MenuManager.TableMenu;
import net.nikr.eve.jeveasset.gui.shared.table.EnumTableFormatAdaptor;
import net.nikr.eve.jeveasset.gui.shared.table.EventListManager;
import net.nikr.eve.jeveasset.gui.shared.table.EventModels;
import net.nikr.eve.jeveasset.gui.shared.table.JSeparatorTable;
import net.nikr.eve.jeveasset.gui.shared.table.PaddingTableCellRenderer;
import net.nikr.eve.jeveasset.gui.shared.table.TableFormatFactory;
import net.nikr.eve.jeveasset.gui.tabs.reprocessed.ReprocessedSeparatorTableCell.ReprocessedCellAction;
import net.nikr.eve.jeveasset.i18n.TabsReprocessed;


public class ReprocessedTab extends JMainTabSecondary {

	private enum ReprocessedAction {
		COLLAPSE,
		EXPAND,
		CLEAR,
		ADD_ITEM
	}

	//GUI
	private final JSeparatorTable jTable;

	//Dialogs
	private final JReprocessedAddItemDialog jAddItemDialog;

	//Table
	private final ReprocessedFilterControl filterControl;
	private final EventList<ReprocessedInterface> eventList;
	private final FilterList<ReprocessedInterface> filterList;
	private final SeparatorList<ReprocessedInterface> separatorList;
	private final DefaultEventSelectionModel<ReprocessedInterface> selectionModel;
	private final DefaultEventTableModel<ReprocessedInterface> tableModel;
	private final EnumTableFormatAdaptor<ReprocessedTableFormat, ReprocessedInterface> tableFormat;

	//Listener
	private final ListenerClass listener = new ListenerClass();

	//Data
	private final Set<Integer> typeIDs = new HashSet<>();
	private final ReprocessedData reprocessedData;

	public static final String NAME = "reprocessed"; //Not to be changed!

	public ReprocessedTab(final Program program) {
		super(program, NAME, TabsReprocessed.get().title(), Images.TOOL_REPROCESSED.getIcon(), true);

		reprocessedData = new ReprocessedData(program);

		JFixedToolBar jToolBarLeft = new JFixedToolBar();

		JButton jAddItem = new JButton(TabsReprocessed.get().addItem(), Images.EDIT_ADD.getIcon());
		jAddItem.setActionCommand(ReprocessedAction.ADD_ITEM.name());
		jAddItem.addActionListener(listener);
		jToolBarLeft.addButton(jAddItem);

		JButton jClear = new JButton(TabsReprocessed.get().removeAll(), Images.EDIT_DELETE.getIcon());
		jClear.setActionCommand(ReprocessedAction.CLEAR.name());
		jClear.addActionListener(listener);
		jToolBarLeft.addButton(jClear);

		jToolBarLeft.addSpace(30);

		JLabel jInfo = new JLabel(TabsReprocessed.get().info());
		jInfo.setMinimumSize(new Dimension(100, Program.getButtonsHeight()));
		jInfo.setMaximumSize(new Dimension(Short.MAX_VALUE, Program.getButtonsHeight()));
		jInfo.setHorizontalAlignment(SwingConstants.LEFT);
		jToolBarLeft.add(jInfo);

		JFixedToolBar jToolBarRight = new JFixedToolBar();

		JButton jCollapse = new JButton(TabsReprocessed.get().collapse(), Images.MISC_COLLAPSED.getIcon());
		jCollapse.setActionCommand(ReprocessedAction.COLLAPSE.name());
		jCollapse.addActionListener(listener);
		jToolBarRight.addButton(jCollapse);

		JButton jExpand = new JButton(TabsReprocessed.get().expand(), Images.MISC_EXPANDED.getIcon());
		jExpand.setActionCommand(ReprocessedAction.EXPAND.name());
		jExpand.addActionListener(listener);
		jToolBarRight.addButton(jExpand);

		//Table Format
		tableFormat = TableFormatFactory.reprocessedTableFormat();
		//Backend
		eventList = EventListManager.create();
		//Sorting (per column)
		eventList.getReadWriteLock().readLock().lock();
		SortedList<ReprocessedInterface> sortedListColumn = new SortedList<>(eventList);
		eventList.getReadWriteLock().readLock().unlock();

		//Sorting Total (Ensure that total is always last)
		eventList.getReadWriteLock().readLock().lock();
		SortedList<ReprocessedInterface> sortedListTotal = new SortedList<>(sortedListColumn, new TotalComparator());
		eventList.getReadWriteLock().readLock().unlock();
		//Filter
		eventList.getReadWriteLock().readLock().lock();
		filterList = new FilterList<>(sortedListTotal);
		eventList.getReadWriteLock().readLock().unlock();
		//Separator
		separatorList = new SeparatorList<>(filterList, new ReprocessedSeparatorComparator(), 1, Integer.MAX_VALUE);
		//Table Model
		tableModel = EventModels.createTableModel(separatorList, tableFormat);
		//Table
		jTable = new JReprocessedTable(program, tableModel, separatorList);
		jTable.setSeparatorRenderer(new ReprocessedSeparatorTableCell(jTable, separatorList, listener));
		jTable.setSeparatorEditor(new ReprocessedSeparatorTableCell(jTable, separatorList, listener));
		jTable.setCellSelectionEnabled(true);
		PaddingTableCellRenderer.install(jTable, 3);
		//Sorting
		TableComparatorChooser.install(jTable, sortedListColumn, TableComparatorChooser.MULTIPLE_COLUMN_MOUSE, tableFormat);
		//Selection Model
		selectionModel = EventModels.createSelectionModel(separatorList);
		selectionModel.setSelectionMode(ListSelection.MULTIPLE_INTERVAL_SELECTION_DEFENSIVE);
		jTable.setSelectionModel(selectionModel);
		//Listeners
		installTable(jTable);
		//Scroll
		JScrollPane jTableScroll = new JScrollPane(jTable);
		//Table Filter
		filterControl = new ReprocessedFilterControl(sortedListTotal);
		//Menu
		installTableTool(new ReprocessedTableMenu(), tableFormat, tableModel, jTable, filterControl, ReprocessedInterface.class);

		//Add item dialog
		ArrayList<Item> reprocessableItems = new ArrayList<>();
		for(Item item : StaticData.get().getItems().values()) {
			if(!item.getReprocessedMaterial().isEmpty()) {
				reprocessableItems.add(item);
			}
		}
		jAddItemDialog = new JReprocessedAddItemDialog(program);
		jAddItemDialog.updateData(reprocessableItems);

		layout.setHorizontalGroup(
			layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
				.addComponent(filterControl.getPanel())
				.addGroup(layout.createSequentialGroup()
					.addComponent(jToolBarLeft, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Integer.MAX_VALUE)
					.addGap(0)
					.addComponent(jToolBarRight)
				)
				.addComponent(jTableScroll, 0, 0, Short.MAX_VALUE)
		);
		layout.setVerticalGroup(
			layout.createSequentialGroup()
				.addComponent(filterControl.getPanel())
				.addGroup(layout.createParallelGroup()
					.addComponent(jToolBarLeft, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(jToolBarRight, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				)
				.addComponent(jTableScroll, 0, 0, Short.MAX_VALUE)
		);
	}

	@Override
	public void updateData() {
		//Save separator expanded/collapsed state
		jTable.saveExpandedState();
		//Update Data
		reprocessedData.updateData(eventList, typeIDs);
		//Restore separator expanded/collapsed state
		jTable.loadExpandedState();
	}

	@Override
	public void clearData() {
		filterControl.clearCache();
	}

	@Override
	public void updateCache() {
		filterControl.createCache();
	}

	@Override
	public Collection<LocationType> getLocations() {
		return new ArrayList<>(); //No Location
	}

	public void set(final Set<Integer> newTypeIDs) {
		typeIDs.clear();
		add(newTypeIDs);
	}

	public void add(final Set<Integer> newTypeIDs) {
		typeIDs.addAll(newTypeIDs);
	}

	public void show() {
		jTable.clearExpandedState();
		if (program.getMainWindow().isOpen(this)) {
			updateData(); //Also update data when already open
		}
		program.getMainWindow().addTab(this);	
	}

	private class ReprocessedTableMenu implements TableMenu<ReprocessedInterface> {
		@Override
		public MenuData<ReprocessedInterface> getMenuData() {
			return new MenuData<>(selectionModel.getSelected());
		}

		@Override
		public JMenu getFilterMenu() {
			return filterControl.getMenu(jTable, selectionModel.getSelected());
		}

		@Override
		public JMenu getColumnMenu() {
			return new JMenuColumns<>(program, tableFormat, tableModel, jTable, NAME);
		}

		@Override
		public void addInfoMenu(JComponent jComponent) { }

		@Override
		public void addToolMenu(JComponent jComponent) { }
	}

	private class ListenerClass implements ActionListener {
		@Override
		public void actionPerformed(final ActionEvent e) {
			if (ReprocessedAction.COLLAPSE.name().equals(e.getActionCommand())) {
				jTable.expandSeparators(false);
			}
			if (ReprocessedAction.EXPAND.name().equals(e.getActionCommand())) {
				jTable.expandSeparators(true);
			}
			if (ReprocessedAction.CLEAR.name().equals(e.getActionCommand())) {
				typeIDs.clear();
				updateData();
			}
			if (ReprocessedCellAction.REMOVE.name().equals(e.getActionCommand())) {
				int index = jTable.getSelectedRow();
				Object o = tableModel.getElementAt(index);
				if (o instanceof SeparatorList.Separator<?>) {
					SeparatorList.Separator<?> separator = (SeparatorList.Separator<?>) o;
					ReprocessedInterface item = (ReprocessedInterface) separator.first();
					ReprocessedTotal total = item.getTotal();
					typeIDs.remove(total.getItem().getTypeID());
					updateData();
				}
			}
			if (ReprocessedAction.ADD_ITEM.name().equals(e.getActionCommand())) {
				Item selectedItem = jAddItemDialog.show();
				if(selectedItem != null) {
					typeIDs.add(selectedItem.getTypeID());
					updateData();
				}
			}
		}
	}

	public static class TotalComparator implements Comparator<ReprocessedInterface> {

		private final Comparator<ReprocessedInterface> comparator;

		public TotalComparator() {
			List<Comparator<ReprocessedInterface>> comparators = new ArrayList<>();
			comparators.add(new ReprocessedSeparatorComparator());
			comparators.add(new InnerTotalComparator());
			comparator = GlazedLists.chainComparators(comparators);
		}

		@Override
		public int compare(final ReprocessedInterface o1, final ReprocessedInterface o2) {
			return comparator.compare(o1, o2);
		}

		private static class InnerTotalComparator implements Comparator<ReprocessedInterface> {
			@Override
			public int compare(final ReprocessedInterface o1, final ReprocessedInterface o2) {
				if (o1.isTotal() && o2.isTotal()) {
					return 0;  //Equal (both StockpileTotal)
				} else if (o1.isTotal()) {
					return 1;  //After
				} else if (o2.isTotal()) {
					return -1; //Before
				} else {
					return 0;  //Equal (not StockpileTotal)
				}
			}
		}
	}

	private class ReprocessedFilterControl extends FilterControl<ReprocessedInterface> {

		public ReprocessedFilterControl(EventList<ReprocessedInterface> exportEventList) {
			super(program.getMainWindow().getFrame(),
					NAME,
					tableFormat,
					eventList,
					exportEventList,
					filterList
					);
		}

		@Override
		protected void afterFilter() {
			jTable.loadExpandedState();
		}

		@Override
		protected void beforeFilter() {
			jTable.saveExpandedState();
		}

		@Override
		public void saveSettings(final String msg) {
			program.saveSettings("Reprocessed Table: " + msg); //Save Reprocessed Filters and Export Setttings
		}
	}

}
