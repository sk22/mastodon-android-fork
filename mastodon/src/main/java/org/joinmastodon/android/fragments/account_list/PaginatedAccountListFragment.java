package org.joinmastodon.android.fragments.account_list;

import android.net.Uri;

import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.stream.Collectors;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;

public abstract class PaginatedAccountListFragment extends BaseAccountListFragment{
	private String nextMaxID;

	protected Account targetAccount;

	public abstract HeaderPaginationRequest<Account> onCreateRequest(String maxID, int count);

	public abstract HeaderPaginationRequest<Account> onCreateRemoteRequest(String id, String maxID, int count);

	@Override
	protected void doLoadData(int offset, int count){
		if (shouldLoadRemote()) {
				UiUtils.lookupRemoteAccount(getContext(), targetAccount, accountID, null, account -> {
					if(account != null){
						loadRemoteFollower(offset, count, account);
					} else {
						loadFollower(offset, count);
					}
				});
		} else {
			loadFollower(offset, count);
		}
	}

	private boolean shouldLoadRemote() {
		if (this instanceof FollowingListFragment || this instanceof FollowerListFragment) {
			return false;
		}
		return targetAccount != null && targetAccount.getDomain() != null;
	}

	void loadFollower(int offset, int count) {
		currentRequest=onCreateRequest(offset==0 ? null : nextMaxID, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(HeaderPaginationList<Account> result){
						if(result.nextPageUri!=null)
							nextMaxID=result.nextPageUri.getQueryParameter("max_id");
						else
							nextMaxID=null;
						onDataLoaded(result.stream().map(AccountItem::new).collect(Collectors.toList()), nextMaxID!=null);
					}
				})
				.exec(accountID);
	}

	private void loadRemoteFollower(int offset, int count, Account account) {
		String ownDomain = AccountSessionManager.getInstance().getLastActiveAccount().domain;
		currentRequest=onCreateRemoteRequest(account.id, offset==0 ? null : nextMaxID, count)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(HeaderPaginationList<Account> result){
						if(result.nextPageUri!=null)
							nextMaxID=result.nextPageUri.getQueryParameter("max_id");
						else
							nextMaxID=null;
						result.stream().forEach(remoteAccount -> {
							remoteAccount.remoteAccount = true;
							if (remoteAccount.getDomain() == null) {
								remoteAccount.acct += "@" + Uri.parse(remoteAccount.url).getHost();
							} else if (remoteAccount.getDomain().equals(ownDomain)) {
								remoteAccount.acct = remoteAccount.username;
							}
						});
						onDataLoaded(result.stream().map(AccountItem::new).collect(Collectors.toList()), nextMaxID!=null);
					}

					@Override
					public void onError(ErrorResponse error) {
						error.showToast(getContext());
						loadFollower(offset, count);
					}
				})
				.execNoAuth(targetAccount.getDomain());
	}

	@Override
	public void onResume(){
		super.onResume();
		if(!loaded && !dataLoading)
			loadData();
	}
}
