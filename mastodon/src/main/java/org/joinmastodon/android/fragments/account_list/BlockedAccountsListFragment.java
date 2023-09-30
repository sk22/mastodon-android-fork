package org.joinmastodon.android.fragments.account_list;

import android.net.Uri;
import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.api.requests.accounts.GetAccountBlocks;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.ui.viewholders.AccountViewHolder;

public class BlockedAccountsListFragment extends AccountRelatedAccountListFragment{

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setTitle(R.string.sk_blocked_accounts);
	}

	@Override
	public HeaderPaginationRequest<Account> onCreateRequest(String maxID, int count){
		return new GetAccountBlocks(maxID, count);
	}

	@Override
	protected void onConfigureViewHolder(AccountViewHolder holder){
		super.onConfigureViewHolder(holder);
		holder.setStyle(AccountViewHolder.AccessoryType.NONE, false);
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return super.getWebUri(base).buildUpon()
				.appendPath("/blocks").build();
	}
}
