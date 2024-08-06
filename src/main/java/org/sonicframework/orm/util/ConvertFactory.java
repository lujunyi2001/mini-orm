package org.sonicframework.orm.util;

import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
* @author lujunyi
*/
public class ConvertFactory {

	public final static String[] DATE_FORMAT_ALL = {"yyyy-MM-dd", "yyyy/MM/dd", "yyyyMMdd", 
			"yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyyMMdd", "yyyyMMddHHmmss", 
			"yyyy-MM-dd HH:mm:ss.SSS", "yyyy/MM/dd HH:mm:ss.SSS", "yyyyMMddHHmmssSSS", 
			"yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy/MM/dd'T'HH:mm:ss.SSS'Z'"};
	@SuppressWarnings("unchecked")
	public static<T> T convertToObject(String value, Class<T> clazz) {
		if(value == null || "".equals(value)) {
			return null;
		}
		T result = null;
		try {
			if(Date.class.isAssignableFrom(clazz)) {
				result = (T) parseDateStrictly(value, DATE_FORMAT_ALL);
				if(result != null) {
					if(clazz == java.sql.Date.class) {
						result = (T) new java.sql.Date(((Date)result).getTime());
					}else if(clazz == java.sql.Timestamp.class) {
						result = (T) new java.sql.Timestamp(((Date)result).getTime());
					}
				}
			}else if(String.class == clazz) {
				result = (T) value;
			}else if(Integer.class == clazz || int.class == clazz) {
				result = (T) new Integer(value);
			}else if(Long.class == clazz || long.class == clazz) {
				result = (T) new Long(value);
			}else if(Double.class == clazz || double.class == clazz) {
				result = (T) new Double(value);
			}else if(Float.class == clazz || float.class == clazz) {
				result = (T) new Float(value);
			}else if(Boolean.class == clazz || boolean.class == clazz) {
				result = (T) new Boolean(value);
			}
		} catch (Exception e) {
		}
		
		return result;
	}
	
	
	public static Date parseDateStrictly(final String str, final String... parsePatterns) throws ParseException {
        return parseDateWithLeniency(str, null, parsePatterns, false);
    }
	
	private static Date parseDateWithLeniency(
            final String str, final Locale locale, final String[] parsePatterns, final boolean lenient) throws ParseException {
        if (str == null || parsePatterns == null) {
            throw new IllegalArgumentException("Date and Patterns must not be null");
        }
        
        SimpleDateFormat parser;
        if (locale == null) {
            parser = new SimpleDateFormat();
        } else {
            parser = new SimpleDateFormat("", locale);
        }
        
        parser.setLenient(lenient);
        final ParsePosition pos = new ParsePosition(0);
        for (final String parsePattern : parsePatterns) {

            String pattern = parsePattern;

            // LANG-530 - need to make sure 'ZZ' output doesn't get passed to SimpleDateFormat
            if (parsePattern.endsWith("ZZ")) {
                pattern = pattern.substring(0, pattern.length() - 1);
            }
            
            parser.applyPattern(pattern);
            pos.setIndex(0);

            String str2 = str;
            // LANG-530 - need to make sure 'ZZ' output doesn't hit SimpleDateFormat as it will ParseException
            if (parsePattern.endsWith("ZZ")) {
                str2 = str.replaceAll("([-+][0-9][0-9]):([0-9][0-9])$", "$1$2"); 
            }

            final Date date = parser.parse(str2, pos);
            if (date != null && pos.getIndex() == str2.length()) {
                return date;
            }
        }
        if(str.chars().allMatch(Character::isDigit)) {
        	return new Date(new Long(str));
        }
        throw new ParseException("Unable to parse the date: " + str, -1);
    }
}
