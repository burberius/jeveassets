/*
 * Copyright 2009-2017 Contributors (see credits.txt)
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

package net.nikr.eve.jeveasset.gui.shared.filter;

import ca.odell.glazedlists.matchers.Matcher;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import net.nikr.eve.jeveasset.gui.shared.Formater;
import net.nikr.eve.jeveasset.gui.shared.filter.Filter.CompareType;
import net.nikr.eve.jeveasset.gui.shared.filter.Filter.LogicType;
import net.nikr.eve.jeveasset.gui.shared.table.EnumTableColumn;
import net.nikr.eve.jeveasset.gui.shared.table.containers.NumberValue;


public class FilterMatcher<E> implements Matcher<E> {

	//TODO i18n Use localized input
	//public static final Locale LOCALE = Locale.getDefault();
	public static final Locale LOCALE = Locale.ENGLISH; //Use english AKA US_EN
	private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(LOCALE);
	private static final NumberFormat PERCENT_FORMAT = NumberFormat.getPercentInstance(LOCALE);
	private static final  Map<String, Map<Object, String>> CACHE = new HashMap<String, Map<Object, String>>();

	private final FilterControl<E> filterControl;
	private final Filter.LogicType logic;
	private final boolean and;
	private final EnumTableColumn<?> enumColumn;
	private final CompareType compare;
	private final String text;
	private final boolean enabled;
	private final Map<Object, String> cache;

	FilterMatcher(final FilterControl<E> filterControl, final Filter filter) {
		this(filterControl, filter.getLogic(), filter.getColumn(), filter.getCompareType(), filter.getText(), true);
	}

	FilterMatcher(final FilterControl<E> filterControl, final LogicType logic, final EnumTableColumn<?> enumColumn, final CompareType compare, final String text, final boolean enabled) {
		this.filterControl = filterControl;
		this.logic = logic;
		this.enumColumn = enumColumn;
		this.compare = compare;
		if (CompareType.isColumnCompare(compare)) {
			this.text = text;
		} else {
			this.text = format(text, true).toLowerCase();
		}
		this.enabled = enabled;
		cache = getCache(filterControl.getName());
		and = logic == Filter.LogicType.AND;
	}

	boolean isAnd() {
		return and;
	}

	public boolean isEmpty() {
		return text.isEmpty() || !enabled;
	}

	@Override
	public boolean matches(final E item) {
		if (enumColumn instanceof Filter.AllColumn) {
			return matchesAll(item, compare, text);
		}
		Object column = filterControl.getColumnValue(item, enumColumn.name());
		if (column == null) {
			return false;
		}
		if (null == compare) { //Fallback: show all...
			return true;
		} else switch (compare) {
			case CONTAINS:
				return contains(column, text);
			case CONTAINS_NOT:
				return !contains(column, text);
			case EQUALS:
				return equals(column, text);
			case EQUALS_DATE:
				return equals(column, text);
			case EQUALS_NOT:
				return !equals(column, text);
			case EQUALS_NOT_DATE:
				return !equals(column, text);
			case GREATER_THAN:
				return great(column, text);
			case LESS_THAN:
				return less(column, text);
			case BEFORE:
				return before(column, text);
			case AFTER:
				return after(column, text);
			case GREATER_THAN_COLUMN:
				return great(column, filterControl.getColumnValue(item, text));
			case LESS_THAN_COLUMN:
				return less(column, filterControl.getColumnValue(item, text));
			case EQUALS_COLUMN:
				return equals(column, format(filterControl.getColumnValue(item, text), false).toLowerCase());
			case EQUALS_NOT_COLUMN:
				return !equals(column, format(filterControl.getColumnValue(item, text), false).toLowerCase());
			case CONTAINS_COLUMN:
				return contains(column, format(filterControl.getColumnValue(item, text), false).toLowerCase());
			case CONTAINS_NOT_COLUMN:
				return !contains(column, format(filterControl.getColumnValue(item, text), false).toLowerCase());
			case BEFORE_COLUMN:
				return before(column, filterControl.getColumnValue(item, text));
			case AFTER_COLUMN:
				return after(column, filterControl.getColumnValue(item, text));
			case LAST_DAYS:
				return lastDays(column, text);
			default:
				//Fallback: show all...
				return true;
		}
	}

	private static Map<Object, String> getCache(String name) {
		Map<Object, String> cache = CACHE.get(name);
		if (cache == null) {
			cache = new HashMap<Object, String>();
			CACHE.put(name, cache);
		}
		return cache;
	}

	public static <E> void cacheDelete(FilterControl<E> filterControl, List<E> delete) {
		if (delete.isEmpty()) {
			return;
		}
		Map<Object, String> cache = getCache(filterControl.getName());
		for (E e : delete) {
			cache.remove(e);
		}
	}

	public static <E> void cacheInsert(FilterControl<E> filterControl, List<E> insert) {
		if (insert.isEmpty()) {
			return;
		}
		Map<Object, String> cache = getCache(filterControl.getName());
		for (E e : insert) {
			String s = buildItemCache(filterControl, e);
			cache.put(e, s);
		}
	}

	public static <E> void cacheUpdate(FilterControl<E> filterControl, List<E> update) {
		if (update.isEmpty()) {
			return;
		}
		System.out.println("Updated " + update.size() + " items");
		cacheInsert(filterControl, update);
	}

	public static <E> void cacheRebuild(FilterControl<E> filterControl) {
		Map<Object, String> cache = getCache(filterControl.getName());
		cache.clear();
		try {
			filterControl.getEventList().getReadWriteLock().readLock().lock();
			for (E e : filterControl.getEventList()) {
				String s = buildItemCache(filterControl, e);
				cache.put(e, s);
			}
		} finally {
			filterControl.getEventList().getReadWriteLock().readLock().unlock();
		}
	}

	private static <E> String buildItemCache(FilterControl<E> filterControl, E e) {
		StringBuilder builder = new StringBuilder();
		for (EnumTableColumn<E> testColumn : filterControl.getColumns()) {
			Object columnValue = filterControl.getColumnValue(e, testColumn.name());
			if (columnValue != null) {
				builder.append("\n");
				builder.append(format(columnValue, false));
				builder.append("\r");
			}
		}
		return builder.toString().toLowerCase();
	}

	private boolean matchesAll(final E item, final Filter.CompareType compareType, final String formatedText) {
		String haystack = cache.get(item);
		if (haystack == null) { //Will be build on update if any filter is set
			haystack = buildItemCache(filterControl, item);
			cache.put(item, haystack);
			throw new RuntimeException("Cache not updated in advance: " + item.getClass() + " " + item.toString());
		}
		if (null == compareType) {
			return true;
		} else switch (compareType) {
			case CONTAINS:
				return haystack.contains(formatedText);
			case CONTAINS_NOT:
				return !haystack.contains(formatedText);
			case EQUALS:
				return haystack.contains("\n" + formatedText + "\r");
			case EQUALS_DATE:
				return haystack.contains("\n" + formatedText + "\r");
			case EQUALS_NOT:
				return !haystack.contains("\n" + formatedText + "\r");
			case EQUALS_NOT_DATE:
				return !haystack.contains("\n" + formatedText + "\r");
			default:
				return true;
		}
	}

	private boolean equals(final Object object1, final String formatedText) {
		//Null
		if (object1 == null || formatedText == null) {
			return false;
		}

		//Equals (case insentive)
		return format(object1, false).toLowerCase().equals(formatedText);
	}

	private boolean contains(final Object object1, final String formatedText) {
		//Null
		if (object1 == null || formatedText == null) {
			return false;
		}

		//Contains (case insentive)
		return format(object1, false).toLowerCase().contains(formatedText);
	}

	private boolean less(final Object object1, final Object object2) {
		return greatThen(object2, object1, false);
	}

	private boolean great(final Object object1, final Object object2) {
		return greatThen(object1, object2, true);
	}

	private boolean greatThen(final Object object1, final Object object2, final boolean fallback) {
		//Null
		if (object1 == null || object2 == null) {
			return fallback;
		}

		//Double / Float
		Double double1 = getDouble(object1);
		Double double2 = getDouble(object2);

		//Long / Integer
		Long long1 = getLong(object1);
		Long long2 = getLong(object2);

		if (long1 != null && long2 != null) {
			return long1 > long2;
		}
		if (long1 != null && double2 != null) {
			return long1 > double2;
		}
		if (double1 != null && double2 != null) {
			return double1 > double2;
		}
		if (double1 != null && long2 != null) {
			return double1 > long2;
		}


		return fallback; //Fallback
	}

	private boolean before(final Object object1, final Object object2) {
		//Date
		Date date1 = getDate(object1, false);
		Date date2 = getDate(object2, true);
		if (date1 != null && date2 != null) {
			return date1.before(date2);
		}
		return false; //Fallback
	}

	private boolean after(final Object object1, final Object object2) {
		Date date1 = getDate(object1, false);
		Date date2 = getDate(object2, true);
		if (date1 != null && date2 != null) {
			return date1.after(date2);
		}
		return false;
	}

	private boolean lastDays(final Object object1, final Object object2) {
		Date date = getDate(object1, false);
		Number days = createNumber(object2);
		if (date != null && days != null) {
			Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);
			calendar.add(Calendar.DAY_OF_MONTH, -days.intValue());  
			return date.after(calendar.getTime());
		}
		return false;
	}

	private static Number getNumber(final Object obj, final boolean userInput) {
		if (obj instanceof Number) {
			return (Number) obj;
		} else if (obj instanceof NumberValue) {
			return ((NumberValue) obj).getNumber();
		} else if (userInput) {
			return createNumber(obj);
		} else {
			return null;
		}
	}
	private Double getDouble(final Object obj) {
		if (obj instanceof Double) {
			return (Double) obj;
		} else if (obj instanceof Float) {
			return Double.valueOf((Float) obj);
		} else if (obj instanceof NumberValue) {
			return ((NumberValue) obj).getDouble();
		} else {
			return createDouble(obj);
		}
	}
	private Long getLong(final Object obj) {
		if (obj instanceof Long) {
			return (Long) obj;
		} else if (obj instanceof Integer) {
			return Long.valueOf((Integer) obj);
		} else if (obj instanceof NumberValue) {
			return ((NumberValue) obj).getLong();
		} else {
			return null;
		}
	}

	private static Double createDouble(final Object object) {
		Number number = parse(object, NUMBER_FORMAT);
		if (number != null) {
			return number.doubleValue();
		} else {
			return null;
		}
	}

	private static Number createNumber(final Object object) {
		Number number = parse(object, NUMBER_FORMAT);
		if (number != null) {
			return number;
		} else {
			return createPercent(object);
		}
	}

	private static Double createPercent(final Object object) {
		Number d = parse(object, PERCENT_FORMAT);
		if (d != null) {
			return d.doubleValue() * 100;
		} else {
			return null;
		}
	}

	private static Number parse(final Object object, final NumberFormat numberFormat) {
		if (object instanceof String) {
			String filterValue = (String) object;
			//Used to check if parsing was successful
			ParsePosition position = new ParsePosition(0);
			//Parse number using the Locale
			Number n = numberFormat.parse(filterValue, position);
			if (n != null && position.getIndex() == filterValue.length()) { //Numeric
				return n;
			}
		}
		return null;
	}

	private static Date getDate(final Object obj, final boolean userInput) {
		if (obj instanceof Date) {
			return (Date) obj;
		} else if (userInput && (obj instanceof String)) {
			return Formater.columnStringToDate((String) obj);
		} else {
			return null;
		}
	}

	static String format(final Object object, final boolean userInput) {
		if (object == null) {
			return null;
		}

		//Number
		Number number = getNumber(object, userInput);
		if (number != null) {
			return Formater.compareFormat(number);
		}

		//Date
		Date date = getDate(object, userInput);
		if (date != null) {
			return Formater.columnDate(date);
		}

		//String
		return object.toString();
	}
}
