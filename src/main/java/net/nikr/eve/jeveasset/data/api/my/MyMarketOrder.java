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
package net.nikr.eve.jeveasset.data.api.my;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.Set;
import javax.management.timer.Timer;
import javax.swing.JButton;
import net.nikr.eve.jeveasset.data.api.accounts.OwnerType;
import net.nikr.eve.jeveasset.data.api.raw.RawMarketOrder;
import net.nikr.eve.jeveasset.data.sde.Item;
import net.nikr.eve.jeveasset.data.sde.MyLocation;
import net.nikr.eve.jeveasset.data.settings.Settings;
import net.nikr.eve.jeveasset.data.settings.types.BlueprintType;
import net.nikr.eve.jeveasset.data.settings.types.ContractPriceType;
import net.nikr.eve.jeveasset.data.settings.types.EditableLocationType;
import net.nikr.eve.jeveasset.data.settings.types.EditablePriceType;
import net.nikr.eve.jeveasset.data.settings.types.ItemType;
import net.nikr.eve.jeveasset.data.settings.types.LastTransactionType;
import net.nikr.eve.jeveasset.data.settings.types.MarketDetailType;
import net.nikr.eve.jeveasset.data.settings.types.OwnersType;
import net.nikr.eve.jeveasset.gui.shared.components.JButtonComparable;
import net.nikr.eve.jeveasset.gui.shared.table.containers.Percent;
import net.nikr.eve.jeveasset.gui.tabs.orders.Outbid;
import net.nikr.eve.jeveasset.i18n.TabsOrders;

public class MyMarketOrder extends RawMarketOrder implements Comparable<MyMarketOrder>, EditableLocationType, ItemType, BlueprintType, EditablePriceType, ContractPriceType, OwnersType, LastTransactionType, MarketDetailType {

	public enum OrderStatus {
		ACTIVE() {
			@Override
			String getI18N() {
				return TabsOrders.get().statusActive();
			}
		},
		CLOSED() {
			@Override
			String getI18N() {
				return TabsOrders.get().statusClosed();
			}
		},
		FULFILLED() {
			@Override
			String getI18N() {
				return TabsOrders.get().statusFulfilled();
			}
		},
		EXPIRED() {
			@Override
			String getI18N() {
				return TabsOrders.get().statusExpired();
			}
		},
		PARTIALLY_FULFILLED() {
			@Override
			String getI18N() {
				return TabsOrders.get().statusPartiallyFulfilled();
			}
		},
		CANCELLED() {
			@Override
			String getI18N() {
				return TabsOrders.get().statusCancelled();
			}
		},
		PENDING() {
			@Override
			String getI18N() {
				return TabsOrders.get().statusPending();
			}
		},
		CHARACTER_DELETED() {
			@Override
			String getI18N() {
				return TabsOrders.get().statusCharacterDeleted();
			}
		},
		UNKNOWN() {
			@Override
			String getI18N() {
				return TabsOrders.get().statusUnknown();
			}
		};

		abstract String getI18N();

		@Override
		public String toString() {
			return getI18N();
		}
	}

	private final Set<Long> owners;
	private Item item;
	private MyLocation location;
	private OrderStatus status;
	private OwnerType owner;
	private double price;
	private double contractPrice;
	private double transactionPrice;
	private double transactionProfitDifference;
	private Percent transactionProfitPercent;
	private String issuedByName = "";
	private Double brokersFee;
	private Outbid outbid;
	private double priceReprocessed;
	//soft init
	private JButton jButton;

	public MyMarketOrder(final RawMarketOrder rawMarketOrder, final Item item, final OwnerType owner) {
		super(rawMarketOrder);
		this.item = item;
		this.owner = owner;
		this.owners = Collections.singleton(owner.getOwnerID());
		if (isExpired()) { //expired (status may be out-of-date)
			if (this.getVolumeRemain() == 0) {
				status = OrderStatus.FULFILLED;
			} else if (Objects.equals(this.getVolumeRemain(), this.getVolumeTotal())) {
				status = OrderStatus.EXPIRED;
			} else {
				status = OrderStatus.PARTIALLY_FULFILLED;
			}
		} else {
			MarketOrderState state = getState();
			if (state == null) {
				status = null;
			} else {
				switch (state) {
					case OPEN: //open/active
						status = OrderStatus.ACTIVE;
						break;
					case CLOSED: //closed
						status = OrderStatus.CLOSED;
						break;
					case EXPIRED: //expired (or fulfilled)
						if (this.getVolumeRemain() == 0) {
							status = OrderStatus.FULFILLED;
						} else if (Objects.equals(this.getVolumeRemain(), this.getVolumeTotal())) {
							status = OrderStatus.EXPIRED;
						} else {
							status = OrderStatus.PARTIALLY_FULFILLED;
						}
						break;
					case CANCELLED: //cancelled
						status = OrderStatus.CANCELLED;
						break;
					case PENDING: //pending
						status = OrderStatus.PENDING;
						break;
					case CHARACTER_DELETED: //character deleted
						status = OrderStatus.CHARACTER_DELETED;
						break;
					case UNKNOWN: //Unknown or Auto Closed
						status = OrderStatus.UNKNOWN;
						break;
				}
			}
		}
	}

	public void close() {
		if (status == OrderStatus.ACTIVE) {
			setState(MarketOrderState.UNKNOWN);
			status = OrderStatus.UNKNOWN;
		}
	}

	@Override
	public int compareTo(final MyMarketOrder o) {
		return  Long.compare(o.getOrderID(), this.getOrderID());
	}

	public Date getExpires() {
		long expires = (this.getIssued().getTime() + ((this.getDuration()) * Timer.ONE_DAY));
		return new Date(expires);
	}

	public final boolean isExpired() {
		return getExpires().before(new Date());
	}

	public final boolean isNearExpired() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, Settings.get().getMarketOrdersSettings().getExpireWarnDays());
		return getExpires().before(cal.getTime());
	}

	public final boolean isNearFilled() {
		//How much do we want to worry about precision vs cost of converting to doubles for possibly
		//thousands of orders in history, if we want more accuracy cast to doubles first and change
		//comparison to Double.compare

		//Multiply by 100 since we are dealing with percents as integers.
		//This will be slightly inaccurate since int are essentially floored if they have a remainder.
		return ( (getVolumeRemain() * 100) / getVolumeTotal() <= Settings.get().getMarketOrdersSettings().getRemainingWarnPercent());
	}

	public boolean isActive() {
		return getState() == MarketOrderState.OPEN && !isExpired();
	}

	@Override
	public boolean isBPC() {
		return false; //Market Orders are always BPO
	}

	@Override
	public boolean isBPO() {
		return item.isBlueprint();
	}

	@Override
	public int getRuns() {
		return -1; //BPO - Can not sell BPC
	}

	@Override
	public int getMaterialEfficiency() {
		return 0; //Zero - Can not sell researched blueprints 
	}

	@Override
	public int getTimeEfficiency() {
		return 0; //Zero - Can not sell researched blueprints
	}

	@Override
	public void setDynamicPrice(double price) {
		this.price = price;
	}

	@Override
	public Double getDynamicPrice() {
		return price;
	}

	@Override
	public double getContractPrice() {
		return contractPrice;
	}

	@Override
	public void setContractPrice(double contractPrice) {
		this.contractPrice = contractPrice;
	}

	@Override
	public double getTransactionPrice() {
		return transactionPrice;
	}

	@Override
	public double getTransactionProfitDifference() {
		return transactionProfitDifference;
	}

	@Override
	public Percent getTransactionProfitPercent() {
		return transactionProfitPercent;
	}

	@Override
	public void setTransactionPrice(double transactionPrice) {
		this.transactionPrice = transactionPrice;
	}

	@Override
	public void setTransactionProfit(double transactionProfitDifference) {
		this.transactionProfitDifference = transactionProfitDifference;
	}

	@Override
	public void setTransactionProfitPercent(Percent transactionProfitPercent) {
		this.transactionProfitPercent = transactionProfitPercent;
	}

	public double getMarketMargin() {
		if (getDynamicPrice() > 0 && getPrice() > 0) {
			if (isBuyOrder()) {
				return (getDynamicPrice() - getPrice()) / getDynamicPrice();
			} else {
				return (getPrice() - getDynamicPrice()) / getPrice();
			}
		} else {
			return 0;
		}
	}

	public double getMarketProfit() {
		if (isBuyOrder()) {
			return getDynamicPrice() - getPrice();
		} else {
			return getPrice() - getDynamicPrice();
		}
	}

	public Date getCreatedOrIssued() {
		if (!getChanges().isEmpty()) {
			return getChanges().iterator().next().getDate();
		} else {
			return getIssued();
		}
	}

	@Override
	public Item getItem() {
		return item;
	}

	@Override
	public long getItemCount() {
		return getVolumeRemain();
	}

	@Override
	public MyLocation getLocation() {
		return location;
	}

	@Override
	public Set<Long> getOwners() {
		return owners;
	}

	@Override
	public void setLocation(MyLocation location) {
		this.location = location;
	}

	public String getIssuedByName() {
		return issuedByName;
	}

	public void setIssuedByName(String issuedByName) {
		this.issuedByName = issuedByName;
	}

	public OwnerType getOwner() {
		return owner;
	}

	public String getOwnerName() {
		return owner.getOwnerName();
	}

	public long getOwnerID() {
		return owner.getOwnerID();
	}

	public OrderStatus getStatus() {
		return status;
	}

	public boolean isCorporation() {
		return owner.isCorporation();
	}

	public Double getBrokersFee() {
		return brokersFee;
	}

	public Double getBrokersFeeNotNull() {
		if (brokersFee != null) {
			return brokersFee;
		} else {
			return 0.0;
		}
	}

	public void setBrokersFee(Double brokerFee) {
		this.brokersFee = brokerFee;
	}

	public boolean isOutbid() {
		if (outbid == null) {
			return false;
		}
		return outbid.getCount() > 0;
	}

	public boolean haveOutbid() {
		return outbid != null;
	}

	public Double getOutbidPrice() {
		if (outbid == null) {
			return null;
		}
		return outbid.getPrice();
	}

	public Long getOutbidCount() {
		if (outbid == null) {
			return null;
		}
		return outbid.getCount();
	}

	public Double getOutbidDelta() {
		if (outbid == null) {
			return null;
		}
		if (isBuyOrder()) {
			return getPrice() - outbid.getPrice();
		} else {
			return outbid.getPrice() - getPrice();
		}
		
	}

	public void setOutbid(Outbid outbid) {
		this.outbid = outbid;
	}

	public double getPriceReprocessed() {
		return priceReprocessed;
	}

	public void setPriceReprocessed(double priceReprocessed) {
		this.priceReprocessed = priceReprocessed;
	}

	@Override
	public JButton getButton() {
		if (jButton == null) {
			jButton = new JButtonComparable(TabsOrders.get().eveUiOpen());
		}
		return jButton;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 89 * hash + Objects.hashCode(this.getOrderID()); //OrderID is globaly unique
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final MyMarketOrder other = (MyMarketOrder) obj;
		return Objects.equals(this.getOrderID(), other.getOrderID()); //OrderID is globaly unique
	}
}
