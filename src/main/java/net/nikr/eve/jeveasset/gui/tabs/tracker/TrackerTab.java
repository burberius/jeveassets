/*
 * Copyright 2009, 2010, 2011, 2012 Contributors (see credits.txt)
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

package net.nikr.eve.jeveasset.gui.tabs.tracker;

import com.beimin.eveapi.shared.accountbalance.EveAccountBalance;
import com.beimin.eveapi.shared.marketorders.ApiMarketOrder;
import com.toedter.calendar.JCalendar;
import com.toedter.calendar.JDateChooser;
import com.toedter.calendar.JTextFieldDateEditor;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import net.nikr.eve.jeveasset.Program;
import net.nikr.eve.jeveasset.data.Account;
import net.nikr.eve.jeveasset.data.Asset;
import net.nikr.eve.jeveasset.data.Human;
import net.nikr.eve.jeveasset.data.Settings;
import net.nikr.eve.jeveasset.gui.images.Images;
import net.nikr.eve.jeveasset.gui.shared.Formater;
import net.nikr.eve.jeveasset.gui.shared.components.JMainTab;
import net.nikr.eve.jeveasset.i18n.TabsTracker;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
import org.jfree.data.time.TimePeriodValuesCollection;


public class TrackerTab extends JMainTab {

	private static final String ACTION_UPDATE_DATA = "ACTION_UPDATE_DATA";
	private static final String ACTION_UPDATE_SHOWN = "ACTION_UPDATE_SHOWN";
	private static final String ACTION_All = "ACTION_All";

	private JFreeChart jNextChart;
	private JDateChooser jFrom;
	private JDateChooser jTo;
	private JComboBox jOwners;
	private JCheckBox jAll;
	private JCheckBox jTotal;
	private JCheckBox jWalletBalance;
	private JCheckBox jAssets;
	private JCheckBox jSellOrders;
	private JCheckBox jEscrows;
	private JCheckBox jEscrowsToCover;

	private Listener listener = new Listener();

	private TimePeriodValuesCollection dataset = new TimePeriodValuesCollection();
	TimePeriodValues total;
	TimePeriodValues walletBalance;
	TimePeriodValues assets;
	TimePeriodValues sellOrders;
	TimePeriodValues escrows;
	TimePeriodValues escrowsToCover;

	public TrackerTab(Program program) {
		super(program, TabsTracker.get().title(), Images.TOOL_TRACKER.getIcon(), true);

		jFrom = createDateChooser(TabsTracker.get().from());
		jTo = createDateChooser(TabsTracker.get().to());

		jOwners = new JComboBox();
		jOwners.setActionCommand(ACTION_UPDATE_DATA);
		jOwners.addActionListener(listener);

		jAll = new JCheckBox(TabsTracker.get().all());
		jAll.setSelected(true);
		jAll.setActionCommand(ACTION_All);
		jAll.addActionListener(listener);
		jAll.setFont(new Font(jAll.getFont().getName(), Font.ITALIC, jAll.getFont().getSize()));

		jTotal = new JCheckBox(TabsTracker.get().total());
		jTotal.setSelected(true);
		jTotal.setActionCommand(ACTION_UPDATE_SHOWN);
		jTotal.addActionListener(listener);

		jWalletBalance = new JCheckBox(TabsTracker.get().walletBalanc());
		jWalletBalance.setSelected(true);
		jWalletBalance.setActionCommand(ACTION_UPDATE_SHOWN);
		jWalletBalance.addActionListener(listener);

		jAssets = new JCheckBox(TabsTracker.get().assets());
		jAssets.setSelected(true);
		jAssets.setActionCommand(ACTION_UPDATE_SHOWN);
		jAssets.addActionListener(listener);

		jSellOrders = new JCheckBox(TabsTracker.get().sellOrders());
		jSellOrders.setSelected(true);
		jSellOrders.setActionCommand(ACTION_UPDATE_SHOWN);
		jSellOrders.addActionListener(listener);

		jEscrows = new JCheckBox(TabsTracker.get().escrows());
		jEscrows.setSelected(true);
		jEscrows.setActionCommand(ACTION_UPDATE_SHOWN);
		jEscrows.addActionListener(listener);

		jEscrowsToCover = new JCheckBox(TabsTracker.get().escrowsToCover());
		jEscrowsToCover.setSelected(true);
		jEscrowsToCover.setActionCommand(ACTION_UPDATE_SHOWN);
		jEscrowsToCover.addActionListener(listener);

		DateAxis domainAxis = new DateAxis(TabsTracker.get().date());
		domainAxis.setDateFormatOverride(new SimpleDateFormat("dd-MM-yyyy"));
		domainAxis.setVerticalTickLabels(true);
		domainAxis.setAutoTickUnitSelection(true);
		domainAxis.setAutoRange(true);

		ValueAxis rangeAxis = new NumberAxis(TabsTracker.get().isk());
		rangeAxis.setAutoRange(true);
		rangeAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());

		XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, new XYLineAndShapeRenderer(true, true));
		plot.setBackgroundPaint(Color.WHITE);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.getRenderer().setBaseToolTipGenerator(new StandardXYToolTipGenerator(
				"{0}: {2} ({1})",
				new SimpleDateFormat("dd-MM-yyyy"),
				new DecimalFormat("#,##0.00 isk")));

		jNextChart = new JFreeChart(plot);
		jNextChart.setAntiAlias(true);
		jNextChart.setBackgroundPaint(jPanel.getBackground());

		ChartPanel jChartPanel = new ChartPanel(jNextChart);
		jChartPanel.addMouseListener(listener);
		jChartPanel.setDomainZoomable(false);
		jChartPanel.setRangeZoomable(false);
		jChartPanel.setPopupMenu(null);

		layout.setHorizontalGroup(
			layout.createSequentialGroup()
				.addComponent(jChartPanel)
				.addGroup(layout.createParallelGroup()
					.addComponent(jOwners)
					.addComponent(jFrom)
					.addComponent(jTo)
					.addComponent(jAll)
					.addComponent(jTotal)
					.addComponent(jWalletBalance)
					.addComponent(jAssets)
					.addComponent(jSellOrders)
					.addComponent(jEscrows)
					.addComponent(jEscrowsToCover)
				)
		);
		layout.setVerticalGroup(
			layout.createParallelGroup()
				.addComponent(jChartPanel)
				.addGroup(layout.createSequentialGroup()
					.addComponent(jOwners, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT)
					.addComponent(jFrom, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(jTo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
					.addComponent(jAll, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT)
					.addComponent(jTotal, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT)
					.addComponent(jWalletBalance, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT)
					.addComponent(jAssets, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT)
					.addComponent(jSellOrders, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT)
					.addComponent(jEscrows, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT)
					.addComponent(jEscrowsToCover, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT, Program.BUTTONS_HEIGHT)
					//.addGap(0, 0, Integer.MAX_VALUE)
				)
		);
	}

	private JDateChooser createDateChooser(String title) {
		JDateChooser jDate = new JDateChooser(Settings.getNow());
		jDate.setBorder(BorderFactory.createTitledBorder(title));
		jDate.setDateFormatString(Formater.COLUMN_FORMAT);
		jDate.setCalendar(null);
		JCalendar jCalendar = jDate.getJCalendar();
		jCalendar.setTodayButtonText("Today");
		jCalendar.setTodayButtonVisible(true);
		jCalendar.setNullDateButtonText("Clear");
		jCalendar.setNullDateButtonVisible(true);
		JTextFieldDateEditor dateEditor = (JTextFieldDateEditor) jDate.getDateEditor().getUiComponent();
		dateEditor.setEnabled(false);
		dateEditor.setBorder(null);
		dateEditor.setDisabledTextColor(Color.BLACK);
		dateEditor.setHorizontalAlignment(JTextFieldDateEditor.CENTER);
		jDate.addPropertyChangeListener(listener);
		return jDate;
	}

	public void createTrackerDataPoint() {
		double allTotal = 0;
		double allWalletBalance = 0;
		double allAssets = 0;
		double allSellOrders = 0;
		double allEscrows = 0;
		double allEscrowsToCover = 0;
		Date date = new Date();
		for (Account account : program.getSettings().getAccounts()) {
			for (Human human : account.getHumans()) {
				if (!human.isShowAssets()) { //Ignore hidden owners
					continue;
				}
				TrackerOwner owner = new TrackerOwner(human.getOwnerID(), human.getName());
				//Add new owner:
				if (!program.getSettings().getTrackerData().containsKey(owner)) {
					program.getSettings().getTrackerData().put(owner, new ArrayList<TrackerData>());
				}
				//Assets
				double assetValue = deepAsset(human.getAssets());
				allAssets = allAssets + assetValue;
				//Account Balance
				double accountBalanceValue = 0;
				for (EveAccountBalance accountBalance : human.getAccountBalances()) {
					accountBalanceValue = accountBalanceValue + accountBalance.getBalance();
					allWalletBalance = allWalletBalance + accountBalance.getBalance();
				}
				//Market Orders
				double sellOrdersValue = 0;
				double escrowsValue = 0;
				double escrowsToCoverValue = 0;
				for (ApiMarketOrder apiMarketOrder : human.getMarketOrders()) {
					if (apiMarketOrder.getOrderState() == 0) {
						if (apiMarketOrder.getBid() < 1) { //Sell Orders
							sellOrdersValue = sellOrdersValue + (apiMarketOrder.getPrice() * apiMarketOrder.getVolRemaining());
							allSellOrders = allSellOrders + (apiMarketOrder.getPrice() * apiMarketOrder.getVolRemaining());
						} else { //Buy Orders
							escrowsValue = escrowsValue + apiMarketOrder.getEscrow();
							allEscrows = allEscrows + apiMarketOrder.getEscrow();
							escrowsToCoverValue = escrowsToCoverValue + ((apiMarketOrder.getPrice() * apiMarketOrder.getVolRemaining()) - apiMarketOrder.getEscrow());
							allEscrowsToCover = allEscrowsToCover + ((apiMarketOrder.getPrice() * apiMarketOrder.getVolRemaining()) - apiMarketOrder.getEscrow());
						}
					}
				}
				//Total
				double totalValue = assetValue + accountBalanceValue + sellOrdersValue + escrowsValue;
				allTotal = allTotal + totalValue;
				//Add data
				TrackerData data = new TrackerData(date, totalValue, accountBalanceValue, assetValue, sellOrdersValue, escrowsValue, escrowsToCoverValue);
				program.getSettings().getTrackerData().get(owner).add(data);
			}
		}
		//Add all
		TrackerOwner owner = new TrackerOwner();
		if (!program.getSettings().getTrackerData().containsKey(owner)) {
			program.getSettings().getTrackerData().put(owner, new ArrayList<TrackerData>());
		}
		TrackerData data = new TrackerData(date, allTotal, allWalletBalance, allAssets, allSellOrders, allEscrows, allEscrowsToCover);
		program.getSettings().getTrackerData().get(owner).add(data);
		//Update data
		updateData();
	}

	private double deepAsset(List<Asset> assets) {
		double assetValue = 0;
		for (Asset asset : assets) {
			assetValue = assetValue + (asset.getPrice() * asset.getCount());
			assetValue = assetValue + deepAsset(asset.getAssets());
		}
		return assetValue;
	}

	@Override
	public void updateTableMenu(JComponent jComponent) {
		jComponent.removeAll();
		jComponent.setEnabled(false);
	}

	@Override
	public void updateData() {
		Set<TrackerOwner> owners = new TreeSet<TrackerOwner>(program.getSettings().getTrackerData().keySet());
		if (owners.isEmpty()) {
			jOwners.setEnabled(false);
			jOwners.getModel().setSelectedItem(new TrackerOwner(-1, TabsTracker.get().noDataFound()));
		} else {
			jOwners.setEnabled(true);
			jOwners.setModel(new DefaultComboBoxModel(owners.toArray()));
		}
		createData();
	}

	private void createData() {
		TrackerOwner owner = (TrackerOwner) jOwners.getSelectedItem();
		total = new TimePeriodValues(TabsTracker.get().total());
		walletBalance = new TimePeriodValues(TabsTracker.get().walletBalanc());
		assets = new TimePeriodValues(TabsTracker.get().assets());
		sellOrders = new TimePeriodValues(TabsTracker.get().sellOrders());
		escrows = new TimePeriodValues(TabsTracker.get().escrows());
		escrowsToCover = new TimePeriodValues(TabsTracker.get().escrowsToCover());
		Date from = jFrom.getDate();
		if (from != null) { //Start of day
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(from);
			calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),calendar.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
			from = calendar.getTime();
		}
		Date to = jTo.getDate();
		if (to != null) { //End of day
			Calendar calendar = Calendar.getInstance();
			calendar.setTime(to);
			calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), 23, 59, 59);
			to = calendar.getTime();
		}
		if (!owner.isEmpty()) { //No data set...
			for (TrackerData data : program.getSettings().getTrackerData().get(owner)) {
				SimpleTimePeriod date = new SimpleTimePeriod(data.getDate(), data.getDate());
				if ((from == null || data.getDate().after(from)) && (to == null || data.getDate().before(to))) {
					total.add(date, data.getTotal());
					walletBalance.add(date, data.getWalletBalance());
					assets.add(date, data.getAssets());
					sellOrders.add(date, data.getSellOrders());
					escrows.add(date, data.getEscrows());
					escrowsToCover.add(date, data.getEscrowsToCover());
				}
			}
		}
		updateShown();
	}

	private void updateShown() {
		//Remove All
		while (dataset.getSeriesCount() != 0) {
			dataset.removeSeries(0);
		}

		if (jTotal.isSelected() && total != null) {
			dataset.addSeries(total);
			updateRender(dataset.getSeriesCount() - 1, Color.RED.darker());
		}
		if (jWalletBalance.isSelected() && walletBalance != null) {
			dataset.addSeries(walletBalance);
			updateRender(dataset.getSeriesCount() - 1, Color.BLUE.darker());

		}
		if (jAssets.isSelected() && assets != null) {
			dataset.addSeries(assets);
			updateRender(dataset.getSeriesCount() - 1, Color.GREEN.darker().darker());
		}
		if (jSellOrders.isSelected() && sellOrders != null) {
			dataset.addSeries(sellOrders);
			updateRender(dataset.getSeriesCount() - 1, Color.CYAN.darker());
		}
		if (jEscrows.isSelected() && escrows != null) {
			dataset.addSeries(escrows);
			updateRender(dataset.getSeriesCount() - 1, Color.BLACK);
		}
		if (jEscrowsToCover.isSelected() && escrowsToCover != null) {
			dataset.addSeries(escrowsToCover);
			updateRender(dataset.getSeriesCount() - 1, Color.GRAY);
		}
		//Add empty dataset
		if (dataset.getSeriesCount() == 0) {
			TimePeriodValues timePeriodValues = new TimePeriodValues(TabsTracker.get().empty());
			dataset.addSeries(timePeriodValues);
			updateRender(dataset.getSeriesCount() - 1, Color.BLACK);
		}
		jNextChart.getXYPlot().getRangeAxis().setAutoRange(true);
		jNextChart.getXYPlot().getDomainAxis().setAutoRange(true);
	}

	private void updateRender(int index, Color color) {
		XYItemRenderer renderer = jNextChart.getXYPlot().getRenderer();
		renderer.setSeriesPaint(index, color);
		renderer.setSeriesStroke(index, new BasicStroke(1));
		renderer.setSeriesShape(index, new Ellipse2D.Float(-3.0f, -3.0f, 6.0f, 6.0f));
	}

	private class Listener extends MouseAdapter implements 
			ActionListener, PropertyChangeListener {

		private int defaultDismissTimeout;
		private int defaultInitialDelay;

		@Override
		public void mouseEntered(MouseEvent me) {
			defaultDismissTimeout = ToolTipManager.sharedInstance().getDismissDelay();
			defaultInitialDelay = ToolTipManager.sharedInstance().getInitialDelay();
			ToolTipManager.sharedInstance().setDismissDelay(60000);
			ToolTipManager.sharedInstance().setInitialDelay(0);
		}

		@Override
		public void mouseExited(MouseEvent me) {
			ToolTipManager.sharedInstance().setDismissDelay(defaultDismissTimeout);
			ToolTipManager.sharedInstance().setInitialDelay(defaultInitialDelay);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (ACTION_UPDATE_DATA.equals(e.getActionCommand())) {
				createData();
			}
			if (ACTION_UPDATE_SHOWN.equals(e.getActionCommand())) {
				updateShown();
				jAll.setSelected(jTotal.isSelected()
						&& jWalletBalance.isSelected()
						&& jAssets.isSelected()
						&& jSellOrders.isSelected()
						&& jEscrows.isSelected()
						&& jEscrowsToCover.isSelected());
			}
			if (ACTION_All.equals(e.getActionCommand())) {
				jTotal.setSelected(jAll.isSelected());
				jWalletBalance.setSelected(jAll.isSelected());
				jAssets.setSelected(jAll.isSelected());
				jSellOrders.setSelected(jAll.isSelected());
				jEscrows.setSelected(jAll.isSelected());
				jEscrowsToCover.setSelected(jAll.isSelected());
				updateShown();
			}
		}

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			Date from = jFrom.getDate();
			Date to = jTo.getDate();
			if (from != null && to != null && from.after(to)) {
				jTo.setDate(from);
			}
			createData();
		}
	}
}
