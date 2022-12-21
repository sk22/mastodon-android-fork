package org.joinmastodon.android;

import static org.joinmastodon.android.api.MastodonAPIController.gson;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalUserPreferences{
	public static boolean playGifs;
	public static boolean useCustomTabs;
	public static boolean trueBlackTheme;
	public static boolean showReplies;
	public static boolean showBoosts;
	public static boolean loadNewPosts;
	public static boolean showFederatedTimeline;
	public static boolean showInteractionCounts;
	public static boolean alwaysExpandContentWarnings;
	public static boolean disableMarquee;
	public static boolean disableSwipe;
	public static boolean voteButtonForSingleChoice;
	public static boolean enableDeleteNotifications;
	public static boolean translateButtonOpenedOnly;
	public static String publishButtonText;
	public static ThemePreference theme;
	public static ColorPreference color;

	private final static Type recentLanguagesType = new TypeToken<Map<String, List<String>>>() {}.getType();
	public static Map<String, List<String>> recentLanguages;

	private static SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("global", Context.MODE_PRIVATE);
	}

	private static <T> T fromJson(String json, Type type, T orElse) {
		try { return gson.fromJson(json, type); }
		catch (JsonSyntaxException ignored) { return orElse; }
	}

	public static void load(){
		SharedPreferences prefs=getPrefs();
		playGifs=prefs.getBoolean("playGifs", true);
		useCustomTabs=prefs.getBoolean("useCustomTabs", true);
		trueBlackTheme=prefs.getBoolean("trueBlackTheme", false);
		showReplies=prefs.getBoolean("showReplies", true);
		showBoosts=prefs.getBoolean("showBoosts", true);
		loadNewPosts=prefs.getBoolean("loadNewPosts", true);
		showFederatedTimeline=prefs.getBoolean("showFederatedTimeline", !BuildConfig.BUILD_TYPE.equals("playRelease"));
		showInteractionCounts=prefs.getBoolean("showInteractionCounts", false);
		alwaysExpandContentWarnings=prefs.getBoolean("alwaysExpandContentWarnings", false);
		disableMarquee=prefs.getBoolean("disableMarquee", false);
		disableSwipe=prefs.getBoolean("disableSwipe", false);
		voteButtonForSingleChoice=prefs.getBoolean("voteButtonForSingleChoice", true);
		enableDeleteNotifications=prefs.getBoolean("enableDeleteNotifications", false);
		translateButtonOpenedOnly=prefs.getBoolean("translateButtonOpenedOnly", false);
		publishButtonText=prefs.getString("publishButtonText", "");
		theme=ThemePreference.values()[prefs.getInt("theme", 0)];
		recentLanguages=fromJson(prefs.getString("recentLanguages", "{}"), recentLanguagesType, new HashMap<>());

		try {
			color=ColorPreference.valueOf(prefs.getString("color", ColorPreference.PINK.name()));
		} catch (IllegalArgumentException|ClassCastException ignored) {
			// invalid color name or color was previously saved as integer
			color=ColorPreference.PINK;
		}
	}

	public static void save(){
		getPrefs().edit()
				.putBoolean("playGifs", playGifs)
				.putBoolean("useCustomTabs", useCustomTabs)
				.putBoolean("showReplies", showReplies)
				.putBoolean("showBoosts", showBoosts)
				.putBoolean("loadNewPosts", loadNewPosts)
				.putBoolean("showFederatedTimeline", showFederatedTimeline)
				.putBoolean("trueBlackTheme", trueBlackTheme)
				.putBoolean("showInteractionCounts", showInteractionCounts)
				.putBoolean("alwaysExpandContentWarnings", alwaysExpandContentWarnings)
				.putBoolean("disableMarquee", disableMarquee)
				.putBoolean("disableSwipe", disableSwipe)
				.putBoolean("enableDeleteNotifications", enableDeleteNotifications)
				.putBoolean("translateButtonOpenedOnly", translateButtonOpenedOnly)
				.putString("publishButtonText", publishButtonText)
				.putInt("theme", theme.ordinal())
				.putString("color", color.name())
				.putString("recentLanguages", gson.toJson(recentLanguages))
				.apply();
	}

	public enum ColorPreference{
		MATERIAL3,
		PINK,
		PURPLE,
		GREEN,
		BLUE,
		BROWN,
		RED,
		YELLOW
	}

	public enum ThemePreference{
		AUTO,
		LIGHT,
		DARK
	}
}

