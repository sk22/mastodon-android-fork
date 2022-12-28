package org.joinmastodon.android.fragments;

import android.app.Activity;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.GetBookmarkedStatuses;
import org.joinmastodon.android.api.requests.statuses.GetScheduledStatuses;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;

import java.util.List;

import me.grishka.appkit.api.SimpleCallback;

public class ScheduledStatusListFragment extends BaseStatusListFragment<ScheduledStatus> {
	private String nextMaxID;

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setTitle(R.string.sk_unsent_posts);
		loadData();
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(ScheduledStatus s) {
		return StatusDisplayItem.buildItems(this, s.params, accountID, s, knownAccounts, false, false, null);
	}

	@Override
	protected void addAccountToKnown(ScheduledStatus s) {}

	@Override
	public void onItemClick(String id) {

	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetScheduledStatuses(offset==0 ? null : nextMaxID, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(HeaderPaginationList<ScheduledStatus> result){
						if(result.nextPageUri!=null)
							nextMaxID=result.nextPageUri.getQueryParameter("max_id");
						else
							nextMaxID=null;
						onDataLoaded(result, nextMaxID!=null);
					}
				})
				.exec(accountID);
	}
}
