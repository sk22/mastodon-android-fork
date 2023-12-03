package org.joinmastodon.android.fragments.account_list;

import android.net.Uri;
import android.os.Bundle;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.api.requests.statuses.GetStatusByID;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.model.Status;
import org.parceler.Parcels;

import java.util.Optional;

public abstract class StatusRelatedAccountListFragment extends PaginatedAccountListFragment<Status> {
	protected Status status;

	protected abstract void updateTitle(Status status);

	protected MastodonAPIRequest<Status> loadRemoteInfo() {
		String[] parts = status.url.split("/");
		if (parts.length == 0) return null;
		return new GetStatusByID(parts[parts.length - 1]);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		status=Parcels.unwrap(getArguments().getParcelable("status"));
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return base
				.encodedPath(isInstanceAkkoma()
						? "/notice/" + status.id
						: '@' + status.account.acct + '/' + status.id)
				.build();
	}

	@Override
	public String getRemoteDomain() {
		return Uri.parse(status.url).getHost();
	}

	@Override
	public Status getCurrentInfo() {
		return doneWithHomeInstance && remoteInfo != null ? remoteInfo : status;
	}

	@Override
	protected AccountSession getRemoteSession() {
		return Optional.ofNullable(remoteInfo)
				.map(s -> s.account)
				.map(AccountSessionManager.getInstance()::tryGetAccount)
				.orElse(null);
	}

	@Override
	protected void onRemoteInfoLoaded(Status info) {
		super.onRemoteInfoLoaded(info);
		updateTitle(remoteInfo);
	}

	@Override
	protected void onRemoteLoadingFailed() {
		super.onRemoteLoadingFailed();
		setSubtitle(getContext().getString(R.string.sk_no_remote_info_hint, getSession().domain));
		updateToolbar();
	}
}
