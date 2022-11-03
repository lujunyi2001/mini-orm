package org.sonicframework.orm.util;

import java.text.NumberFormat;
import java.util.Iterator;

/**
* @author lujunyi
*/
public class LocalStringUtil {

	private final static String EMPTY = "";
	private LocalStringUtil() {
	}
	
	public static boolean isEmpty(String str) {
		return str == null || EMPTY.equals(str);
	}
	
	public static String join(final Object[] array, String separator) {
		if (array == null) {
			return null;
		}
		if (separator == null) {
			separator = EMPTY;
		}
		int length = array.length;
		final StringBuilder buf = new StringBuilder(length * 16);
		
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				buf.append(separator);
			}
			if (array[i] != null) {
				buf.append(array[i]);
			}
		}
		return buf.toString();
	}
	
	public static String join(final Iterable<?> iterable, final String separator) {
        if (iterable == null) {
            return null;
        }
        return join(iterable.iterator(), separator);
    }
	
	public static String join(final Iterator<?> iterator, final String separator) {

        // handle null, zero and one elements before building a buffer
        if (iterator == null) {
            return null;
        }
        if (!iterator.hasNext()) {
            return EMPTY;
        }
        final Object first = iterator.next();
        if (!iterator.hasNext()) {
            final String result = toString(first);
            return result;
        }

        // two or more elements
        final StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
        if (first != null) {
            buf.append(first);
        }

        while (iterator.hasNext()) {
            if (separator != null) {
                buf.append(separator);
            }
            final Object obj = iterator.next();
            if (obj != null) {
                buf.append(obj);
            }
        }
        return buf.toString();
    }
	
	public static boolean isNumberic(String str, int digitNum) {
		if(isEmpty(str)) {
			return false;
		}
		String[] split = str.split("\\.");
		if(digitNum <= 0) {
			return split.length == 1 && isInt(split[0], false, -1);
		}else {
			boolean valid = split.length <= 2;
			valid &= isInt(split[0], false, -1);
			if(split.length > 1) {
				valid &= isInt(split[1], true, digitNum);
			}
			return valid;
		}
		
	}
	
	private static boolean isInt(String str, boolean rightPad, int length) {
		boolean isNumber = str.chars().allMatch(Character::isDigit);
		if(isNumber && length <= 0) {
			return true;
		}else {
			return str.length() <= length;
		}
	}

	
	public static String toString(final Object obj) {
		if(obj == null) {
			return EMPTY;
		}
		if(obj instanceof Number) {
			NumberFormat nf = NumberFormat.getNumberInstance();
			nf.setGroupingUsed(false);
			nf.setMaximumFractionDigits(20);
			return nf.format(obj);
		}
        return obj.toString();
    }
	

}
