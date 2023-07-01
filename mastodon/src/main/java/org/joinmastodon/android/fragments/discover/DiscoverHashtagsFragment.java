package org.joinmastodon.android.fragments.discover;

import static org.joinmastodon.android.ui.displayitems.HashtagStatusDisplayItem.Holder.withHistoryParams;
import static org.joinmastodon.android.ui.displayitems.HashtagStatusDisplayItem.Holder.withoutHistoryParams;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.trends.GetTrendingHashtags;
import org.joinmastodon.android.fragments.IsOnTop;
import org.joinmastodon.android.fragments.RecyclerFragment;
import org.joinmastodon.android.fragments.ScrollableToTop;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.ui.DividerItemDecoration;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.HashtagChartView;
import org.joinmastodon.android.utils.ProvidesAssistContent;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class DiscoverHashtagsFragment extends RecyclerFragment<Hashtag> implements ScrollableToTop, IsOnTop, ProvidesAssistContent.ProvidesWebUri {
	private String accountID;
	private DiscoverInfoBannerHelper bannerHelper=new DiscoverInfoBannerHelper(DiscoverInfoBannerHelper.BannerType.TRENDING_HASHTAGS);

	public DiscoverHashtagsFragment(){
		super(10);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetTrendingHashtags(10)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Hashtag> result){
						if (getActivity() == null) return;
						onDataLoaded(result, false);
					}
				})
				.exec(accountID);
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		return new HashtagsAdapter();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new DividerItemDecoration(getActivity(), R.attr.colorM3OutlineVariant, .5f, 16, 16));
		bannerHelper.maybeAddBanner(contentWrap);
	}

	@Override
	public void scrollToTop(){
		smoothScrollRecyclerViewToTop(list);
	}

	@Override
	public boolean isOnTop() {
		return isRecyclerViewOnTop(list);
	}

	@Override
	public String getAccountID() {
		return accountID;
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return isInstanceAkkoma() ? null : base.path("/explore/tags").build();
	}

	private class HashtagsAdapter extends RecyclerView.Adapter<HashtagViewHolder>{
		@NonNull
		@Override
		public HashtagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new HashtagViewHolder();
		}

		@Override
		public void onBindViewHolder(@NonNull HashtagViewHolder holder, int position){
			holder.bind(data.get(position));
		}

		@Override
		public int getItemCount(){
			return data.size();
		}
	}

	private class HashtagViewHolder extends BindableViewHolder<Hashtag> implements UsableRecyclerView.Clickable{
		private final TextView title, subtitle;
		private final HashtagChartView chart;

		public HashtagViewHolder(){
			super(getActivity(), R.layout.item_trending_hashtag, list);
			title=findViewById(R.id.title);
			subtitle=findViewById(R.id.subtitle);
			chart=findViewById(R.id.chart);
		}

		@Override
		public void onBind(Hashtag item){
			title.setText('#'+item.name);
			if (item.history == null || item.history.isEmpty()) {
				subtitle.setText(null);
				chart.setVisibility(View.GONE);
				title.setLayoutParams(withoutHistoryParams);
				return;
			}
			chart.setVisibility(View.VISIBLE);
			title.setLayoutParams(withHistoryParams);
			int numPeople=item.history.get(0).accounts;
			if(item.history.size()>1)
				numPeople+=item.history.get(1).accounts;
			subtitle.setText(getResources().getQuantityString(R.plurals.x_people_talking, numPeople, numPeople));
			chart.setData(item.history);
		}

		@Override
		public void onClick(){
			UiUtils.openHashtagTimeline(getActivity(), accountID, item.name, item.following);
		}
	}
}
