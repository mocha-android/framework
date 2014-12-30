/**
 *  @author Shaun
 *  @date 4/13/13
 *  @copyright 2013 Mocha. All rights reserved.
 */
package mocha.ui;

import mocha.graphics.Context;
import mocha.graphics.Image;
import mocha.graphics.Rect;
import mocha.graphics.Size;

public class PageControl extends Control {

	private int numberOfPages;
	private int currentPage;
	private boolean hidesForSinglePage;
	private Size pageDotSize;
	private float pageGapWidth;

	private Image indicatorImage;
	private Image indicatorCurrentImage;

	public PageControl() { super(); }
	public PageControl(Rect frame) { super(frame); }

	protected void onCreate(Rect frame) {
		this.indicatorCurrentImage = Image.imageNamed(R.drawable.mocha_page_indicator_current);
		this.indicatorImage = Image.imageNamed(R.drawable.mocha_page_indicator);

		this.pageGapWidth = 5.0f;
		this.pageDotSize = new Size(10.0f, 20.0f);

		this.hidesForSinglePage = false;

		super.onCreate(frame);
	}

	public void draw(Context context, Rect rect) {
		if (this.hidesForSinglePage && (this.numberOfPages == 1)) return;

		float offsetX = 0.0f;

		Size size = this.getSizeForNumberOfPages(this.getNumberOfPages());
		float gap = this.getBounds().size.width - size.width;
		offsetX += floorf(gap / 2.0f);

		for (int i = 0; i < this.getNumberOfPages(); i++) {

			Image image = i == this.currentPage ? this.indicatorCurrentImage : this.indicatorImage;
			Size imageSize = image.getSize();

			Rect indicatorRect = new Rect(floorf(offsetX + ((this.pageDotSize.width / 2.0f) - (imageSize.width / 2.0f))), floorf(((this.getBounds().size.height / 2.0f) - (imageSize.height / 2.0f))), imageSize.width, imageSize.height);
			image.draw(context, indicatorRect);

			offsetX += (this.pageDotSize.width) + this.pageGapWidth;
		}
	}

	public Size getSizeForNumberOfPages(int num) {
		return new Size(((this.pageDotSize.width * num) + (this.pageGapWidth * (num-1))), this.pageDotSize.height);
	}

	public int getNumberOfPages() {
		return this.numberOfPages;
	}

	public void setNumberOfPages(int numberOfPages) {
		if(this.numberOfPages != numberOfPages) {
			this.numberOfPages = numberOfPages;
			this.setNeedsDisplay();
		}
	}

	public int getCurrentPage() {
		return this.currentPage;
	}

	public void setCurrentPage(int page) {
		if(page != this.currentPage) {
			page = Math.max(0, page);

			if (page > this.numberOfPages) {
				page = this.numberOfPages;
			}

			this.currentPage = page;
			this.setNeedsDisplay();
		}
	}

	public boolean hidesForSinglePage() {
		return this.hidesForSinglePage;
	}

	public void setHidesForSinglePage(boolean hidesForSinglePage) {
		if(this.hidesForSinglePage != hidesForSinglePage) {
			this.hidesForSinglePage = hidesForSinglePage;
			this.setNeedsDisplay();
		}
	}

}
