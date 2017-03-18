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

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;

import org.jdrupes.httpcodec.util.ListItemizer;

/**
 * Represents a parameterized value
 * such as `value; param1=value1; param2=value2`.
 * 
 * @param <T> the type of the value
 */
public class ParameterizedValue<T> {

	private T value;
	private Map<String, String> params;
	
	/**
	 * Creates a new object with the given value and parameters. 
	 * 
	 * @param value the value
	 * @param parameters the parameters
	 */
	public ParameterizedValue(T value, Map<String,String> parameters) {
		this.value = value;
		this.params = parameters;
	}

	/**
	 * For builder only.
	 */
	private ParameterizedValue() {
		params = new HashMap<>();
	}
	
	/**
	 * Returns the value.
	 * 
	 * @return the value
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Returns the parameters.
	 * 
	 * @return the parameters as unmodifiable map 
	 */
	public Map<String, String> getParameters() {
		return Collections.unmodifiableMap(params);
	}
	
	/**
	 * Return the value of the parameter with the given name.
	 * 
	 * @param name the name
	 * @return the value or `null` if there is no parameter with this name
	 */
	public String getParameter(String name) {
		return params.get(name);
	}
	
	/**
	 * Creates a new builder for a parameterized value.
	 * 
	 * @return the builder
	 */
	public static <T> Builder<ParameterizedValue<T>, T> builder() {
		return new Builder<ParameterizedValue<T>, T>(new ParameterizedValue<>());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((params == null) ? 0 : params.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
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
		@SuppressWarnings("rawtypes")
		ParameterizedValue other = (ParameterizedValue) obj;
		if (params == null) {
			if (other.params != null) {
				return false;
			}
		} else if (!params.equals(other.params)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
			return false;
		}
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public String toString() {
		return new ParameterizedValueConverter<>(new Converter<Object>() {

			@Override
			public String asFieldValue(Object value) {
				return value.toString();
			}

			@Override
			public Object fromFieldValue(String text) throws ParseException {
				throw new UnsupportedOperationException();
			}
			
		}).asFieldValue((ParameterizedValue<Object>)this);
	}

	/**
	 * A builder for the (immutable) parameterized type.
	 */
	public static class Builder<R extends ParameterizedValue<T>, T> {
		
		private R value;
		
		protected Builder(R value) {
			this.value = value;
		}
		
		/**
		 * Initialize the builder from an existing value. 
		 * 
		 * @param existing the existing value, assumed to be immutable
		 * @return the builder for easy chaining
		 */
		public Builder<R, T> from(ParameterizedValue<T> existing) {
			((ParameterizedValue<T>)value).value = existing.getValue();
			((ParameterizedValue<T>)value).params.clear();
			((ParameterizedValue<T>)value).params
				.putAll(existing.getParameters());
			return this;
		}

		/**
		 * Returns the object built.
		 * 
		 * @return the object
		 */
		public R build() {
			R result = value;
			value = null;
			return result;
		}
		
		/**
		 * Set a new value.
		 * 
		 * @param value the value
		 * @return the builder for easy chaining
		 */
		public Builder<R, T> setValue(T value) {
			((ParameterizedValue<T>)this.value).value = value; 
			return this;
		}

		/**
		 * Set a parameter.
		 * 
		 * @param name the parameter name
		 * @param value the value
		 * @return the builder for easy chaining
		 */
		public Builder<R, T> setParameter(String name, String value) {
			((ParameterizedValue<T>)this.value).params.put(name, value);
			return this;
		}

		/**
		 * Remove a parameter
		 * 
		 * @param name the parameter name
		 * @return the builder for easy chaining
		 */
		public Builder<R, T> remove(String name) {
			((ParameterizedValue<T>)this.value).params.remove(name);
			return this;
		}
	}
	
	/**
	 * A base class for converters for parameterized values. 
	 * Converts field values such as `value; param1=value1; param2=value2`.
	 * 
	 * @param <P> the parameterized type
	 * @param <B> the base (unparameterized) type
	 */
	public static class 
		ParamValueConverterBase<P extends ParameterizedValue<B>, B>
		implements Converter<P> {

		private Converter<B> valueConverter;
		private Converter<String> paramValueConverter;
		private BiFunction<B, Map<String,String>, P> resultConstructor;
		
		/**
		 * Creates a new converter by extending the given value converter
		 * with functionality for handling the parameters. Parameter
		 * values are used literally (no quoting).
		 * 
		 * @param valueConverter the converter for a value (without parameters)
		 * @param resultConstructor a method that creates the result
		 * from an instance of the type and a map of parameters
		 * (used by {@link #fromFieldValue(String)}).
		 */
		public ParamValueConverterBase(Converter<B> valueConverter,
				BiFunction<B, Map<String,String>, P> resultConstructor) {
			this(valueConverter, Converters.UNQUOTED_STRING_CONVERTER,
					resultConstructor);
		}

		/**
		 * Creates a new converter by extending the given value converter
		 * with functionality for handling the parameters.
		 * 
		 * @param valueConverter the converter for a value (without parameters)
		 * @param paramValueConverter the converter for parameterValues
		 * @param resultConstructor a method that creates the result
		 * from an instance of the type and a map of parameters
		 * (used by {@link #fromFieldValue(String)}).
		 */
		public ParamValueConverterBase(	Converter<B> valueConverter, 
				Converter<String> paramValueConverter,
				BiFunction<B, Map<String,String>, P> resultConstructor) {
			this.valueConverter = valueConverter;
			this.paramValueConverter = paramValueConverter;
			this.resultConstructor = resultConstructor;
		}

		public String asFieldValue(P value) {
			StringBuilder result = new StringBuilder();
			result.append(valueConverter.asFieldValue(value.getValue()));
			for (Entry<String, String> e: value.getParameters().entrySet()) {
				result.append("; ");
				result.append(e.getKey());
				result.append('=');
				result.append(paramValueConverter.asFieldValue(e.getValue()));
			}
			return result.toString();
		}

		public P fromFieldValue(String text) throws ParseException {
			ListItemizer li = new ListItemizer(text, ";");
			String valueRepr = li.nextItem();
			if (valueRepr == null) {
				throw new ParseException("Value may not be empty", 0);
			}
			B value = valueConverter.fromFieldValue(valueRepr);
			Map<String,String> params = new HashMap<>();
			while (true) {
				String param = li.nextItem();
				if (param == null) {
					break;
				}
				ListItemizer pi = new ListItemizer(param, "=");
				String paramKey = pi.nextItem().trim().toLowerCase();
				if (paramKey == null) {
					throw new ParseException("parameter may not be empty", 0);
				}
				String paramValue = pi.nextItem();
				if (paramValue != null) {
					paramValue = paramValueConverter.fromFieldValue(paramValue);
				}
				params.put(paramKey, paramValue);
			}
			return resultConstructor.apply(value, params);
		}
	}

	/**
	 * Extends {@link ParamValueConverterBase} to a realization
	 * of `Converter<ParameterizedValue<T>>`.
	 * 
	 * @param <T> the base value type
	 */
	public static class ParameterizedValueConverter<T> 
		extends ParamValueConverterBase<ParameterizedValue<T>, T> {

		public ParameterizedValueConverter(Converter<T> valueConverter) {
			super(valueConverter, ParameterizedValue<T>::new);
		}
		
	}
}