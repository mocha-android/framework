/**
 *  @author Shaun
 *  @date 3/10/15
 *  @copyright 2015 Mocha. All rights reserved.
 */
package mocha.foundation;

import android.content.SharedPreferences;
import mocha.ui.Application;

import java.util.Date;

public class UserDefaults {
	private static UserDefaults standardUserDefaults = new UserDefaults("UserDefaults");
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;

	public static UserDefaults standardUserDefaults() {
		return standardUserDefaults;
	}

	public UserDefaults(String name) {
		this.sharedPreferences = Application.sharedApplication().getContext().getSharedPreferences(name, 0);
	}

	public boolean contains(String name) {
		return this.sharedPreferences.contains(name);
	}

	public void put(String name, Object value) {
		if (value instanceof String) {
			this.putString(name, (String)value);
		} else if (value instanceof Date) {
			this.putDate(name, (Date)value);
		} else if (value instanceof Integer) {
			this.putInt(name, (Integer) value);
		} else if (value instanceof Long) {
			this.putLong(name, (Long) value);
		} else if (value instanceof Float) {
			this.putFloat(name, (Float) value);
		} else if (value instanceof Boolean) {
			this.putBoolean(name, (Boolean) value);
		} else {
			throw new RuntimeException("Tried to store unsupported content type: " + value.getClass() + ", " + value);
		}
	}

	public void putString(String name, String value) {
		this.getEditor().putString(name, value);
	}

	public String getString(String name, String defaultValue) {
		return this.sharedPreferences.getString(name, defaultValue);
	}

	public String optString(String name) {
		return this.getString(name, null);
	}

	public void putBoolean(String name, boolean value) {
		this.getEditor().putBoolean(name, value);
	}

	public boolean getBoolean(String name, boolean defaultValue) {
		return this.sharedPreferences.getBoolean(name, defaultValue);
	}

	public boolean optBoolean(String name, boolean defaultValue) {
		return this.getBoolean(name, false);
	}

	public void putInt(String name, int value) {
		this.getEditor().putInt(name, value);
	}

	public int getInt(String name, int defaultValue) {
		return this.sharedPreferences.getInt(name, defaultValue);
	}

	public int optInt(String name) {
		return this.getInt(name, 0);
	}

	public void putLong(String name, long value) {
		this.getEditor().putLong(name, value);
	}

	public long getLong(String name, long defaultValue) {
		return this.sharedPreferences.getLong(name, defaultValue);
	}

	public long optLong(String name) {
		return this.getLong(name, 0L);
	}

	public void putFloat(String name, float value) {
		this.getEditor().putFloat(name, value);
	}

	public float getFloat(String name, float defaultValue) {
		return this.sharedPreferences.getFloat(name, defaultValue);
	}

	public float optFloat(String name) {
		return this.getFloat(name, 0.0f);
	}

	public void putDate(String name, Date value) {
		putLong(name, value.getTime());
	}

	public Date getDate(String name, Date defaultValue) {
		long time = getLong(name, -1);
		if(time == -1) {
			if(defaultValue != null) {
				return defaultValue;
			} else {
				return null;
			}
		} else {
			return new Date(time);
		}
	}

	public Date optDate(String name) {
		return this.getDate(name, null);
	}

	public void remove(String name) {
		this.getEditor().remove(name);
	}

	private SharedPreferences.Editor getEditor() {
		if(this.editor == null) {
			this.editor = this.sharedPreferences.edit();
		}

		return this.editor;
	}

	public void save() {
		if(this.editor != null) {
			this.editor.apply();
			this.editor = null;
		}
	}

}
