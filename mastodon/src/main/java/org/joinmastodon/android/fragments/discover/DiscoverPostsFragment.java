package org.joinmastodon.android.fragments.discover;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.joinmastodon.android.api.requests.trends.GetTrendingStatuses;
import org.joinmastodon.android.fragments.StatusListFragment;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.utils.StatusFilterPredicate;

import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.api.SimpleCallback;

public class DiscoverPostsFragment extends StatusListFragment {
	private DiscoverInfoBannerHelper bannerHelper=new DiscoverInfoBannerHelper(DiscoverInfoBannerHelper.BannerType.TRENDING_POSTS);

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetTrendingStatuses(offset, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						if (getActivity() == null) return;
						result=result.stream().filter(new StatusFilterPredicate(accountID, getFilterContext())).collect(Collectors.toList());
						onDataLoaded(result, !result.isEmpty());
					}
				}).exec(accountID);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		bannerHelper.maybeAddBanner(contentWrap);
	}

	@Override
	protected FilterContext getFilterContext() {
		return FilterContext.PUBLIC;
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return isInstanceAkkoma() ? null : base.path("/explore/posts").build();
	}
}
