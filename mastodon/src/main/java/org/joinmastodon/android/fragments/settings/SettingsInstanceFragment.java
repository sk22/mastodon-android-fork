package org.joinmastodon.android.fragments.settings;

import android.os.Bundle;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HasAccountID;
import org.joinmastodon.android.model.ContentType;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;

import java.util.Arrays;
import java.util.List;

import me.grishka.appkit.Nav;

public class SettingsInstanceFragment extends BaseSettingsFragment<Void> implements HasAccountID{
	private CheckableListItem<Void> contentTypesItem;
	private ListItem<Void> defaultContentTypeItem;
	private AccountLocalPreferences lp;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.sk_settings_instance);
		AccountSession s=AccountSessionManager.get(accountID);
		lp=s.getLocalPreferences();
		onDataLoaded(List.of(
				new ListItem<>(AccountSessionManager.get(accountID).domain, getString(R.string.settings_server_explanation), R.drawable.ic_fluent_server_24_regular, this::onServerClick, true),
				contentTypesItem=new CheckableListItem<>(R.string.sk_settings_content_types, R.string.sk_settings_content_types_explanation, CheckableListItem.Style.SWITCH, lp.contentTypesEnabled, R.drawable.ic_fluent_text_edit_style_24_regular, this::onContentTypeClick),
				defaultContentTypeItem=new ListItem<>(R.string.sk_settings_default_content_type, lp.defaultContentType.getName(), R.drawable.ic_fluent_text_bold_24_regular, this::onDefaultContentTypeClick)
		));
		contentTypesItem.checkedChangeListener=checked->onContentTypeClick();
		defaultContentTypeItem.isEnabled=contentTypesItem.checked;
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected void onHidden(){
		super.onHidden();
		lp.contentTypesEnabled=contentTypesItem.checked;
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

	@Override
	public String getAccountID(){
		return accountID;
	}
}
