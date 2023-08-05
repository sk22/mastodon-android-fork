package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HasAccountID;
import org.joinmastodon.android.model.ContentType;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.Arrays;
import java.util.List;

import me.grishka.appkit.Nav;

public class SettingsInstanceFragment extends BaseSettingsFragment<Void> implements HasAccountID{
	private CheckableListItem<Void> contentTypesItem, emojiReactionsItem, emojiReactionsInListsItem, localOnlyItem, glitchModeItem;
	private ListItem<Void> defaultContentTypeItem;
	private AccountLocalPreferences lp;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.sk_settings_instance);
		AccountSession s=AccountSessionManager.get(accountID);
		lp=s.getLocalPreferences();
		onDataLoaded(List.of(
				new ListItem<>(AccountSessionManager.get(accountID).domain, getString(R.string.settings_server_explanation), R.drawable.ic_fluent_server_24_regular, this::onServerClick),
				new ListItem<>(R.string.sk_settings_profile, 0, R.drawable.ic_fluent_open_24_regular, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+s.domain+"/settings/profile")),
				new ListItem<>(R.string.sk_settings_posting, 0, R.drawable.ic_fluent_open_24_regular, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+s.domain+"/settings/preferences/other")),
				new ListItem<>(R.string.sk_settings_auth, 0, R.drawable.ic_fluent_open_24_regular, ()->UiUtils.launchWebBrowser(getActivity(), "https://"+s.domain+"/auth/edit"), 0, true),
				contentTypesItem=new CheckableListItem<>(R.string.sk_settings_content_types, R.string.sk_settings_content_types_explanation, CheckableListItem.Style.SWITCH, lp.contentTypesEnabled, R.drawable.ic_fluent_text_edit_style_24_regular, this::onContentTypeClick),
				defaultContentTypeItem=new ListItem<>(R.string.sk_settings_default_content_type, lp.defaultContentType.getName(), R.drawable.ic_fluent_text_bold_24_regular, this::onDefaultContentTypeClick),
				emojiReactionsItem=new CheckableListItem<>(R.string.sk_settings_emoji_reactions, R.string.sk_settings_emoji_reactions_explanation, CheckableListItem.Style.SWITCH, lp.emojiReactionsEnabled, R.drawable.ic_fluent_emoji_laugh_24_regular, this::onEmojiReactionsClick),
				emojiReactionsInListsItem=new CheckableListItem<>(R.string.sk_settings_emoji_reactions_in_lists, R.string.sk_settings_emoji_reactions_in_lists_explanation, CheckableListItem.Style.SWITCH, lp.showEmojiReactionsInLists, R.drawable.ic_fluent_emoji_24_regular, ()->toggleCheckableItem(emojiReactionsInListsItem)),
				localOnlyItem=new CheckableListItem<>(R.string.sk_settings_support_local_only, R.string.sk_settings_local_only_explanation, CheckableListItem.Style.SWITCH, lp.localOnlySupported, R.drawable.ic_fluent_eye_24_regular, this::onLocalOnlyClick),
				glitchModeItem=new CheckableListItem<>(R.string.sk_settings_glitch_instance, R.string.sk_settings_glitch_mode_explanation, CheckableListItem.Style.SWITCH, lp.glitchInstance, R.drawable.ic_fluent_eye_24_filled, ()->toggleCheckableItem(glitchModeItem))
		));
		contentTypesItem.checkedChangeListener=checked->onContentTypeClick();
		defaultContentTypeItem.isEnabled=contentTypesItem.checked;
		emojiReactionsItem.checkedChangeListener=checked->onEmojiReactionsClick();
		emojiReactionsInListsItem.isEnabled=emojiReactionsItem.checked;
		localOnlyItem.checkedChangeListener=checked->onLocalOnlyClick();
		glitchModeItem.isEnabled=localOnlyItem.checked;
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected void onHidden(){
		super.onHidden();
		lp.contentTypesEnabled=contentTypesItem.checked;
		lp.emojiReactionsEnabled=emojiReactionsItem.checked;
		lp.showEmojiReactionsInLists=emojiReactionsInListsItem.checked;
		lp.localOnlySupported=localOnlyItem.checked;
		lp.glitchInstance=glitchModeItem.checked;
		lp.save();
	}

	private void onServerClick(){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		Nav.go(getActivity(), SettingsServerFragment.class, args);
	}

	private void onContentTypeClick(){
		toggleCheckableItem(contentTypesItem);
		defaultContentTypeItem.isEnabled=contentTypesItem.checked;
		resetDefaultContentType();
		rebindItem(defaultContentTypeItem);
	}

	private void resetDefaultContentType(){
		lp.defaultContentType=defaultContentTypeItem.isEnabled
				? ContentType.PLAIN : ContentType.UNSPECIFIED;
		defaultContentTypeItem.subtitleRes=lp.defaultContentType.getName();
	}

	private void onDefaultContentTypeClick(){
		int selected=lp.defaultContentType.ordinal();
		int[] newSelected={selected};
		ContentType[] supportedContentTypes=Arrays.stream(ContentType.values())
				.filter(t->t.supportedByInstance(getInstance().orElse(null)))
				.toArray(ContentType[]::new);
		String[] names=Arrays.stream(supportedContentTypes)
				.map(ContentType::getName)
				.map(this::getString)
				.toArray(String[]::new);

		new M3AlertDialogBuilder(getActivity())
				.setTitle(R.string.settings_theme)
				.setSingleChoiceItems(names,
						selected, (dlg, item)->newSelected[0]=item)
				.setPositiveButton(R.string.ok, (dlg, item)->{
					ContentType type=supportedContentTypes[newSelected[0]];
					lp.defaultContentType=type;
					defaultContentTypeItem.subtitleRes=type.getName();
					rebindItem(defaultContentTypeItem);
				})
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void onEmojiReactionsClick(){
		toggleCheckableItem(emojiReactionsItem);
		emojiReactionsInListsItem.checked=false;
		emojiReactionsInListsItem.isEnabled=emojiReactionsItem.checked;
		rebindItem(emojiReactionsInListsItem);
	}

	private void onLocalOnlyClick(){
		toggleCheckableItem(localOnlyItem);
		glitchModeItem.checked=localOnlyItem.checked && !isInstanceAkkoma();
		glitchModeItem.isEnabled=localOnlyItem.checked;
		rebindItem(glitchModeItem);
	}

	@Override
	public String getAccountID(){
		return accountID;
	}
}
