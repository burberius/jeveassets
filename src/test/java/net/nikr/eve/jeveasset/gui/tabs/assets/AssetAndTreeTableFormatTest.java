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
package net.nikr.eve.jeveasset.gui.tabs.assets;

import net.nikr.eve.jeveasset.TestUtil;
import net.nikr.eve.jeveasset.gui.tabs.tree.TreeTableFormat;
import static org.junit.Assert.*;
import org.junit.Test;


public class AssetAndTreeTableFormatTest extends TestUtil {

	@Test
	public void testColumns() {
		for (AssetTableFormat format : AssetTableFormat.values()) {
			try {
				TreeTableFormat.valueOf(format.name());
			} catch (IllegalArgumentException es) {
				fail(format.name() + " is missing from TreeTableFormat");
			}
		}
		for (TreeTableFormat format : TreeTableFormat.values()) {
			try {
				AssetTableFormat.valueOf(format.name());
			} catch (IllegalArgumentException es) {
				fail(format.name() + " is missing from AssetTableFormat");
			}
		}
	}
}
