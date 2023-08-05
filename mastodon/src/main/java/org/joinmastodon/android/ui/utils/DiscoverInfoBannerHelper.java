package org.joinmastodon.android.ui.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.MastodonApp;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSessionManager;

import java.util.EnumSet;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;

public class DiscoverInfoBannerHelper{
	private View banner;
	private final BannerType type;
	private final String accountID;
	private static EnumSet<BannerType> bannerTypesToShow=EnumSet.noneOf(BannerType.class);

	static{
		for(BannerType t:BannerType.values()){
			if(!getPrefs().getBoolean("bannerHidden_"+t, false))
				bannerTypesToShow.add(t);
		}
	}

	public DiscoverInfoBannerHelper(BannerType type, String accountID){
		this.type=type;
		this.accountID=accountID;
	}

	private static SharedPreferences getPrefs(){
		return MastodonApp.context.getSharedPreferences("onboarding", Context.MODE_PRIVATE);
	}

	public void maybeAddBanner(RecyclerView list, MergeRecyclerAdapter adapter){
		if(bannerTypesToShow.contains(type)){
			banner=((Activity)list.getContext()).getLayoutInflater().inflate(R.layout.discover_info_banner, list, false);
			TextView text=banner.findViewById(R.id.banner_text);
			text.setText(switch(type){
				case TRENDING_POSTS -> list.getResources().getString(R.string.trending_posts_info_banner);
				case TRENDING_LINKS -> list.getResources().getString(R.string.trending_links_info_banner);
				case FEDERATED_TIMELINE -> list.getResources().getString(R.string.sk_federated_timeline_info_banner);
				case POST_NOTIFICATIONS -> list.getResources().getString(R.string.sk_notify_posts_info_banner);
				case BUBBLE_TIMELINE -> list.getResources().getString(R.string.sk_bubble_timeline_info_banner);
				case LOCAL_TIMELINE -> list.getResources().getString(R.string.local_timeline_info_banner, AccountSessionManager.get(accountID).domain);
				case ACCOUNTS -> list.getResources().getString(R.string.recommended_accounts_info_banner);
			});
			ImageView icon=banner.findViewById(R.id.icon);
			icon.setImageResource(switch(type){
				case TRENDING_POSTS -> R.drawable.ic_fluent_arrow_trending_24_regular;
				case TRENDING_LINKS -> R.drawable.ic_fluent_news_24_regular;
				case ACCOUNTS -> R.drawable.ic_fluent_people_add_24_regular;
				// no icon because those are displayed as timelines - with icon in top left
				case LOCAL_TIMELINE, FEDERATED_TIMELINE, BUBBLE_TIMELINE, POST_NOTIFICATIONS -> 0;
			});
			adapter.addAdapter(new SingleViewRecyclerAdapter(banner));
		}
	}

	public void onBannerBecameVisible(){
		getPrefs().edit().putBoolean("bannerHidden_"+type, true).apply();
		// bannerTypesToShow is not updated here on purpose so the banner keeps showing until the app is relaunched
	}

	public static void reset(){
		SharedPreferences prefs=getPrefs();
		SharedPreferences.Editor e=prefs.edit();
		prefs.getAll().keySet().stream().filter(k->k.startsWith("bannerHidden_")).forEach(e::remove);
		e.apply();
		bannerTypesToShow=EnumSet.allOf(BannerType.class);
	}

	public enum BannerType{
		TRENDING_POSTS,
		TRENDING_LINKS,
		LOCAL_TIMELINE,
		FEDERATED_TIMELINE,
		POST_NOTIFICATIONS,
		ACCOUNTS,
		BUBBLE_TIMELINE
	}
}
