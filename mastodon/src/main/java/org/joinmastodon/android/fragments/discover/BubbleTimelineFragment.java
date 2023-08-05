package org.joinmastodon.android.fragments.discover;

import android.net.Uri;
import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.api.requests.timelines.GetBubbleTimeline;
import org.joinmastodon.android.fragments.StatusListFragment;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.utils.StatusFilterPredicate;

import java.util.List;
import java.util.stream.Collectors;

import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;

public class BubbleTimelineFragment extends StatusListFragment {
    private DiscoverInfoBannerHelper bannerHelper;
    private String maxID;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		bannerHelper=new DiscoverInfoBannerHelper(DiscoverInfoBannerHelper.BannerType.BUBBLE_TIMELINE, accountID);
	}


	@Override
    protected boolean wantsComposeButton() {
        return true;
    }

	@Override
    protected void doLoadData(int offset, int count){
        currentRequest=new GetBubbleTimeline(refreshing ? null : maxID, count, getLocalPrefs().timelineReplyVisibility)
                .setCallback(new SimpleCallback<>(this){
                    @Override
                    public void onSuccess(List<Status> result){
                        if(!result.isEmpty())
                            maxID=result.get(result.size()-1).id;
                        if (getActivity() == null) return;
                        result=result.stream().filter(new StatusFilterPredicate(accountID, getFilterContext())).collect(Collectors.toList());
                        onDataLoaded(result, !result.isEmpty());
                    }
                })
                .exec(accountID);
    }

	@Override
	protected RecyclerView.Adapter<?> getAdapter(){
		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		bannerHelper.maybeAddBanner(list, adapter);
		adapter.addAdapter(super.getAdapter());
		return adapter;
	}

	@Override
    protected FilterContext getFilterContext() {
        return FilterContext.PUBLIC;
    }

    @Override
    public Uri getWebUri(Uri.Builder base) {
        return isInstanceAkkoma() ? base.path("/main/bubble").build() : null;
    }
}
