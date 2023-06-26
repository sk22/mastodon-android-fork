package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.markers.SaveMarkers;
import org.joinmastodon.android.api.requests.notifications.PleromaMarkNotificationsRead;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.AllNotificationsSeenEvent;
import org.joinmastodon.android.events.PollUpdatedEvent;
import org.joinmastodon.android.events.RemoveAccountPostsEvent;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.CacheablePaginatedResponse;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.PaginatedResponse;
import org.joinmastodon.android.model.TimelineMarkers;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.displayitems.AccountCardStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.ExtendedFooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.FooterStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.HeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.NotificationHeaderStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.TextStatusDisplayItem;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.DiscoverInfoBannerHelper;
import org.joinmastodon.android.ui.utils.InsetStatusItemDecoration;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.utils.ObjectIdComparator;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.api.SimpleCallback;

public class NotificationsListFragment extends BaseStatusListFragment<Notification>{
	private boolean onlyMentions;
	private boolean onlyPosts;
	private String maxID;
	private final DiscoverInfoBannerHelper bannerHelper = new DiscoverInfoBannerHelper(DiscoverInfoBannerHelper.BannerType.POST_NOTIFICATIONS);
	private String unreadMarker, realUnreadMarker;

	@Override
	protected boolean wantsComposeButton() {
		return false;
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		E.register(this);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		onlyMentions=getArguments().getBoolean("onlyMentions", false);
		onlyPosts=getArguments().getBoolean("onlyPosts", false);
	}

	void resetUnreadBackground(){
		unreadMarker=realUnreadMarker;
		list.invalidate();
	}

	@Override
	public void onRefresh(){
		super.onRefresh();
		if (getParentFragment() instanceof NotificationsFragment notificationsFragment) {
			notificationsFragment.refreshFollowRequestsBadge();
		}
		resetUnreadBackground();
		AccountSessionManager.get(accountID).reloadNotificationsMarker(m->{
			unreadMarker=realUnreadMarker=m;
		});
	}

	@Override
	protected List<StatusDisplayItem> buildDisplayItems(Notification n){
		NotificationHeaderStatusDisplayItem titleItem;
		if(n.type==Notification.Type.MENTION || n.type==Notification.Type.STATUS){
			titleItem=null;
		}else{
			titleItem=new NotificationHeaderStatusDisplayItem(n.id, this, n, accountID);
			if(n.status!=null){
				n.status.card=null;
				n.status.spoilerText=null;
			}
		}
		if(n.status!=null){
			int flags=titleItem==null ? 0 : (StatusDisplayItem.FLAG_NO_FOOTER | StatusDisplayItem.FLAG_INSET); // | StatusDisplayItem.FLAG_NO_HEADER);
			ArrayList<StatusDisplayItem> items=StatusDisplayItem.buildItems(this, n.status, accountID, n, knownAccounts, null, flags);
			if(titleItem!=null)
				items.add(0, titleItem);
			return items;
		}else if(titleItem!=null){
			return Collections.singletonList(titleItem);
		}else{
			return Collections.emptyList();
		}
	}

	@Override
	protected void addAccountToKnown(Notification s){
		if(!knownAccounts.containsKey(s.account.id))
			knownAccounts.put(s.account.id, s.account);
		if(s.status!=null && !knownAccounts.containsKey(s.status.account.id))
			knownAccounts.put(s.status.account.id, s.status.account);
		if(s.status!=null && s.status.reblog!=null && !knownAccounts.containsKey(s.status.reblog.account.id))
			knownAccounts.put(s.status.reblog.account.id, s.status.reblog.account);
	}

	@Override
	protected void doLoadData(int offset, int count){
//		endMark.setVisibility(View.GONE);
		AccountSessionManager.getInstance()
				.getAccount(accountID).getCacheController()
				.getNotifications(offset>0 ? maxID : null, count, onlyMentions, onlyPosts, refreshing, new SimpleCallback<>(this){
					@Override
					public void onSuccess(PaginatedResponse<List<Notification>> result){
						if(getActivity()==null)
							return;
						onDataLoaded(result.items.stream().filter(n->n.type!=null).collect(Collectors.toList()), !result.items.isEmpty());
						maxID=result.maxID;
//						endMark.setVisibility(result.items.isEmpty() ? View.VISIBLE : View.GONE);
					}
				});
	}

	/* protected void oldDoLoadData(int offset, int count){
		AccountSessionManager.getInstance()
				.getAccount(accountID).getCacheController()
				.getNotifications(offset>0 ? maxID : null, count, onlyMentions, onlyPosts, refreshing, new SimpleCallback<>(this){
					@Override
					public void onSuccess(PaginatedResponse<List<Notification>> result){
						if (getActivity() == null) return;
						if(refreshing)
							relationships.clear();
						onDataLoaded(result.items.stream().filter(n->n.type!=null).collect(Collectors.toList()), !result.items.isEmpty());
						Set<String> needRelationships=result.items.stream()
								.filter(ntf->ntf.status==null && !relationships.containsKey(ntf.account.id))
								.map(ntf->ntf.account.id)
								.collect(Collectors.toSet());
						loadRelationships(needRelationships);
						maxID=result.maxID;

						// TODO
//						TimelineMarkers markers = AccountSessionManager.getInstance().getAccount(accountID).markers;
//						if(offset==0 && !result.items.isEmpty() && !result.isFromCache() && markers != null && markers.notifications != null){
//							E.post(new AllNotificationsSeenEvent());
//							new SaveMarkers(null, result.items.get(0).id).exec(accountID);
//							AccountSessionManager.getInstance().getAccount(accountID).markers
//									.notifications.lastReadId = result.items.get(0).id;
//							AccountSessionManager.getInstance().writeAccountsFile();
//
//							if (isInstanceAkkoma()) {
//								new PleromaMarkNotificationsRead(result.items.get(0).id).exec(accountID);
//							}
//						}
					}
				});
	} */

	@Override
	protected void onRelationshipsLoaded(){
		if(getActivity()==null)
			return;
		for(int i=0;i<list.getChildCount();i++){
			RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
			if(holder instanceof AccountCardStatusDisplayItem.Holder accountHolder)
				accountHolder.rebind();
		}
	}

	@Override
	protected void onShown(){
		super.onShown();
		unreadMarker=realUnreadMarker=AccountSessionManager.get(accountID).getLastKnownNotificationsMarker();
//		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
//			loadData();
	}

	@Override
	public void onItemClick(String id){
		Notification n=getNotificationByID(id);
		Bundle args = new Bundle();
		if(n.status != null && n.status.inReplyToAccountId != null && knownAccounts.containsKey(n.status.inReplyToAccountId))
			args.putParcelable("inReplyToAccount", Parcels.wrap(knownAccounts.get(n.status.inReplyToAccountId)));
		UiUtils.showFragmentForNotification(getContext(), n, accountID, args);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		list.addItemDecoration(new InsetStatusItemDecoration(this));
		if (onlyPosts) bannerHelper.maybeAddBanner(contentWrap);
	}

	private Notification getNotificationByID(String id){
		for(Notification n:data){
			if(n.id.equals(id))
				return n;
		}
		return null;
	}

	@Subscribe
	public void onPollUpdated(PollUpdatedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		for(Notification ntf:data){
			if(ntf.status==null)
				continue;
			Status contentStatus=ntf.status.getContentStatus();
			if(contentStatus.poll!=null && contentStatus.poll.id.equals(ev.poll.id)){
				updatePoll(ntf.id, ntf.status, ev.poll);
			}
		}
	}

	// copied from StatusListFragment.EventListener (just like the method above)
	// (which assumes this.data to be a list of statuses...)
	@Subscribe
	public void onStatusCountersUpdated(StatusCountersUpdatedEvent ev){
		for(Notification n:data){
			if (n.status == null) continue;
			if(n.status.getContentStatus().id.equals(ev.id)){
				n.status.getContentStatus().update(ev);
				for(int i=0;i<list.getChildCount();i++){
					RecyclerView.ViewHolder holder=list.getChildViewHolder(list.getChildAt(i));
					if(holder instanceof FooterStatusDisplayItem.Holder footer && footer.getItem().status==n.status.getContentStatus()){
						footer.rebind();
					}else if(holder instanceof ExtendedFooterStatusDisplayItem.Holder footer && footer.getItem().status==n.status.getContentStatus()){
						footer.rebind();
					}
				}
			}
		}
		for(Notification n:preloadedData){
			if (n.status == null) continue;
			if(n.status.getContentStatus().id.equals(ev.id)){
				n.status.getContentStatus().update(ev);
			}
		}
	}

	@Subscribe
	public void onRemoveAccountPostsEvent(RemoveAccountPostsEvent ev){
		if(!ev.accountID.equals(accountID) || ev.isUnfollow)
			return;
		List<Notification> toRemove=Stream.concat(data.stream(), preloadedData.stream())
				.filter(n->n.account!=null && n.account.id.equals(ev.postsByAccountID))
				.collect(Collectors.toList());
		for(Notification n:toRemove){
			removeNotification(n);
		}
	}

	public void removeNotification(Notification n){
		data.remove(n);
		preloadedData.remove(n);
		int index=-1;
		for(int i=0;i<displayItems.size();i++){
			if(n.id.equals(displayItems.get(i).parentID)){
				index=i;
				break;
			}
		}
		if(index==-1)
			return;
		int lastIndex;
		for(lastIndex=index;lastIndex<displayItems.size();lastIndex++){
			if(!displayItems.get(lastIndex).parentID.equals(n.id))
				break;
		}
		displayItems.subList(index, lastIndex).clear();
		adapter.notifyItemRangeRemoved(index, lastIndex-index);
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return base.path(isInstanceAkkoma()
				? "/users/" + getSession().self.username + "/interactions"
				: "/notifications").build();
	}

	List<Notification> getData() {
		return data;
	}

	String getRealUnreadMarker() {
		return realUnreadMarker;
	}

	void setRealUnreadMarker(String realUnreadMarker) {
		this.realUnreadMarker = realUnreadMarker;
	}

	@Override
	public void onAppendItems(List<Notification> items){
		super.onAppendItems(items);
		if(data.isEmpty() || data.get(0).id.equals(realUnreadMarker))
			return;
		for(Notification n:items){
			if(ObjectIdComparator.INSTANCE.compare(n.id, realUnreadMarker)<=0
				&& getParentFragment() instanceof NotificationsFragment p){
				p.markAsRead();
				break;
			}
		}
	}
}
