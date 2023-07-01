package org.joinmastodon.android.api.session;

import static org.joinmastodon.android.GlobalUserPreferences.fromJson;
import static org.joinmastodon.android.GlobalUserPreferences.enumValue;

import android.content.SharedPreferences;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.model.ContentType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AccountLocalPreferences{
	private final SharedPreferences prefs;

	public boolean showInteractionCounts;
	public boolean customEmojiInNames;
	public boolean showCWs;
	public boolean hideSensitiveMedia;
	public boolean serverSideFiltersSupported;

	// MEGALODON
	public List<String> recentLanguages;
	public boolean bottomEncoding;
	public ContentType defaultContentType;

	private final static Type recentLanguagesType = new TypeToken<List<String>>() {}.getType();

	public AccountLocalPreferences(SharedPreferences prefs){
		this.prefs=prefs;
		showInteractionCounts=prefs.getBoolean("interactionCounts", true);
		customEmojiInNames=prefs.getBoolean("emojiInNames", true);
		showCWs=prefs.getBoolean("showCWs", true);
		hideSensitiveMedia=prefs.getBoolean("hideSensitive", true);
		serverSideFiltersSupported=prefs.getBoolean("serverSideFilters", false);

		// MEGALODON
		recentLanguages=fromJson(prefs.getString("recentLanguages", null), recentLanguagesType, new ArrayList<>());
		bottomEncoding=prefs.getBoolean("bottomEncoding", false);
		defaultContentType=enumValue(ContentType.class, prefs.getString("defaultContentType", null));
	}

	public long getNotificationsPauseEndTime(){
		return prefs.getLong("notificationsPauseTime", 0L);
	}

	public void setNotificationsPauseEndTime(long time){
		prefs.edit().putLong("notificationsPauseTime", time).apply();
	}

	public void save(){
		prefs.edit()
				.putBoolean("interactionCounts", showInteractionCounts)
				.putBoolean("emojiInNames", customEmojiInNames)
				.putBoolean("showCWs", showCWs)
				.putBoolean("hideSensitive", hideSensitiveMedia)
				.putBoolean("serverSideFilters", serverSideFiltersSupported)
				// MEGALODON
				// todo: put recent languages, ...
				.apply();
	}
}
