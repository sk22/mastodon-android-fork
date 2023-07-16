package org.joinmastodon.android.fragments.settings;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.StringRes;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusDisplaySettingsChangedEvent;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.views.TextInputFrameLayout;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import me.grishka.appkit.FragmentStackActivity;

public class SettingsDisplayFragment extends BaseSettingsFragment<Void>{
	private ImageView themeTransitionWindowView;
	private ListItem<Void> themeItem;
	private CheckableListItem<Void> revealCWsItem, hideSensitiveMediaItem, interactionCountsItem, emojiInNamesItem;

	// MEGALODON
	private CheckableListItem<Void> trueBlackModeItem, marqueeItem, disableSwipeItem, reduceMotionItem, altIndicatorItem, noAltIndicatorItem, collapsePostsItem, spectatorModeItem, hideFabItem, translateOpenedItem, disablePillItem;
	private ListItem<Void> colorItem, publishTextItem, autoRevealCWsItem;

	private AccountLocalPreferences lp;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_display);
		AccountSession s=AccountSessionManager.get(accountID);
		lp=s.getLocalPreferences();
		onDataLoaded(List.of(
				themeItem=new ListItem<>(R.string.settings_theme, getAppearanceValue(), R.drawable.ic_fluent_weather_moon_24_regular, this::onAppearanceClick),
				colorItem=new ListItem<>(R.string.sk_settings_color_palette, getColorPaletteValue(), R.drawable.ic_fluent_color_24_regular, this::onColorClick),
				trueBlackModeItem=new CheckableListItem<>(R.string.sk_settings_true_black, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.trueBlackTheme, R.drawable.ic_fluent_dark_theme_24_regular, this::onTrueBlackModeClick, true),
				publishTextItem=new ListItem<>(getString(R.string.sk_settings_publish_button_text), getPublishButtonText(), R.drawable.ic_fluent_send_24_regular, this::onPublishTextClick),
				autoRevealCWsItem=new ListItem<>(R.string.sk_settings_auto_reveal_equal_spoilers, getAutoRevealSpoilersText(), R.drawable.ic_fluent_eye_24_regular, this::onAutoRevealSpoilersClick),
				revealCWsItem=new CheckableListItem<>(R.string.sk_settings_always_reveal_content_warnings, 0, CheckableListItem.Style.SWITCH, lp.revealCWs, R.drawable.ic_fluent_chat_warning_24_regular, ()->toggleCheckableItem(revealCWsItem)),
				hideSensitiveMediaItem=new CheckableListItem<>(R.string.settings_hide_sensitive_media, 0, CheckableListItem.Style.SWITCH, lp.hideSensitiveMedia, R.drawable.ic_fluent_flag_24_regular, ()->toggleCheckableItem(hideSensitiveMediaItem)),
				interactionCountsItem=new CheckableListItem<>(R.string.settings_show_interaction_counts, 0, CheckableListItem.Style.SWITCH, lp.showInteractionCounts, R.drawable.ic_fluent_number_row_24_regular, ()->toggleCheckableItem(interactionCountsItem)),
				emojiInNamesItem=new CheckableListItem<>(R.string.settings_show_emoji_in_names, 0, CheckableListItem.Style.SWITCH, lp.customEmojiInNames, R.drawable.ic_fluent_emoji_24_regular, ()->toggleCheckableItem(emojiInNamesItem)),
				marqueeItem=new CheckableListItem<>(R.string.sk_settings_enable_marquee, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.toolbarMarquee, R.drawable.ic_fluent_text_more_24_regular, ()->toggleCheckableItem(marqueeItem)),
				reduceMotionItem=new CheckableListItem<>(R.string.sk_settings_reduce_motion, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.reduceMotion, R.drawable.ic_fluent_star_emphasis_24_regular, ()->toggleCheckableItem(reduceMotionItem)),
				disableSwipeItem=new CheckableListItem<>(R.string.sk_settings_tabs_disable_swipe, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.disableSwipe, R.drawable.ic_fluent_swipe_right_24_regular, ()->toggleCheckableItem(disableSwipeItem)),
				altIndicatorItem=new CheckableListItem<>(R.string.sk_settings_show_alt_indicator, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.showAltIndicator, R.drawable.ic_fluent_scan_text_24_regular, ()->toggleCheckableItem(altIndicatorItem)),
				noAltIndicatorItem=new CheckableListItem<>(R.string.sk_settings_show_no_alt_indicator, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.showNoAltIndicator, R.drawable.ic_fluent_important_24_regular, ()->toggleCheckableItem(noAltIndicatorItem)),
				collapsePostsItem=new CheckableListItem<>(R.string.sk_settings_collapse_long_posts, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.collapseLongPosts, R.drawable.ic_fluent_chevron_down_24_regular, ()->toggleCheckableItem(collapsePostsItem)),
				spectatorModeItem=new CheckableListItem<>(R.string.sk_settings_hide_interaction, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.spectatorMode, R.drawable.ic_fluent_star_off_24_regular, ()->toggleCheckableItem(spectatorModeItem)),
				hideFabItem=new CheckableListItem<>(R.string.sk_settings_hide_fab, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.autoHideFab, R.drawable.ic_fluent_edit_24_regular, ()->toggleCheckableItem(hideFabItem)),
				translateOpenedItem=new CheckableListItem<>(R.string.sk_settings_translate_only_opened, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.translateButtonOpenedOnly, R.drawable.ic_fluent_translate_24_regular, ()->toggleCheckableItem(translateOpenedItem)),
				disablePillItem=new CheckableListItem<>(R.string.sk_disable_pill_shaped_active_indicator, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.disableM3PillActiveIndicator, R.drawable.ic_fluent_pill_24_regular, ()->toggleCheckableItem(disablePillItem))
		));
		trueBlackModeItem.checkedChangeListener=checked->onTrueBlackModeClick();
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		if(themeTransitionWindowView!=null){
			// Activity has finished recreating. Remove the overlay.
			activity.getSystemService(WindowManager.class).removeView(themeTransitionWindowView);
			themeTransitionWindowView=null;
		}
	}

	@Override
	protected void onHidden(){
		super.onHidden();

		boolean restartPlease=
				GlobalUserPreferences.disableM3PillActiveIndicator!=disablePillItem.checked;

		lp.revealCWs=revealCWsItem.checked;
		lp.hideSensitiveMedia=hideSensitiveMediaItem.checked;
		lp.showInteractionCounts=interactionCountsItem.checked;
		lp.customEmojiInNames=emojiInNamesItem.checked;
		lp.save();
		GlobalUserPreferences.toolbarMarquee=marqueeItem.checked;
		GlobalUserPreferences.reduceMotion=reduceMotionItem.checked;
		GlobalUserPreferences.disableSwipe=disableSwipeItem.checked;
		GlobalUserPreferences.showAltIndicator=altIndicatorItem.checked;
		GlobalUserPreferences.showNoAltIndicator=noAltIndicatorItem.checked;
		GlobalUserPreferences.collapseLongPosts=collapsePostsItem.checked;
		GlobalUserPreferences.spectatorMode=spectatorModeItem.checked;
		GlobalUserPreferences.autoHideFab=hideFabItem.checked;
		GlobalUserPreferences.translateButtonOpenedOnly=translateOpenedItem.checked;
		GlobalUserPreferences.disableM3PillActiveIndicator=disablePillItem.checked;
		GlobalUserPreferences.save();
		if(restartPlease) restartActivityToApplyNewTheme();
		else E.post(new StatusDisplaySettingsChangedEvent(accountID));
	}

	private @StringRes int getAppearanceValue(){
		return switch(GlobalUserPreferences.theme){
			case AUTO -> R.string.theme_auto;
			case LIGHT -> R.string.theme_light;
			case DARK -> R.string.theme_dark;
		};
	}

	private @StringRes int getColorPaletteValue(){
		return switch(GlobalUserPreferences.color){
			case MATERIAL3 -> R.string.sk_color_palette_material3;
			case PINK -> R.string.sk_color_palette_pink;
			case PURPLE -> R.string.sk_color_palette_purple;
			case GREEN -> R.string.sk_color_palette_green;
			case BLUE -> R.string.sk_color_palette_blue;
			case BROWN -> R.string.sk_color_palette_brown;
			case RED -> R.string.sk_color_palette_red;
			case YELLOW -> R.string.sk_color_palette_yellow;
		};
	}

	private String getPublishButtonText() {
		return TextUtils.isEmpty(AccountSessionManager.get(accountID).getLocalPreferences().publishButtonText)
				? getContext().getString(R.string.publish)
				: AccountSessionManager.get(accountID).getLocalPreferences().publishButtonText;
	}

	private @StringRes int getAutoRevealSpoilersText() {
		return switch(GlobalUserPreferences.autoRevealEqualSpoilers){
			case THREADS -> R.string.sk_settings_auto_reveal_author;
			case DISCUSSIONS -> R.string.sk_settings_auto_reveal_anyone;
			default -> R.string.sk_settings_auto_reveal_nobody;
		};
	}

	private void onTrueBlackModeClick(){
		toggleCheckableItem(trueBlackModeItem);
		boolean prev=GlobalUserPreferences.trueBlackTheme;
		GlobalUserPreferences.trueBlackTheme=trueBlackModeItem.checked;
		maybeApplyNewThemeRightNow(null, null, prev);
	}

	private void onAppearanceClick(){
		int selected=switch(GlobalUserPreferences.theme){
			case LIGHT -> 0;
			case DARK -> 1;
			case AUTO -> 2;
		};
		int[] newSelected={selected};
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.settings_theme)
				.setSingleChoiceItems((String[])IntStream.of(R.string.theme_light, R.string.theme_dark, R.string.theme_auto).mapToObj(this::getString).toArray(String[]::new),
						selected, (dlg, item)->newSelected[0]=item)
				.setPositiveButton(R.string.ok, (dlg, item)->{
					GlobalUserPreferences.ThemePreference pref=switch(newSelected[0]){
						case 0 -> GlobalUserPreferences.ThemePreference.LIGHT;
						case 1 -> GlobalUserPreferences.ThemePreference.DARK;
						case 2 -> GlobalUserPreferences.ThemePreference.AUTO;
						default -> throw new IllegalStateException("Unexpected value: "+newSelected[0]);
					};
					if(pref!=GlobalUserPreferences.theme){
						GlobalUserPreferences.ThemePreference prev=GlobalUserPreferences.theme;
						GlobalUserPreferences.theme=pref;
						GlobalUserPreferences.save();
						themeItem.subtitleRes=getAppearanceValue();
						rebindItem(themeItem);
						maybeApplyNewThemeRightNow(prev, null, null);
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void onColorClick(){
		int selected=GlobalUserPreferences.color.ordinal();
		int[] newSelected={selected};
		String[] names=Arrays.stream(GlobalUserPreferences.ColorPreference.values()).map(GlobalUserPreferences.ColorPreference::getName).map(this::getString).toArray(String[]::new);
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.settings_theme)
				.setSingleChoiceItems(names,
						selected, (dlg, item)->newSelected[0]=item)
				.setPositiveButton(R.string.ok, (dlg, item)->{
					GlobalUserPreferences.ColorPreference pref=GlobalUserPreferences.ColorPreference.values()[newSelected[0]];
					if(pref!=GlobalUserPreferences.color){
						GlobalUserPreferences.ColorPreference prev=GlobalUserPreferences.color;
						GlobalUserPreferences.color=pref;
						GlobalUserPreferences.save();
						colorItem.subtitleRes=getColorPaletteValue();
						rebindItem(colorItem);
						maybeApplyNewThemeRightNow(null, prev, null);
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void onPublishTextClick(){
		TextInputFrameLayout input = new TextInputFrameLayout(
				getContext(),
				getString(R.string.publish),
				TextUtils.isEmpty(lp.publishButtonText) ? "" : lp.publishButtonText.trim()
		);
		new M3AlertDialogBuilder(getContext()).setTitle(R.string.sk_settings_publish_button_text_title).setView(input)
				.setPositiveButton(R.string.save, (d, which) -> {
					lp.publishButtonText=input.getEditText().getText().toString().trim();
					lp.save();
					publishTextItem.subtitle=getPublishButtonText();
					rebindItem(publishTextItem);
				})
				.setNeutralButton(R.string.clear, (d, which) -> {
					lp.publishButtonText=null;
					lp.save();
					publishTextItem.subtitle=getPublishButtonText();
					rebindItem(publishTextItem);
				})
				.setNegativeButton(R.string.cancel, (d, which) -> {})
				.show();
	}

	private void onAutoRevealSpoilersClick(){
		int selected=GlobalUserPreferences.autoRevealEqualSpoilers.ordinal();
		int[] newSelected={selected};
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.sk_settings_auto_reveal_equal_spoilers)
				.setSingleChoiceItems((String[])IntStream.of(R.string.sk_settings_auto_reveal_nobody, R.string.sk_settings_auto_reveal_author, R.string.sk_settings_auto_reveal_anyone).mapToObj(this::getString).toArray(String[]::new),
						selected, (dlg, item)->newSelected[0]=item)
				.setPositiveButton(R.string.ok, (dlg, item)->{
					GlobalUserPreferences.autoRevealEqualSpoilers=GlobalUserPreferences.AutoRevealMode.values()[newSelected[0]];
					autoRevealCWsItem.subtitleRes=getAutoRevealSpoilersText();
					rebindItem(autoRevealCWsItem);
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void maybeApplyNewThemeRightNow(GlobalUserPreferences.ThemePreference prevTheme, GlobalUserPreferences.ColorPreference prevColor, Boolean prevTrueBlack){
		if(prevTheme==null) prevTheme=GlobalUserPreferences.theme;
		if(prevTrueBlack==null) prevTrueBlack=GlobalUserPreferences.trueBlackTheme;
		if(prevColor==null) prevColor=GlobalUserPreferences.color;

		boolean isCurrentDark=prevTheme==GlobalUserPreferences.ThemePreference.DARK ||
				(prevTheme==GlobalUserPreferences.ThemePreference.AUTO && Build.VERSION.SDK_INT>=30 && getResources().getConfiguration().isNightModeActive());
		boolean isNewDark=GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.DARK ||
				(GlobalUserPreferences.theme==GlobalUserPreferences.ThemePreference.AUTO && Build.VERSION.SDK_INT>=30 && getResources().getConfiguration().isNightModeActive());
		boolean isNewBlack=GlobalUserPreferences.trueBlackTheme;
		if(isCurrentDark!=isNewDark || prevColor!=GlobalUserPreferences.color || (isNewDark && prevTrueBlack!=isNewBlack)){
			restartActivityToApplyNewTheme();
		}
	}

	private void restartActivityToApplyNewTheme(){
		// Calling activity.recreate() causes a black screen for like half a second.
		// So, let's take a screenshot and overlay it on top to create the illusion of a smoother transition.
		// As a bonus, we can fade it out to make it even smoother.
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N && Build.VERSION.SDK_INT<Build.VERSION_CODES.S){
			View activityDecorView=getActivity().getWindow().getDecorView();
			Bitmap bitmap=Bitmap.createBitmap(activityDecorView.getWidth(), activityDecorView.getHeight(), Bitmap.Config.ARGB_8888);
			activityDecorView.draw(new Canvas(bitmap));
			themeTransitionWindowView=new ImageView(MastodonApp.context);
			themeTransitionWindowView.setImageBitmap(bitmap);
			WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.TYPE_APPLICATION);
			lp.flags=WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
					WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
			lp.systemUiVisibility=View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
			lp.systemUiVisibility|=(activityDecorView.getWindowSystemUiVisibility() & (View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
			lp.width=lp.height=WindowManager.LayoutParams.MATCH_PARENT;
			lp.token=getActivity().getWindow().getAttributes().token;
			lp.windowAnimations=R.style.window_fade_out;
			MastodonApp.context.getSystemService(WindowManager.class).addView(themeTransitionWindowView, lp);
		}
		getActivity().recreate();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		((FragmentStackActivity)getActivity()).invalidateSystemBarColors(this);
	}
}
