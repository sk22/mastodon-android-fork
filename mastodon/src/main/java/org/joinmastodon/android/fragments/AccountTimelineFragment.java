package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountStatuses;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.events.StatusUnpinnedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Filter;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.utils.StatusFilterPredicate;
import org.parceler.Parcels;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import me.grishka.appkit.api.SimpleCallback;

public class AccountTimelineFragment extends StatusListFragment{
	private Account user;
	private GetAccountStatuses.Filter filter;

	public AccountTimelineFragment(){
		setListLayoutId(R.layout.recycler_fragment_no_refresh);
	}

	public static AccountTimelineFragment newInstance(String accountID, Account profileAccount, GetAccountStatuses.Filter filter, boolean load){
		AccountTimelineFragment f=new AccountTimelineFragment();
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putParcelable("profileAccount", Parcels.wrap(profileAccount));
		args.putString("filter", filter.toString());
		if(!load)
			args.putBoolean("noAutoLoad", true);
		args.putBoolean("__is_tab", true);
		f.setArguments(args);
		return f;
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		user=Parcels.unwrap(getArguments().getParcelable("profileAccount"));
		filter=GetAccountStatuses.Filter.valueOf(getArguments().getString("filter"));
	}

	@Override
	protected void doLoadData(int offset, int count){
		if(user==null) // TODO figure out why this happens
			return;
		currentRequest=new GetAccountStatuses(user.id, offset>0 ? getMaxID() : null, null, count, filter)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						if(getActivity()==null) return;
						AccountSessionManager asm = AccountSessionManager.getInstance();
						result=result.stream().filter(status -> {
							// don't hide own posts in own profile
							if (asm.isSelf(accountID, user) && asm.isSelf(accountID, status.account)) return true;
							else return new StatusFilterPredicate(accountID, getFilterContext()).test(status);
						}).collect(Collectors.toList());
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}

	protected void onStatusCreated(StatusCreatedEvent ev){
		AccountSessionManager asm = AccountSessionManager.getInstance();
		if(!asm.isSelf(accountID, ev.status.account) || !asm.isSelf(accountID, user))
			return;
		if(filter==GetAccountStatuses.Filter.PINNED) return;
		if(filter==GetAccountStatuses.Filter.DEFAULT){
			// Keep replies to self, discard all other replies
			if(ev.status.inReplyToAccountId!=null && !ev.status.inReplyToAccountId.equals(AccountSessionManager.getInstance().getAccount(accountID).self.id))
				return;
		}else if(filter==GetAccountStatuses.Filter.MEDIA){
			if(Optional.ofNullable(ev.status.mediaAttachments).map(List::isEmpty).orElse(true))
				return;
		}
		prependItems(Collections.singletonList(ev.status), true);
		if (isOnTop()) scrollToTop();
	}

	protected void onStatusUnpinned(StatusUnpinnedEvent ev){
		if(!ev.accountID.equals(accountID) || filter!=GetAccountStatuses.Filter.PINNED)
			return;

		Status status=getStatusByID(ev.id);
		data.remove(status);
		preloadedData.remove(status);
		HeaderStatusDisplayItem item=findItemOfType(ev.id, HeaderStatusDisplayItem.class);
		if(item==null)
			return;
		int index=displayItems.indexOf(item);
		int lastIndex;
		for(lastIndex=index;lastIndex<displayItems.size();lastIndex++){
			if(!displayItems.get(lastIndex).parentID.equals(ev.id))
				break;
		}
		displayItems.subList(index, lastIndex).clear();
		adapter.notifyItemRangeRemoved(index, lastIndex-index);
	}

	@Override
	protected void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
		// no-op
	}


	@Override
	protected FilterContext getFilterContext() {
		return FilterContext.ACCOUNT;
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		// could return different uris based on filter (e.g. media -> "/media"), but i want to
		// return the remote url to the user, and i don't know whether i'd need to append
		// '#media' (akkoma/pleroma) or '/media' (glitch/mastodon) since i don't know anything
		// about the remote instance. so, just returning the base url to the user instead
		return Uri.parse(user.url);
	}
}
