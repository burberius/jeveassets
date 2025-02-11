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
package net.nikr.eve.jeveasset.gui.tabs.routing;

import ca.odell.glazedlists.TextFilterator;
import java.util.Comparator;
import java.util.List;
import net.nikr.eve.jeveasset.Program;
import net.nikr.eve.jeveasset.data.sde.MyLocation;
import net.nikr.eve.jeveasset.gui.images.Images;
import net.nikr.eve.jeveasset.gui.shared.components.JAutoCompleteDialog;
import net.nikr.eve.jeveasset.i18n.TabsRouting;


public class JStationDialog extends JAutoCompleteDialog<MyLocation> {

	public JStationDialog(Program program) {
		super(program, TabsRouting.get().addStationTitle(), Images.TOOL_ROUTING.getImage(), TabsRouting.get().addStationSelect(), true, true);
	}

	@Override
	protected MyLocation getValue(Object object) {
		if (object instanceof MyLocation) {
			return (MyLocation) object;
		} else {
			return null;
		}
	}

	@Override
	protected Comparator<MyLocation> getComparator() {
		return new StationComparator();
	}

	@Override
	protected TextFilterator<MyLocation> getFilterator() {
		return new Filterator();
	}

	private static class Filterator implements TextFilterator<MyLocation> {
		@Override
		public void getFilterStrings(final List<String> baseList, final MyLocation element) {
			baseList.add(element.getLocation());
		}
	}

	private static class StationComparator implements Comparator<MyLocation> {
		@Override
		public int compare(MyLocation o1, MyLocation o2) {
			return o1.getLocation().compareToIgnoreCase(o2.getLocation());
		}
	}
}
