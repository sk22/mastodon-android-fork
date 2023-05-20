package org.joinmastodon.android.model;

import android.view.Menu;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.R;

public enum ContentType {
	@SerializedName("text/plain")
	PLAIN,
	@SerializedName("text/html")
	HTML,
	@SerializedName("text/markdown")
	MARKDOWN,
	@SerializedName("text/bbcode")
	BBCODE, // akkoma
	@SerializedName("text/x.misskeymarkdown")
	MISSKEY_MARKDOWN; // akkoma/*key

	public static int getContentTypeRes(@Nullable ContentType contentType) {
		return contentType == null ? R.id.content_type_null : switch(contentType) {
			case PLAIN -> R.id.content_type_plain;
			case HTML -> R.id.content_type_html;
			case MARKDOWN -> R.id.content_type_markdown;
			case BBCODE -> R.id.content_type_bbcode;
			case MISSKEY_MARKDOWN -> R.id.content_type_misskey_markdown;
		};
	}

	public static void adaptMenuToInstance(Menu m, Instance i) {
		if (i.pleroma == null) {
			// memo: change this if glitch or another mastodon fork supports bbcode or mfm
			m.findItem(R.id.content_type_bbcode).setVisible(false);
			m.findItem(R.id.content_type_misskey_markdown).setVisible(false);
		}
	}
}
