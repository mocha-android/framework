package mocha.foundation;

import android.content.SharedPreferences;
import mocha.ui.Application;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserDefaults {
	private static UserDefaults standardUserDefaults = new UserDefaults("UserDefaults");
	private SharedPreferences sharedPreferences;
	private SharedPreferences.Editor editor;
	private Map<String, Object> defaults;

	public static UserDefaults standardUserDefaults() {
		return standardUserDefaults;
	}

	public UserDefaults(String name) {
		this.sharedPreferences = Application.sharedApplication().getContext().getSharedPreferences(name, 0);
	}

	public void registerDefaults(Map<String, Object> defaults) {
		if (this.defaults == null) {
			this.defaults = new HashMap<>();
		}

		this.defaults.putAll(defaults);
	}

	public boolean contains(String name) {
		return this.sharedPreferences.contains(name) || (this.defaults != null && this.defaults.containsKey(name));
	}

	public void put(String name, Object value) {
		if (value instanceof String) {
			this.putString(name, (String) value);
		} else if (value instanceof Date) {
			this.putDate(name, (Date) value);
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
		if (!this.sharedPreferences.contains(name) && (this.defaults != null && this.defaults.containsKey(name))) {
			return (String) this.defaults.get(name);
		} else {
			return this.sharedPreferences.getString(name, defaultValue);
		}
	}

	public String optString(String name) {
		return this.getString(name, null);
	}

	public void putBoolean(String name, boolean value) {
		this.getEditor().putBoolean(name, value);
	}

	public boolean getBoolean(String name, boolean defaultValue) {
		if (!this.sharedPreferences.contains(name) && (this.defaults != null && this.defaults.containsKey(name))) {
			return (Boolean) this.defaults.get(name);
		} else {
			return this.sharedPreferences.getBoolean(name, defaultValue);
		}
	}

	public boolean optBoolean(String name) {
		return this.getBoolean(name, false);
	}

	public void putInt(String name, int value) {
		this.getEditor().putInt(name, value);
	}

	public int getInt(String name, int defaultValue) {
		if (!this.sharedPreferences.contains(name) && (this.defaults != null && this.defaults.containsKey(name))) {
			return (Integer) this.defaults.get(name);
		} else {
			return this.sharedPreferences.getInt(name, defaultValue);
		}
	}

	public int optInt(String name) {
		return this.getInt(name, 0);
	}

	public void putLong(String name, long value) {
		this.getEditor().putLong(name, value);
	}

	public long getLong(String name, long defaultValue) {
		if (!this.sharedPreferences.contains(name) && (this.defaults != null && this.defaults.containsKey(name))) {
			return (Long) this.defaults.get(name);
		} else {
			return this.sharedPreferences.getLong(name, defaultValue);
		}
	}

	public long optLong(String name) {
		return this.getLong(name, 0L);
	}

	public void putFloat(String name, float value) {
		this.getEditor().putFloat(name, value);
	}

	public float getFloat(String name, float defaultValue) {
		if (!this.sharedPreferences.contains(name) && (this.defaults != null && this.defaults.containsKey(name))) {
			return (Float) this.defaults.get(name);
		} else {
			return this.sharedPreferences.getFloat(name, defaultValue);
		}
	}

	public float optFloat(String name) {
		return this.getFloat(name, 0.0f);
	}

	public void putDate(String name, Date value) {
		putLong(name, value.getTime());
	}

	public Date getDate(String name, Date defaultValue) {
		if (!this.sharedPreferences.contains(name) && (this.defaults != null && this.defaults.containsKey(name))) {
			return (Date) this.defaults.get(name);
		} else {
			long time = getLong(name, -1);
			if (time == -1) {
				if (defaultValue != null) {
					return defaultValue;
				} else {
					return null;
				}
			} else {
				return new Date(time);
			}
		}
	}

	public Date optDate(String name) {
		return this.getDate(name, null);
	}

	public void remove(String name) {
		this.getEditor().remove(name);
	}

	private SharedPreferences.Editor getEditor() {
		if (this.editor == null) {
			this.editor = this.sharedPreferences.edit();
		}

		return this.editor;
	}

	public void save() {
		if (this.editor != null) {
			this.editor.apply();
			this.editor = null;
		}
	}

}
