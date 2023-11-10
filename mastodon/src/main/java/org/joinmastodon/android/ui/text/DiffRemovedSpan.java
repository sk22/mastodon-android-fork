package org.joinmastodon.android.ui.text;

import android.text.TextPaint;
import android.text.style.CharacterStyle;

import org.joinmastodon.android.ui.utils.UiUtils;

public class DiffRemovedSpan extends CharacterStyle {

	private final String text;

	public DiffRemovedSpan(String text){
		this.text=text;
	}


	@Override
	public void updateDrawState(TextPaint tp) {
		tp.setStrikeThruText(true);
		tp.setColor(0xFFCA5B63);
	}
	
	public String getText() {
		return text;
	}
}
