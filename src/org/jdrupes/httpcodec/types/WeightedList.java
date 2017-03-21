/*
 * This file is part of the JDrupes non-blocking HTTP Codec
 * Copyright (C) 2017 Michael N. Lipp
 *
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jdrupes.httpcodec.types;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * A list of parameterzed values that support a `q` parameter.
 * 
 * @param <I> the type of elements in the list
 */
@SuppressWarnings("serial")
public class WeightedList<I extends ParameterizedValue<?>> 
	extends ArrayList<I> {

	/**
	 * See {@see #sortByWeightDesc()}.
	 */
	private static Comparator<ParameterizedValue<?>> COMP 
		= Comparator.nullsFirst(
			Comparator.comparing(mt -> mt.getParameter("q"),
					Comparator.nullsFirst(
							Comparator.comparing(Float::parseFloat)
							.reversed())));
	
	public void sortByWeightDesc() {
		sort(COMP);
	}

}
