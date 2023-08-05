package org.joinmastodon.android.api.session;

import static org.joinmastodon.android.GlobalUserPreferences.fromJson;
import static org.joinmastodon.android.GlobalUserPreferences.enumValue;
import static org.joinmastodon.android.api.MastodonAPIController.gson;

import android.content.SharedPreferences;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.model.ContentType;
import org.joinmastodon.android.model.TimelineDefinition;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AccountLocalPreferences{
	private final SharedPreferences prefs;

	public boolean showInteractionCounts;
	public boolean customEmojiInNames;
	public boolean revealCWs;
	public boolean hideSensitiveMedia;
	public boolean serverSideFiltersSupported;

	// MEGALODON
	public boolean showReplies;
	public boolean showBoosts;
	public ArrayList<String> recentLanguages;
	public boolean bottomEncoding;
	public ContentType defaultContentType;
	public boolean contentTypesEnabled;
	public ArrayList<TimelineDefinition> timelines;
	public boolean localOnlySupported;
	public boolean glitchInstance;
	public String publishButtonText;
	public String timelineReplyVisibility; // akkoma-only
	public boolean keepOnlyLatestNotification;

	public boolean emojiReactionsEnabled;
	public boolean showEmojiReactionsInLists;

	private final static Type recentLanguagesType = new TypeToken<ArrayList<String>>() {}.getType();
	private final static Type timelinesType = new TypeToken<ArrayList<TimelineDefinition>>() {}.getType();

	public AccountLocalPreferences(SharedPreferences prefs, AccountSession session){
		this.prefs=prefs;
		showInteractionCounts=prefs.getBoolean("interactionCounts", false);
		customEmojiInNames=prefs.getBoolean("emojiInNames", true);
		revealCWs=prefs.getBoolean("revealCWs", false);
		hideSensitiveMedia=prefs.getBoolean("hideSensitive", true);
		serverSideFiltersSupported=prefs.getBoolean("serverSideFilters", false);

		// MEGALODON
		showReplies=prefs.getBoolean("showReplies", true);
		showBoosts=prefs.getBoolean("showBoosts", true);
		recentLanguages=fromJson(prefs.getString("recentLanguages", null), recentLanguagesType, new ArrayList<>());
		bottomEncoding=prefs.getBoolean("bottomEncoding", false);
		defaultContentType=enumValue(ContentType.class, prefs.getString("defaultContentType", ContentType.PLAIN.name()));
		contentTypesEnabled=prefs.getBoolean("contentTypesEnabled", true);
		timelines=fromJson(prefs.getString("timelines", null), timelinesType, TimelineDefinition.getDefaultTimelines(session.getID()));
		localOnlySupported=prefs.getBoolean("localOnlySupported", false);
		glitchInstance=prefs.getBoolean("glitchInstance", false);
		publishButtonText=prefs.getString("publishButtonText", null);
		timelineReplyVisibility=prefs.getString("timelineReplyVisibility", null);
		keepOnlyLatestNotification=prefs.getBoolean("keepOnlyLatestNotification", false);
		emojiReactionsEnabled=prefs.getBoolean("emojiReactionsEnabled", session.getInstance().isPresent() && session.getInstance().get().isAkkoma());
		showEmojiReactionsInLists=prefs.getBoolean("showEmojiReactionsInLists", false);
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
				.putBoolean("revealCWs", revealCWs)
				.putBoolean("hideSensitive", hideSensitiveMedia)
				.putBoolean("serverSideFilters", serverSideFiltersSupported)

				// MEGALODON
				.putBoolean("showReplies", showReplies)
				.putBoolean("showBoosts", showBoosts)
				.putString("recentLanguages", gson.toJson(recentLanguages))
				.putBoolean("bottomEncoding", bottomEncoding)
				.putString("defaultContentType", defaultContentType==null ? null : defaultContentType.name())
				.putBoolean("contentTypesEnabled", contentTypesEnabled)
				.putString("timelines", gson.toJson(timelines))
				.putBoolean("localOnlySupported", localOnlySupported)
				.putBoolean("glitchInstance", glitchInstance)
				.putString("publishButtonText", publishButtonText)
				.putString("timelineReplyVisibility", timelineReplyVisibility)
				.putBoolean("keepOnlyLatestNotification", keepOnlyLatestNotification)
				.putBoolean("emojiReactionsEnabled", emojiReactionsEnabled)
				.putBoolean("showEmojiReactionsInLists", showEmojiReactionsInLists)
				.apply();
	}
}
