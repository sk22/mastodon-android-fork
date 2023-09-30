package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.res.TypedArray;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.MastodonErrorResponse;
import org.joinmastodon.android.api.requests.tags.GetTag;
import org.joinmastodon.android.api.requests.tags.SetTagFollowed;
import org.joinmastodon.android.api.requests.timelines.GetHashtagTimeline;
import org.joinmastodon.android.model.FilterContext;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.TimelineDefinition;
import org.joinmastodon.android.ui.text.SpacerSpan;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.ProgressBarButton;
import org.parceler.Parcels;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;

public class HashtagTimelineFragment extends PinnableStatusListFragment{
	private Hashtag hashtag;
	private String hashtagName;
	private TextView headerTitle, headerSubtitle;
	private ProgressBarButton followButton;
	private ProgressBar followProgress;
	private MenuItem followMenuItem, pinMenuItem;
	private boolean followRequestRunning;
	private boolean toolbarContentVisible;

	private List<String> any;
	private List<String> all;
	private List<String> none;
	private boolean following;
	private boolean localOnly;
	private Menu optionsMenu;
	private MenuInflater optionsMenuInflater;

	@Override
	protected boolean wantsComposeButton() {
		return true;
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		following=getArguments().getBoolean("following", false);
		localOnly=getArguments().getBoolean("localOnly", false);
		any=getArguments().getStringArrayList("any");
		all=getArguments().getStringArrayList("all");
		none=getArguments().getStringArrayList("none");
		if(getArguments().containsKey("hashtag")){
			hashtag=Parcels.unwrap(getArguments().getParcelable("hashtag"));
			hashtagName=hashtag.name;
		}else{
			hashtagName=getArguments().getString("hashtagName");
		}
		setTitle('#'+hashtagName);
		setHasOptionsMenu(true);
	}

	@Override
	protected TimelineDefinition makeTimelineDefinition() {
		return TimelineDefinition.ofHashtag(hashtag);
	}

	@Override
	protected void doLoadData(int offset, int count){
		currentRequest=new GetHashtagTimeline(hashtagName, offset==0 ? null : getMaxID(), null, count, any, all, none, localOnly, getLocalPrefs().timelineReplyVisibility)
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(List<Status> result){
						onDataLoaded(result, !result.isEmpty());
					}
				})
				.exec(accountID);
	}

	@Override
	protected void onShown(){
		super.onShown();
		if(!getArguments().getBoolean("noAutoLoad") && !loaded && !dataLoading)
			loadData();
	}

	@Override
	public void loadData(){
		reloadTag();
		super.loadData();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		fab=view.findViewById(R.id.fab);
		fab.setOnClickListener(this::onFabClick);

		if(getParentFragment() instanceof HomeTabFragment) return;

		list.addOnScrollListener(new RecyclerView.OnScrollListener(){
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
				View topChild=recyclerView.getChildAt(0);
				int firstChildPos=recyclerView.getChildAdapterPosition(topChild);
				float newAlpha=firstChildPos>0 ? 1f : Math.min(1f, -topChild.getTop()/(float)headerTitle.getHeight());
				toolbarTitleView.setAlpha(newAlpha);
				boolean newToolbarVisibility=newAlpha>0.5f;
				if(newToolbarVisibility!=toolbarContentVisible){
					toolbarContentVisible=newToolbarVisibility;
					createOptionsMenu();
				}
			}
		});
	}

	@Override
	public void onFabClick(View v){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		args.putString("prefilledText", '#'+hashtagName+' ');
		Nav.go(getActivity(), ComposeFragment.class, args);
	}

	@Override
	protected void onSetFabBottomInset(int inset){
		((ViewGroup.MarginLayoutParams) fab.getLayoutParams()).bottomMargin=V.dp(16)+inset;
	}

	@Override
	protected FilterContext getFilterContext() {
		return FilterContext.PUBLIC;
	}

	@Override
	public Uri getWebUri(Uri.Builder base) {
		return base.path((isInstanceAkkoma() ? "/tag/" : "/tags/") + hashtag).build();
	}

	@Override
	protected RecyclerView.Adapter getAdapter(){
		View header=getActivity().getLayoutInflater().inflate(R.layout.header_hashtag_timeline, list, false);
		headerTitle=header.findViewById(R.id.title);
		headerSubtitle=header.findViewById(R.id.subtitle);
		followButton=header.findViewById(R.id.profile_action_btn);
		followProgress=header.findViewById(R.id.action_progress);

		headerTitle.setText("#"+hashtagName);
		followButton.setVisibility(View.GONE);
		followButton.setOnClickListener(v->{
			if(hashtag==null)
				return;
			setFollowed(!hashtag.following);
		});
		followButton.setOnLongClickListener(v->{
			if(hashtag==null) return false;
			UiUtils.pickAccount(getActivity(), accountID, R.string.sk_follow_as, R.drawable.ic_fluent_person_add_28_regular, session -> {
				new SetTagFollowed(hashtagName, true).setCallback(new Callback<>(){
					@Override
					public void onSuccess(Hashtag hashtag) {
						Toast.makeText(
								getActivity(),
								getString(R.string.sk_followed_as, session.self.getShortUsername()),
								Toast.LENGTH_SHORT
						).show();
					}

					@Override
					public void onError(ErrorResponse error) {
						error.showToast(getActivity());
					}
				}).exec(session.getID());
			}, null);
			return true;
		});
		updateHeader();

		MergeRecyclerAdapter mergeAdapter=new MergeRecyclerAdapter();
		if(!(getParentFragment() instanceof HomeTabFragment)){
			mergeAdapter.addAdapter(new SingleViewRecyclerAdapter(header));
		}
		mergeAdapter.addAdapter(super.getAdapter());
		return mergeAdapter;
	}

	@Override
	protected int getMainAdapterOffset(){
		return 1;
	}

	private void createOptionsMenu(){
		optionsMenu.clear();
		optionsMenuInflater.inflate(R.menu.hashtag_timeline, optionsMenu);
		followMenuItem=optionsMenu.findItem(R.id.follow_hashtag);
		pinMenuItem=optionsMenu.findItem(R.id.pin);
		followMenuItem.setVisible(toolbarContentVisible);
		followMenuItem.setTitle(getString(hashtag.following ? R.string.unfollow_user : R.string.follow_user, "#"+hashtagName));
		followMenuItem.setIcon(hashtag.following ? R.drawable.ic_fluent_person_delete_24_filled : R.drawable.ic_fluent_person_add_24_regular);
		pinMenuItem.setShowAsAction(toolbarContentVisible ? MenuItem.SHOW_AS_ACTION_NEVER : MenuItem.SHOW_AS_ACTION_ALWAYS);
		super.updatePinButton(pinMenuItem);
		if(toolbarContentVisible){
			UiUtils.enableOptionsMenuIcons(getContext(), optionsMenu);
		}else{
			UiUtils.enableOptionsMenuIcons(getContext(), optionsMenu, R.id.pin);
		}
	}

	@Override
	public void updatePinButton(MenuItem pin){
		super.updatePinButton(pin);
		if(toolbarContentVisible) UiUtils.insetPopupMenuIcon(getContext(), pin);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.hashtag_timeline, menu);
		super.onCreateOptionsMenu(menu, inflater);
		optionsMenu=menu;
		optionsMenuInflater=inflater;
		createOptionsMenu();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		if (super.onOptionsItemSelected(item)) return true;
		if (item.getItemId() == R.id.follow_hashtag && hashtag!=null) {
			setFollowed(!hashtag.following);
		}
		return true;
	}

	@Override
	protected void onUpdateToolbar(){
		super.onUpdateToolbar();
		toolbarTitleView.setAlpha(toolbarContentVisible ? 1f : 0f);
		createOptionsMenu();
	}

	private void updateHeader(){
		if(hashtag==null)
			return;

		if(hashtag.history!=null && !hashtag.history.isEmpty()){
			int weekPosts=hashtag.history.stream().mapToInt(h->h.uses).sum();
			int todayPosts=hashtag.history.get(0).uses;
			int numAccounts=hashtag.history.stream().mapToInt(h->h.accounts).sum();
			int hSpace=V.dp(8);
			SpannableStringBuilder ssb=new SpannableStringBuilder();
			ssb.append(getResources().getQuantityString(R.plurals.x_posts, weekPosts, weekPosts));
			ssb.append(" ", new SpacerSpan(hSpace, 0), 0);
			ssb.append('·');
			ssb.append(" ", new SpacerSpan(hSpace, 0), 0);
			ssb.append(getResources().getQuantityString(R.plurals.x_participants, numAccounts, numAccounts));
			ssb.append(" ", new SpacerSpan(hSpace, 0), 0);
			ssb.append('·');
			ssb.append(" ", new SpacerSpan(hSpace, 0), 0);
			ssb.append(getResources().getQuantityString(R.plurals.x_posts_today, todayPosts, todayPosts));
			headerSubtitle.setText(ssb);
		}

		int styleRes;
		followButton.setVisibility(View.VISIBLE);
		if(hashtag.following){
			followButton.setText(R.string.button_following);
			styleRes=R.style.Widget_Mastodon_M3_Button_Tonal;
		}else{
			followButton.setText(R.string.button_follow);
			styleRes=R.style.Widget_Mastodon_M3_Button_Filled;
		}
		TypedArray ta=followButton.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.background});
		followButton.setBackground(ta.getDrawable(0));
		ta.recycle();
		ta=followButton.getContext().obtainStyledAttributes(styleRes, new int[]{android.R.attr.textColor});
		followButton.setTextColor(ta.getColorStateList(0));
		followProgress.setIndeterminateTintList(ta.getColorStateList(0));
		ta.recycle();

		followButton.setTextVisible(true);
		followProgress.setVisibility(View.GONE);
		if(followMenuItem!=null){
			followMenuItem.setTitle(getString(hashtag.following ? R.string.unfollow_user : R.string.follow_user, "#"+hashtagName));
			followMenuItem.setIcon(hashtag.following ? R.drawable.ic_fluent_person_delete_24_filled : R.drawable.ic_fluent_person_add_24_regular);
			UiUtils.insetPopupMenuIcon(getContext(), followMenuItem);
		}
	}

	private void reloadTag(){
		new GetTag(hashtagName)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Hashtag result){
						hashtag=result;
						updateHeader();
					}

					@Override
					public void onError(ErrorResponse error){

					}
				})
				.exec(accountID);
	}

	private void setFollowed(boolean followed){
		if(followRequestRunning)
			return;
		getToolbar().performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
		followButton.setTextVisible(false);
		followProgress.setVisibility(View.VISIBLE);
		followRequestRunning=true;
		new SetTagFollowed(hashtagName, followed)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Hashtag result){
						if(getActivity()==null)
							return;
						hashtag=result;
						updateHeader();
						followRequestRunning=false;
					}

					@Override
					public void onError(ErrorResponse error){
						if(getActivity()==null)
							return;
						if(error instanceof MastodonErrorResponse er && "Duplicate record".equals(er.error)){
							hashtag.following=true;
						}else{
							error.showToast(getActivity());
						}
						updateHeader();
						followRequestRunning=false;
					}
				})
				.exec(accountID);
	}
}
