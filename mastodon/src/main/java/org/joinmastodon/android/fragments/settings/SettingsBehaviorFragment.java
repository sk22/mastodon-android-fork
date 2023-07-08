package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Preferences;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.viewcontrollers.ComposeLanguageAlertViewController;
import org.joinmastodon.android.utils.MastodonLanguage;

import java.util.List;

public class SettingsBehaviorFragment extends BaseSettingsFragment<Void>{
	private ListItem<Void> languageItem;
	private CheckableListItem<Void> altTextItem, playGifsItem, customTabsItem, confirmUnfollowItem, confirmBoostItem, confirmDeleteItem;
	private MastodonLanguage postLanguage;
	private ComposeLanguageAlertViewController.SelectedOption newPostLanguage;
	private MastodonLanguage.LanguageResolver languageResolver;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.settings_behavior);

		AccountSession s=AccountSessionManager.get(accountID);
		languageResolver = s.getInstance().map(MastodonLanguage.LanguageResolver::new).orElse(null);
		postLanguage=s.preferences==null || s.preferences.postingDefaultLanguage==null ? null :
				languageResolver.from(s.preferences.postingDefaultLanguage).orElse(null);

		onDataLoaded(List.of(
				languageItem=new ListItem<>(getString(R.string.default_post_language), postLanguage!=null ? postLanguage.getDisplayName(getContext()) : null, R.drawable.ic_fluent_local_language_24_regular, this::onDefaultLanguageClick),
				altTextItem=new CheckableListItem<>(R.string.settings_alt_text_reminders, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.altTextReminders, R.drawable.ic_fluent_image_alt_text_24_regular, ()->toggleCheckableItem(altTextItem)),
				playGifsItem=new CheckableListItem<>(R.string.settings_gif, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.playGifs, R.drawable.ic_fluent_gif_24_regular, ()->toggleCheckableItem(playGifsItem)),
				customTabsItem=new CheckableListItem<>(R.string.settings_custom_tabs, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.useCustomTabs, R.drawable.ic_fluent_link_24_regular, ()->toggleCheckableItem(customTabsItem)),
				confirmUnfollowItem=new CheckableListItem<>(R.string.settings_confirm_unfollow, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.confirmUnfollow, R.drawable.ic_fluent_person_delete_24_regular, ()->toggleCheckableItem(confirmUnfollowItem)),
				confirmBoostItem=new CheckableListItem<>(R.string.settings_confirm_boost, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.confirmBoost, R.drawable.ic_fluent_arrow_repeat_all_24_regular, ()->toggleCheckableItem(confirmBoostItem)),
				confirmDeleteItem=new CheckableListItem<>(R.string.settings_confirm_delete_post, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.confirmDeletePost, R.drawable.ic_fluent_delete_24_regular, ()->toggleCheckableItem(confirmDeleteItem))
		));
	}

	@Override
	protected void doLoadData(int offset, int count){}

	private void onDefaultLanguageClick(){
		if (languageResolver == null) return;
		ComposeLanguageAlertViewController vc=new ComposeLanguageAlertViewController(getActivity(), null, new ComposeLanguageAlertViewController.SelectedOption(postLanguage), null, languageResolver);
		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.default_post_language)
				.setView(vc.getView())
				.setPositiveButton(R.string.ok, (dlg, which)->{
					ComposeLanguageAlertViewController.SelectedOption opt=vc.getSelectedOption();
					if(!opt.language.equals(postLanguage)){
						newPostLanguage=opt;
						languageItem.subtitle=newPostLanguage.language.getDefaultName();
						rebindItem(languageItem);
					}
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		GlobalUserPreferences.playGifs=playGifsItem.checked;
		GlobalUserPreferences.useCustomTabs=customTabsItem.checked;
		GlobalUserPreferences.altTextReminders=altTextItem.checked;
		GlobalUserPreferences.confirmUnfollow=customTabsItem.checked;
		GlobalUserPreferences.confirmBoost=confirmBoostItem.checked;
		GlobalUserPreferences.confirmDeletePost=confirmDeleteItem.checked;
		GlobalUserPreferences.save();
		if(newPostLanguage!=null){
			AccountSession s=AccountSessionManager.get(accountID);
			if(s.preferences==null)
				s.preferences=new Preferences();
			s.preferences.postingDefaultLanguage=newPostLanguage.language.getLanguage();
			s.savePreferencesLater();
		}
	}
}
