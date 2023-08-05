package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.api.requests.markers.SaveMarkers;
import org.joinmastodon.android.api.requests.timelines.GetHomeTimeline;
import org.joinmastodon.android.api.session.AccountLocalPreferences;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.StatusCreatedEvent;
import org.joinmastodon.android.model.CacheablePaginatedResponse;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.TimelineMarkers;
import org.joinmastodon.android.ui.displayitems.GapStatusDisplayItem;
import org.joinmastodon.android.ui.displayitems.StatusDisplayItem;
import org.joinmastodon.android.utils.StatusFilterPredicate;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.V;

public class HomeTimelineFragment extends StatusListFragment {
	private HomeTabFragment parent;
	private String maxID;
	private String lastSavedMarkerID;

	@Override
	protected boolean wantsComposeButton() {
		return true;
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		if (getParentFragment() instanceof HomeTabFragment home) parent = home;
		loadData();
	}

	private boolean typeFilterPredicate(Status s) {
		AccountLocalPreferences lp=getLocalPrefs();
		return (lp.showReplies || s.inReplyToId == null) &&
				(lp.showBoosts || s.reblog == null);
	}

	private List<Status> filterPosts(List<Status> items) {
		return items.stream().filter(this::typeFilterPredicate).collect(Collectors.toList());
	}

	@Override
	protected void doLoadData(int offset, int count){
		AccountSessionManager.getInstance()
				.getAccount(accountID).getCacheController()
				.getHomeTimeline(offset>0 ? maxID : null, count, refreshing, new SimpleCallback<>(this){
					@Override
					public void onSuccess(CacheablePaginatedResponse<List<Status>> result){
						if (getActivity() == null) return;
						List<Status> filteredItems = filterPosts(result.items);
						maxID=result.maxID;
						onDataLoaded(filteredItems, !result.items.isEmpty());
						if(result.isFromCache())
							loadNewPosts();
					}
				});
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);

		list.addOnScrollListener(new RecyclerView.OnScrollListener(){
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
				if(parent != null && parent.isNewPostsBtnShown() && list.getChildAdapterPosition(list.getChildAt(0))<=getMainAdapterOffset()){
					parent.hideNewPostsButton();
				}
			}
		});
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad")){
			if(!loaded && !dataLoading){
				loadData();
			}else if(!dataLoading){
				loadNewPosts();
			}
		}
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		if(!data.isEmpty()){
			String topPostID=displayItems.get(Math.max(0, list.getChildAdapterPosition(list.getChildAt(0))-getMainAdapterOffset())).parentID;
			if(!topPostID.equals(lastSavedMarkerID)){
				lastSavedMarkerID=topPostID;
				new SaveMarkers(topPostID, null)
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(TimelineMarkers result){
							}

							@Override
							public void onError(ErrorResponse error){
								lastSavedMarkerID=null;
							}
						})
						.exec(accountID);
			}
		}
	}

	public void onStatusCreated(Status status){
		prependItems(Collections.singletonList(status), true);
	}

	private void loadNewPosts(){
		if (!GlobalUserPreferences.loadNewPosts) return;
		dataLoading=true;
		// The idea here is that we request the timeline such that if there are fewer than `limit` posts,
		// we'll get the currently topmost post as last in the response. This way we know there's no gap
		// between the existing and newly loaded parts of the timeline.
		String sinceID=data.size()>1 ? data.get(1).id : "1";
		currentRequest=new GetHomeTimeline(null, null, 20, sinceID, getLocalPrefs().timelineReplyVisibility)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Status> result){
						currentRequest=null;
						dataLoading=false;
						result = filterPosts(result);
						if(result.isEmpty() || getActivity()==null)
							return;
						Status last=result.get(result.size()-1);
						List<Status> toAdd;
						if(!data.isEmpty() && last.id.equals(data.get(0).id)){ // This part intersects with the existing one
							toAdd=result.subList(0, result.size()-1); // Remove the already known last post
						}else{
							result.get(result.size()-1).hasGapAfter=true;
							toAdd=result;
						}
						AccountSessionManager.get(accountID).filterStatuses(toAdd, getFilterContext());
						if(!toAdd.isEmpty()){
							prependItems(toAdd, true);
							if (parent != null && GlobalUserPreferences.showNewPostsButton) parent.showNewPostsButton();
							AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putHomeTimeline(toAdd, false);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequest=null;
						dataLoading=false;
					}
				})
				.exec(accountID);

		if (parent.getParentFragment() instanceof HomeFragment homeFragment) {
			homeFragment.reloadNotificationsForUnreadCount();
		}
	}

	@Override
	public void onGapClick(GapStatusDisplayItem.Holder item){
		if(dataLoading)
			return;
		item.getItem().loading=true;
		V.setVisibilityAnimated(item.progress, View.VISIBLE);
		V.setVisibilityAnimated(item.text, View.GONE);
		GapStatusDisplayItem gap=item.getItem();
		dataLoading=true;
		currentRequest=new GetHomeTimeline(item.getItemID(), null, 20, null, getLocalPrefs().timelineReplyVisibility)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(List<Status> result){
						currentRequest=null;
						dataLoading=false;
						if(getActivity()==null)
							return;
						int gapPos=displayItems.indexOf(gap);
						if(gapPos==-1)
							return;
						if(result.isEmpty()){
							displayItems.remove(gapPos);
							adapter.notifyItemRemoved(getMainAdapterOffset()+gapPos);
							Status gapStatus=getStatusByID(gap.parentID);
							if(gapStatus!=null){
								gapStatus.hasGapAfter=false;
								AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putHomeTimeline(Collections.singletonList(gapStatus), false);
							}
						}else{
							Set<String> idsBelowGap=new HashSet<>();
							boolean belowGap=false;
							int gapPostIndex=0;
							for(Status s:data){
								if(belowGap){
									idsBelowGap.add(s.id);
								}else if(s.id.equals(gap.parentID)){
									belowGap=true;
									s.hasGapAfter=false;
									AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putHomeTimeline(Collections.singletonList(s), false);
								}else{
									gapPostIndex++;
								}
							}
							int endIndex=0;
							for(Status s:result){
								endIndex++;
								if(idsBelowGap.contains(s.id))
									break;
							}
							if(endIndex==result.size()){
								result.get(result.size()-1).hasGapAfter=true;
							}else{
								result=result.subList(0, endIndex);
							}
							List<StatusDisplayItem> targetList=displayItems.subList(gapPos, gapPos+1);
							targetList.clear();
							List<Status> insertedPosts=data.subList(gapPostIndex+1, gapPostIndex+1);
							StatusFilterPredicate filterPredicate=new StatusFilterPredicate(accountID, getFilterContext());
							for(Status s:result){
								if(idsBelowGap.contains(s.id))
									break;
								if(typeFilterPredicate(s) && filterPredicate.test(s)){
									targetList.addAll(buildDisplayItems(s));
									insertedPosts.add(s);
								}
							}
							AccountSessionManager.get(accountID).filterStatuses(insertedPosts, getFilterContext());
							if(targetList.isEmpty()){
								// oops. We didn't add new posts, but at least we know there are none.
								adapter.notifyItemRemoved(getMainAdapterOffset()+gapPos);
							}else{
								adapter.notifyItemChanged(getMainAdapterOffset()+gapPos);
								adapter.notifyItemRangeInserted(getMainAdapterOffset()+gapPos+1, targetList.size()-1);
							}
							AccountSessionManager.getInstance().getAccount(accountID).getCacheController().putHomeTimeline(insertedPosts, false);
						}
					}

					@Override
					public void onError(ErrorResponse error){
						currentRequest=null;
						dataLoading=false;
						gap.loading=false;
						Activity a=getActivity();
						if(a!=null){
							error.showToast(a);
							int gapPos=displayItems.indexOf(gap);
							if(gapPos>=0)
								adapter.notifyItemChanged(gapPos);
						}
					}
				})
				.exec(accountID);

	}

	@Override
	public void onRefresh(){
		if(currentRequest!=null){
			currentRequest.cancel();
			currentRequest=null;
			dataLoading=false;
		}
		if (parent != null) parent.hideNewPostsButton();
		super.onRefresh();
	}

	@Override
	protected boolean shouldRemoveAccountPostsWhenUnfollowing(){
		return true;
	}

	@Override
	protected FilterContext getFilterContext() {
		return FilterContext.HOME;
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return base.path("/").build();
	}
}
