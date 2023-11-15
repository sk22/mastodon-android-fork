package org.joinmastodon.android.fragments.settings;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.BuildConfig;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIController;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.viewmodel.CheckableListItem;
import org.joinmastodon.android.model.viewmodel.ListItem;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.imageloader.ImageCache;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class SettingsAboutAppFragment extends BaseSettingsFragment<Void>{
	private static final String TAG="SettingsAboutAppFragment";
	private ListItem<Void> mediaCacheItem, copyCrashLogItem;
	private CheckableListItem<Void> enablePreReleasesItem;
	private AccountSession session;
	private boolean timelineCacheCleared=false;
	private File crashLogFile=new File(MastodonApp.context.getFilesDir(), "crash.log");

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(getString(R.string.about_app, getString(R.string.sk_app_name)));
		session=AccountSessionManager.get(accountID);

		String lastModified=crashLogFile.exists()
				? DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(crashLogFile.lastModified()))
				: getString(R.string.sk_settings_crash_log_unavailable);
		List<ListItem<Void>> items=new ArrayList<>(List.of(
				new ListItem<>(R.string.sk_settings_donate, 0, R.drawable.ic_fluent_heart_24_regular, i->UiUtils.openHashtagTimeline(getActivity(), accountID, getString(R.string.donate_hashtag))),
				new ListItem<>(R.string.sk_settings_contribute, 0, R.drawable.ic_fluent_open_24_regular, i->UiUtils.launchWebBrowser(getActivity(), getString(R.string.repo_url))),
				new ListItem<>(R.string.settings_tos, 0, R.drawable.ic_fluent_open_24_regular, i->UiUtils.launchWebBrowser(getActivity(), "https://"+session.domain+"/terms")),
				new ListItem<>(R.string.settings_privacy_policy, 0, R.drawable.ic_fluent_open_24_regular, i->UiUtils.launchWebBrowser(getActivity(), getString(R.string.privacy_policy_url)), 0, true),
				mediaCacheItem=new ListItem<>(R.string.settings_clear_cache, 0, this::onClearMediaCacheClick),
				new ListItem<>(getString(R.string.sk_settings_clear_timeline_cache), session.domain, this::onClearTimelineCacheClick),
				copyCrashLogItem=new ListItem<>(getString(R.string.sk_settings_copy_crash_log), lastModified, 0, this::onCopyCrashLog)
		));

		if(GithubSelfUpdater.needSelfUpdating()){
			items.add(enablePreReleasesItem=new CheckableListItem<>(R.string.sk_updater_enable_pre_releases, 0, CheckableListItem.Style.SWITCH, GlobalUserPreferences.enablePreReleases, i->toggleCheckableItem(enablePreReleasesItem)));
		}

		copyCrashLogItem.isEnabled=crashLogFile.exists();
		onDataLoaded(items);
		updateMediaCacheItem();
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		GlobalUserPreferences.enablePreReleases=enablePreReleasesItem.checked;
		GlobalUserPreferences.save();
		if(timelineCacheCleared) getActivity().recreate();
	}

	@Override
	protected void doLoadData(int offset, int count){}

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		adapter.addAdapter(super.getAdapter());

		TextView versionInfo=new TextView(getActivity());
		versionInfo.setSingleLine();
		versionInfo.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(32)));
		versionInfo.setTextAppearance(R.style.m3_label_medium);
		versionInfo.setTextColor(UiUtils.getThemeColor(getActivity(), R.attr.colorM3Outline));
		versionInfo.setGravity(Gravity.CENTER);
		versionInfo.setText(getString(R.string.sk_settings_app_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
		adapter.addAdapter(new SingleViewRecyclerAdapter(versionInfo));

		return adapter;
	}

	private void onClearMediaCacheClick(ListItem<?> item){
		MastodonAPIController.runInBackground(()->{
			Activity activity=getActivity();
			ImageCache.getInstance(getActivity()).clear();
			activity.runOnUiThread(()->{
				Toast.makeText(activity, R.string.media_cache_cleared, Toast.LENGTH_SHORT).show();
				updateMediaCacheItem();
			});
		});
	}

	private void onClearTimelineCacheClick(ListItem<?> item){
		session.getCacheController().putHomeTimeline(List.of(), true);
		Toast.makeText(getContext(), R.string.sk_timeline_cache_cleared, Toast.LENGTH_SHORT).show();
		timelineCacheCleared=true;
	}

	private void updateMediaCacheItem(){
		long size=ImageCache.getInstance(getActivity()).getDiskCache().size();
		mediaCacheItem.subtitle=UiUtils.formatFileSize(getActivity(), size, false);
		mediaCacheItem.isEnabled=size>0;
		rebindItem(mediaCacheItem);
	}

	private void onCopyCrashLog(ListItem<?> item){
		if(!crashLogFile.exists()) return;
		try(InputStream is=new FileInputStream(crashLogFile)){
			BufferedReader reader=new BufferedReader(new InputStreamReader(is));
			StringBuilder sb=new StringBuilder();
			String line;
			while ((line=reader.readLine())!=null) sb.append(line).append("\n");
			UiUtils.copyText(list, sb.toString());
		} catch(IOException e){
			Log.e(TAG, "Error reading crash log", e);
		}
	}
}
