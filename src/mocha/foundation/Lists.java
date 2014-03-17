/**
 *  @author Shaun
 *  @date 3/15/14
 *  @copyright 2014 TV Guide, Inc. All rights reserved.
 */
package mocha.foundation;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class Lists {

	private Lists() { }

	public static <T> List<T> create(T... items) {
		List<T> list = new ArrayList<T>();
		Collections.addAll(list, items);
		return list;
	}

	public static <T> T last(List<T> list) {
		return last(list, null);
	}

	public static <T> T last(List<T> list, T defaultItemIfNull) {
		if(list == null) {
			return defaultItemIfNull;
		} else {
			int size = list.size();

			if(size == 0) {
				return defaultItemIfNull;
			} else {
				return list.get(size - 1);
			}
		}
	}

	public static <T> void sort(List<T> list, SortDescriptor sortDescriptor) {
		if(list.size() < 2) return;

		Class<?> itemClass = list.get(0).getClass();
		Class<?> type;
		Object fieldOrMethod;

		try {
			fieldOrMethod = itemClass.getField(sortDescriptor.key);
			type = ((Field)fieldOrMethod).getType();
		} catch (NoSuchFieldException e) {
			String getter = "get" + sortDescriptor.key.substring(0, 1).toUpperCase() + sortDescriptor.key.substring(1);

			try {
				fieldOrMethod = itemClass.getMethod(getter);
				type = ((Method)fieldOrMethod).getReturnType();
			} catch (NoSuchMethodException e1) {
				throw new RuntimeException(e);
			}
		}

		Comparator<T> comparator = null;

		if(type != null) {
			if (type.equals(Integer.class) || type.equals(int.class)) {
				comparator = getIntComparator(fieldOrMethod);
			} else if (type.equals(Long.class) || type.equals(long.class)) {
				comparator = getLongComparator(fieldOrMethod);
			} else if (type.equals(Float.class) || type.equals(float.class)) {
				comparator = getFloatComparator(fieldOrMethod);
			} else if (type.equals(Double.class) || type.equals(double.class)) {
				comparator = getDoubleComparator(fieldOrMethod);
			} else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
				comparator = getBooleanComparator(fieldOrMethod);
			} else if (type.equals(String.class)) {
				comparator = getStringComparator(fieldOrMethod);
			}
		}

		if(comparator == null) {
			throw new RuntimeException("Type '" + type + "' is not supported.");
		}

		Collections.sort(list, comparator);

		if(!sortDescriptor.ascending) {
			Collections.reverse(list);
		}
	}

	private static <T> Comparator<T> getStringComparator(Object o) {
		return new ReflectionComparator<T, String>(o) {
			protected int compareValues(String value1, String value2) {
				if(value1 == null) value1 = "";
				if(value2 == null) value2 = "";
				return value1.compareTo(value2);
			}
		};
	}

	private static <T> Comparator<T> getLongComparator(Object o) {
		return new ReflectionComparator<T, Long>(o) {
			protected int compareValues(Long value1, Long value2) {
				if(value1 == null) value1 = 0L;
				if(value2 == null) value2 = 0L;
				return value1.compareTo(value2);
			}
		};
	}

	private static <T> Comparator<T> getIntComparator(Object o) {
		return new ReflectionComparator<T, Integer>(o) {
			protected int compareValues(Integer value1, Integer value2) {
				if(value1 == null) value1 = 0;
				if(value2 == null) value2 = 0;
				return value1.compareTo(value2);
			}
		};
	}

	private static <T> Comparator<T> getDoubleComparator(Object o) {
		return new ReflectionComparator<T, Double>(o) {
			protected int compareValues(Double value1, Double value2) {
				if(value1 == null) value1 = 0.0;
				if(value2 == null) value2 = 0.0;
				return value1.compareTo(value2);
			}
		};
	}

	private static <T> Comparator<T> getFloatComparator(Object o) {
		return new ReflectionComparator<T, Float>(o) {
			protected int compareValues(Float value1, Float value2) {
				if(value1 == null) value1 = 0.0f;
				if(value2 == null) value2 = 0.0f;
				return value1.compareTo(value2);
			}
		};
	}

	private static <T> Comparator<T> getBooleanComparator(Object o) {
		return new ReflectionComparator<T, Boolean>(o) {
			protected int compareValues(Boolean value1, Boolean value2) {
				if(value1 == null) value1 = false;
				if(value2 == null) value2 = false;
				return value1.compareTo(value2);
			}
		};
	}

	private static abstract class ReflectionComparator<T, F> implements Comparator<T> {
		Field field;
		Method method;

		protected ReflectionComparator(Object object) {
			if(object instanceof Field) {
				this.field = (Field)object;
			} else if(object instanceof Method) {
				this.method = (Method)object;
			}
		}

		@SuppressWarnings("unchecked")
		public int compare(T t, T t2) {
			F value1;

			try {
				if(method != null) {
					value1 = (F)this.method.invoke(t);
				} else {
					value1 = (F)this.field.get(t);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			F value2;

			try {
				if(this.method != null) {
					value2 = (F)this.method.invoke(t2);
				} else {
					value2 = (F)this.field.get(t2);
				}
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

			return compareValues(value1, value2);
		}

		abstract protected int compareValues(F value1, F value2);
	}

}
