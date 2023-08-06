package org.joinmastodon.android.fragments.account_list;

import android.os.Bundle;
import android.view.View;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.viewmodel.AccountViewModel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;

public abstract class PaginatedAccountListFragment<T> extends BaseAccountListFragment{
	private String nextMaxID;
	private MastodonAPIRequest<T> remoteInfoRequest;
	protected boolean doneWithHomeInstance, remoteRequestFailed, startedRemoteLoading, remoteDisabled;
	protected int localOffset;
	protected T remoteInfo;

	public abstract HeaderPaginationRequest<Account> onCreateRequest(String maxID, int count);

	protected abstract MastodonAPIRequest<T> loadRemoteInfo();
	public abstract T getCurrentInfo();
	public abstract String getRemoteDomain();

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// already have remote info (e.g. from arguments), so no need to fetch it again
		if (remoteInfo != null) {
			onRemoteInfoLoaded(remoteInfo);
			return;
		}

		remoteDisabled = !GlobalUserPreferences.allowRemoteLoading
				|| getSession().domain.equals(getRemoteDomain());
		if (!remoteDisabled) {
			remoteInfoRequest = loadRemoteInfo().setCallback(new Callback<>() {
				@Override
				public void onSuccess(T result) {
					if (getContext() == null) return;
					onRemoteInfoLoaded(result);
				}

				@Override
				public void onError(ErrorResponse error) {
					if (getContext() == null) return;
					onRemoteLoadingFailed();
				}
			});
			remoteInfoRequest.execRemote(getRemoteDomain(), getRemoteSession());
		}
	}

	/**
	 * override to provide an ideal account session (e.g. if you're logged into the author's remote
	 * account) to make the remote request from. if null is provided, will try to get any session
	 * on the remote domain, or tries the request without authentication.
	 */
	protected AccountSession getRemoteSession() {
		return null;
	}

	protected void onRemoteInfoLoaded(T info) {
		this.remoteInfo = info;
		this.remoteInfoRequest = null;
		maybeStartLoadingRemote();
	}

	protected void onRemoteLoadingFailed() {
		this.remoteRequestFailed = true;
		this.remoteInfo = null;
		this.remoteInfoRequest = null;
		if (doneWithHomeInstance) dataLoaded();
	}

	@Override
	public void dataLoaded() {
		super.dataLoaded();
		footerProgress.setVisibility(View.GONE);
	}

	private void maybeStartLoadingRemote() {
		if (startedRemoteLoading || remoteDisabled) return;
		if (!remoteRequestFailed) {
			if (data.size() == 0) showProgress();
			else footerProgress.setVisibility(View.VISIBLE);
		}
		if (doneWithHomeInstance && remoteInfo != null) {
			startedRemoteLoading = true;
			loadData(localOffset, itemsPerPage * 2);
		}
	}

	@Override
	public void onRefresh() {
		localOffset = 0;
		doneWithHomeInstance = false;
		startedRemoteLoading = false;
		super.onRefresh();
	}

	@Override
	public void loadData(int offset, int count) {
		// always subtract the amount loaded through the home instance once loading from remote
		// since loadData gets called with data.size() (data includes both local and remote)
		if (doneWithHomeInstance) offset -= localOffset;
		super.loadData(offset, count);
	}

	@Override
	protected void doLoadData(int offset, int count){
		MastodonAPIRequest<?> request = onCreateRequest(offset==0 ? null : nextMaxID, count)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(HeaderPaginationList<Account> result){
						boolean justRefreshed = !doneWithHomeInstance && offset == 0;
						Collection<AccountViewModel> d = justRefreshed ? List.of() : data;

						if(result.nextPageUri!=null)
							nextMaxID=result.nextPageUri.getQueryParameter("max_id");
						else
							nextMaxID=null;
						if (getActivity() == null) return;
						List<AccountViewModel> items = result.stream()
								.filter(a -> d.size() > 1000 || d.stream()
										.noneMatch(i -> i.account.url.equals(a.url)))
								.peek(account ->{
									if (account.getDomainFromURL().equals(getRemoteDomain()))
										account.acct=account.getFullyQualifiedName();
								})
								.map(a->new AccountViewModel(a, accountID))
								.collect(Collectors.toList());

						boolean hasMore = nextMaxID != null;

						if (!hasMore && !doneWithHomeInstance) {
							// only runs last time data was fetched from the home instance
							localOffset = d.size() + items.size();
							doneWithHomeInstance = true;
						}

						onDataLoaded(items, hasMore);
						if (doneWithHomeInstance) maybeStartLoadingRemote();
					}

					@Override
					public void onError(ErrorResponse error) {
						if (doneWithHomeInstance) {
							onRemoteLoadingFailed();
							onDataLoaded(Collections.emptyList(), false);
							return;
						}
						super.onError(error);
					}
				});

		if (doneWithHomeInstance && remoteInfo == null) return; // we are waiting
		if (doneWithHomeInstance && remoteInfo != null) {
			request.execRemote(getRemoteDomain(), getRemoteSession());
		} else {
			request.exec(accountID);
		}
		currentRequest = request;
	}

	@Override
	public void onResume(){
		super.onResume();
		if(!loaded && !dataLoading)
			loadData();
	}
}
