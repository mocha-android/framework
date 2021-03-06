package mocha.ui;

import mocha.foundation.MObject;
import mocha.graphics.Image;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class BarItem extends MObject {
	private Image image;
	private Image landscapeImagePhone;
	private String title;
	private boolean enabled;
	private EdgeInsets imageInsets;
	private EdgeInsets landscapeImagePhoneInsets;
	private int tag;
	private Map<EnumSet<Control.State>, TextAttributes> titleTextAttributes;

	public BarItem() {
		this.enabled = true;
		this.imageInsets = EdgeInsets.zero();
		this.landscapeImagePhoneInsets = EdgeInsets.zero();
		this.titleTextAttributes = new HashMap<>();
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public Image getLandscapeImagePhone() {
		return landscapeImagePhone;
	}

	public void setLandscapeImagePhone(Image landscapeImagePhone) {
		this.landscapeImagePhone = landscapeImagePhone;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public EdgeInsets getImageInsets() {
		return imageInsets;
	}

	public void setImageInsets(EdgeInsets imageInsets) {
		this.imageInsets = imageInsets;
	}

	public EdgeInsets getLandscapeImagePhoneInsets() {
		return landscapeImagePhoneInsets;
	}

	public void setLandscapeImagePhoneInsets(EdgeInsets landscapeImagePhoneInsets) {
		this.landscapeImagePhoneInsets = landscapeImagePhoneInsets;
	}

	public void setTitleTextAttributes(TextAttributes textAttributes, Control.State... state) {
		EnumSet<Control.State> stateSet = Control.getStateSet(state);

		if (textAttributes == null) {
			this.titleTextAttributes.remove(stateSet);
		} else {
			this.titleTextAttributes.put(stateSet, textAttributes);
		}
	}

	public TextAttributes getTitleTextAttributesForState(Control.State... state) {
		return this.titleTextAttributes.get(Control.getStateSet(state));
	}

	Map<EnumSet<Control.State>, TextAttributes> getTitleTextAttributes() {
		return Collections.unmodifiableMap(this.titleTextAttributes);
	}

	public int getTag() {
		return tag;
	}

	public void setTag(int tag) {
		this.tag = tag;
	}

}
