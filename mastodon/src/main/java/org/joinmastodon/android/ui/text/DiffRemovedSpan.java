package org.joinmastodon.android.ui.text;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

public class DiffRemovedSpan extends CharacterStyle {

	private final String text;
	private final int color;

	public DiffRemovedSpan(String text, int color){
		this.text=text;
		this.color=color;
	}


	@Override
	public void updateDrawState(TextPaint tp) {
		tp.setStrikeThruText(true);
		tp.setColor(color);
	}
	
	public String getText() {
		return text;
	}
}
