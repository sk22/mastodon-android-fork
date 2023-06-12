package org.joinmastodon.android.fragments;

import android.net.Uri;
import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class PinnedPostsListFragment extends StatusListFragment{
	private Account account;

	public PinnedPostsListFragment() {
		setListLayoutId(R.layout.recycler_fragment_no_refresh);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		account=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
		setTitle(R.string.posts);
		loadData();
	}

	@Override
	protected void doLoadData(int offset, int count){
		new GetAccountStatuses(account.id, null, null, 100, GetAccountStatuses.Filter.PINNED)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, false);
					}
				}).exec(accountID);
	}

	@Override
	protected Filter.FilterContext getFilterContext() {
		return Filter.FilterContext.ACCOUNT;
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return Uri.parse(account.url);
	}
}
