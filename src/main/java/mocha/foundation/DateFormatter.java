/*
 *  @author Shaun
 *	@date 3/10/15
 *	@copyright	2015 Mocha. All rights reserved.
 */
package mocha.foundation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateFormatter extends MObject {

	public enum Style {
		NONE,
		SHORT,
		MEDIUM,
		LONG,
		FULL
	}

	private String dateFormat;
	private Style dateStyle;
	private Style timeStyle;
	private SimpleDateFormat formatter;

	public DateFormatter() {
		this.dateStyle = Style.SHORT;
		this.timeStyle = Style.SHORT;
	}

	private SimpleDateFormat getFormatter() {
		if(this.formatter == null) {
			String format = this.dateFormat;

			if(format == null) {
				if(this.dateStyle == null) {
					this.dateStyle = Style.NONE;
				}

				switch (this.dateStyle) {
					case NONE:
						format = "";
						break;
					case SHORT:
						format = "M/d/yy";
						break;
					case MEDIUM:
						format = "MMM d, yyyy";
						break;
					case LONG:
						format = "MMMM d, yyyy";
						break;
					case FULL:
						format = "EEEE, MMMM d, yyyy";
						break;
				}

				if(this.timeStyle == null) {
					this.timeStyle = Style.NONE;
				}

				switch (this.timeStyle) {
					case NONE:
						break;
					case SHORT:
						format += " h:mm a";
						break;
					case MEDIUM:
						format += " h:mm:ss a";
						break;
					case LONG:
						format += " h:mm:ss a zzz";
						break;
					case FULL:
						format += " h:mm:ss a zzzz";
						break;
				}

				format = format.trim();
			}

			this.formatter = new SimpleDateFormat(format);
		}

		return this.formatter;
	}

	public String stringFromDate(Date date) {
		return this.getFormatter().format(date);
	}

	public Date dateFromString(String string) {
		try {
			return this.getFormatter().parse(string);
		} catch (ParseException e) {
			MLogException(e, "Could not parse string: " + string);
			return null;
		}
	}

	public String getDateFormat() {
		return this.dateFormat;
	}

	public void setDateFormat(String format) {
		this.dateFormat = format;
		this.formatter = null;
	}

	public Style getDateStyle() {
		return this.dateStyle;
	}

	public void setDateStyle(Style dateStyle) {
		this.dateStyle = dateStyle;
		this.formatter = null;
	}

	public Style getTimeStyle() {
		return this.timeStyle;
	}

	public void setTimeStyle(Style timeStyle) {
		this.timeStyle = timeStyle;
		this.formatter = null;
	}
}
