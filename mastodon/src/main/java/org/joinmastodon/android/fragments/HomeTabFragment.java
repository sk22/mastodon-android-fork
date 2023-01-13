package org.joinmastodon.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.announcements.GetAnnouncements;
import org.joinmastodon.android.api.requests.lists.GetLists;
import org.joinmastodon.android.api.requests.tags.GetFollowedHashtags;
import org.joinmastodon.android.events.SelfUpdateStateChangedEvent;
import org.joinmastodon.android.fragments.discover.FederatedTimelineFragment;
import org.joinmastodon.android.fragments.discover.LocalTimelineFragment;
import org.joinmastodon.android.model.Announcement;
import org.joinmastodon.android.model.Hashtag;
import org.joinmastodon.android.model.HeaderPaginationList;
import org.joinmastodon.android.model.ListTimeline;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class HomeTabFragment extends MastodonToolbarFragment implements ScrollableToTop, OnBackPressedListener {
	private static final int ANNOUNCEMENTS_RESULT = 654;

	private String accountID;
	private MenuItem announcements;
//	private ImageView toolbarLogo;
	private Button toolbarShowNewPostsBtn;
	private boolean newPostsBtnShown;
	private AnimatorSet currentNewPostsAnim;
	private ViewPager2 pager;
	private final List<Fragment> fragments = new ArrayList<>();
	private final List<FrameLayout> tabViews = new ArrayList<>();
	private FrameLayout toolbarFrame;
	private ImageView timelineIcon;
	private ImageView collapsedChevron;
	private TextView timelineTitle;
	private PopupMenu switcherPopup;
	private final Map<Integer, ListTimeline> listItems = new HashMap<>();
	private final Map<Integer, Hashtag> hashtagsItems = new HashMap<>();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		accountID = getArguments().getString("account");
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
		FrameLayout view = new FrameLayout(getContext());
		pager = new ViewPager2(getContext());
		toolbarFrame = (FrameLayout) LayoutInflater.from(getContext()).inflate(R.layout.home_toolbar, getToolbar(), false);

		if (fragments.size() == 0) {
			Bundle args = new Bundle();
			args.putString("account", accountID);
			args.putBoolean("__is_tab", true);

			fragments.add(new HomeTimelineFragment());
			fragments.add(new LocalTimelineFragment());
			if (GlobalUserPreferences.showFederatedTimeline) fragments.add(new FederatedTimelineFragment());

			FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
			for (int i = 0; i < fragments.size(); i++) {
				fragments.get(i).setArguments(args);
				FrameLayout tabView = new FrameLayout(getActivity());
				tabView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				tabView.setVisibility(View.GONE);
				tabView.setId(i + 1);
				transaction.add(i + 1, fragments.get(i));
				view.addView(tabView);
				tabViews.add(tabView);
			}
			transaction.commit();
		}

		view.addView(pager, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		return view;
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);

		timelineIcon = toolbarFrame.findViewById(R.id.timeline_icon);
		timelineTitle = toolbarFrame.findViewById(R.id.timeline_title);
		collapsedChevron = toolbarFrame.findViewById(R.id.collapsed_chevron);
		View switcher = toolbarFrame.findViewById(R.id.switcher_btn);
		switcherPopup = new PopupMenu(getContext(), switcher);
		switcherPopup.inflate(R.menu.home_switcher);
		switcherPopup.setOnMenuItemClickListener(this::onSwitcherItemSelected);
		UiUtils.enablePopupMenuIcons(getContext(), switcherPopup);
		switcher.setOnClickListener(v->{
			updateSwitcherMenu();
			switcherPopup.show();
		});
		View.OnTouchListener listener = switcherPopup.getDragToOpenListener();
		switcher.setOnTouchListener((v, m)-> {
			updateSwitcherMenu();
			return listener.onTouch(v, m);
		});

		UiUtils.reduceSwipeSensitivity(pager);
		pager.setUserInputEnabled(!GlobalUserPreferences.disableSwipe);
		pager.setAdapter(new HomePagerAdapter());
		pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
			@Override
			public void onPageSelected(int position){
				updateSwitcherIcon(position);
				if (position==0) return;
				hideNewPostsButton();
				if (fragments.get(position) instanceof BaseRecyclerFragment<?> page){
					if(!page.loaded && !page.isDataLoading()) page.loadData();
				}
			}
		});

		updateToolbarLogo();

		if(GithubSelfUpdater.needSelfUpdating()){
			E.register(this);
			updateUpdateState(GithubSelfUpdater.getInstance().getState());
		}

		new GetLists().setCallback(new Callback<>() {
			@Override
			public void onSuccess(List<ListTimeline> lists) {
				addListsToSwitcher(lists);
			}

			@Override
			public void onError(ErrorResponse error) {
				error.showToast(getContext());
			}
		}).exec(accountID);

		new GetFollowedHashtags().setCallback(new Callback<>() {
			@Override
			public void onSuccess(HeaderPaginationList<Hashtag> hashtags) {
				addHashtagsToSwitcher(hashtags);
			}

			@Override
			public void onError(ErrorResponse error) {
				error.showToast(getContext());
			}
		}).exec(accountID);
	}

	public void updateToolbarLogo(){
		Toolbar toolbar = getToolbar();
		ViewParent parentView = toolbarFrame.getParent();
		if (parentView == toolbar) return;
		if (parentView instanceof Toolbar parentToolbar) parentToolbar.removeView(toolbarFrame);
		toolbar.addView(toolbarFrame, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		toolbar.setOnClickListener(v->scrollToTop());
		toolbar.setNavigationContentDescription(R.string.back);
		toolbar.setContentInsetsAbsolute(0, toolbar.getContentInsetRight());

		updateSwitcherIcon(pager.getCurrentItem());

//		toolbarLogo=new ImageView(getActivity());
//		toolbarLogo.setScaleType(ImageView.ScaleType.CENTER);
//		toolbarLogo.setImageResource(R.drawable.logo);
//		toolbarLogo.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary)));

		toolbarShowNewPostsBtn=toolbarFrame.findViewById(R.id.show_new_posts_btn);
		toolbarShowNewPostsBtn.setCompoundDrawableTintList(toolbarShowNewPostsBtn.getTextColors());
		if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N) UiUtils.fixCompoundDrawableTintOnAndroid6(toolbarShowNewPostsBtn);
		toolbarShowNewPostsBtn.setOnClickListener(this::onNewPostsBtnClick);

		toolbar.post(() -> {
			// toolbar frame goes from screen edge to beginning of right-aligned option buttons.
			// centering button by applying the same space on the left
			int padding = toolbar.getWidth() - toolbarFrame.getWidth();
			((FrameLayout) toolbarShowNewPostsBtn.getParent()).setPadding(padding, 0, 0, 0);
		});

		if(newPostsBtnShown){
			toolbarShowNewPostsBtn.setVisibility(View.VISIBLE);
			collapsedChevron.setVisibility(View.VISIBLE);
			collapsedChevron.setAlpha(1f);
			timelineTitle.setVisibility(View.GONE);
			timelineTitle.setAlpha(0f);
		}else{
			toolbarShowNewPostsBtn.setVisibility(View.INVISIBLE);
			toolbarShowNewPostsBtn.setAlpha(0f);
			collapsedChevron.setVisibility(View.GONE);
			collapsedChevron.setAlpha(0f);
			toolbarShowNewPostsBtn.setScaleX(.8f);
			toolbarShowNewPostsBtn.setScaleY(.8f);
			timelineTitle.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.home, menu);
		announcements = menu.findItem(R.id.announcements);

		new GetAnnouncements(false).setCallback(new Callback<>() {
			@Override
			public void onSuccess(List<Announcement> result) {
				boolean hasUnread = result.stream().anyMatch(a -> !a.read);
				announcements.setIcon(hasUnread ? R.drawable.ic_announcements_24_badged : R.drawable.ic_fluent_megaphone_24_regular);
			}

			@Override
			public void onError(ErrorResponse error) {
				error.showToast(getActivity());
			}
		}).exec(accountID);
	}

	private void addListsToSwitcher(List<ListTimeline> lists) {
		if (lists.size() == 0) return;
		for (int i = 0; i < lists.size(); i++) {
			ListTimeline list = lists.get(i);
			int id = View.generateViewId();
			listItems.put(id, list);
		}
		updateSwitcherMenu();
	}

	private void addHashtagsToSwitcher(List<Hashtag> hashtags) {
		if (hashtags.size() == 0) return;
		for (int i = 0; i < hashtags.size(); i++) {
			Hashtag tag = hashtags.get(i);
			int id = View.generateViewId();
			hashtagsItems.put(id, tag);
		}
		updateSwitcherMenu();
	}

	private void updateSwitcherMenu() {
		Context context = getContext();
		switcherPopup.getMenu().findItem(R.id.federated).setVisible(GlobalUserPreferences.showFederatedTimeline);

		if (!listItems.isEmpty()) {
			MenuItem listsItem = switcherPopup.getMenu().findItem(R.id.lists);
			listsItem.setVisible(true);
			SubMenu listsMenu = listsItem.getSubMenu();
			listsMenu.clear();
			listItems.forEach((id, list) -> {
				MenuItem item = listsMenu.add(Menu.NONE, id, Menu.NONE, list.title);
				item.setIcon(R.drawable.ic_fluent_people_list_24_regular);
				UiUtils.insetPopupMenuIcon(context, item);
			});
		}

		if (!hashtagsItems.isEmpty()) {
			MenuItem hashtagsItem = switcherPopup.getMenu().findItem(R.id.followed_hashtags);
			hashtagsItem.setVisible(true);
			SubMenu hashtagsMenu = hashtagsItem.getSubMenu();
			hashtagsMenu.clear();
			hashtagsItems.forEach((id, hashtag) -> {
				MenuItem item = hashtagsMenu.add(Menu.NONE, id, Menu.NONE, hashtag.name);
				item.setIcon(R.drawable.ic_fluent_number_symbol_24_regular);
				UiUtils.insetPopupMenuIcon(context, item);
			});
		}
	}

	private boolean onSwitcherItemSelected(MenuItem item) {
		int id = item.getItemId();
		ListTimeline list;
		Hashtag hashtag;
		if (id == R.id.home) {
			navigateTo(0);
			return true;
		} else if (id == R.id.local) {
			navigateTo(1);
			return true;
		} else if (id == R.id.federated) {
			navigateTo(2);
			return true;
		} else if ((list = listItems.get(id)) != null) {
			Bundle args = new Bundle();
			args.putString("account", accountID);
			args.putString("listID", list.id);
			args.putString("listTitle", list.title);
			args.putInt("repliesPolicy", list.repliesPolicy.ordinal());
			Nav.go(getActivity(), ListTimelineFragment.class, args);
		} else if ((hashtag = hashtagsItems.get(id)) != null) {
			UiUtils.openHashtagTimeline(getActivity(), accountID, hashtag.name, hashtag.following);
		}
		return false;
	}

	private void navigateTo(int i) {
		pager.setCurrentItem(i);
		updateSwitcherIcon(i);
	}

	private void updateSwitcherIcon(int i) {
		timelineIcon.setImageResource(switch (i) {
			default -> R.drawable.ic_fluent_home_24_regular;
			case 1 -> R.drawable.ic_fluent_people_community_24_regular;
			case 2 -> R.drawable.ic_fluent_earth_24_regular;
		});
		timelineTitle.setText(switch (i) {
			default -> R.string.sk_timeline_home;
			case 1 -> R.string.sk_timeline_local;
			case 2 -> R.string.sk_timeline_federated;
		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		Bundle args=new Bundle();
		args.putString("account", accountID);
		if (item.getItemId() == R.id.settings) Nav.go(getActivity(), SettingsFragment.class, args);
		if (item.getItemId() == R.id.announcements) {
			Nav.goForResult(getActivity(), AnnouncementsFragment.class, args, ANNOUNCEMENTS_RESULT, this);
		}
		return true;
	}

	@Override
	public void scrollToTop(){
		((ScrollableToTop) fragments.get(pager.getCurrentItem())).scrollToTop();
	}

	public void hideNewPostsButton(){
		if(!newPostsBtnShown)
			return;
		newPostsBtnShown=false;
		if(currentNewPostsAnim!=null){
			currentNewPostsAnim.cancel();
		}
		timelineTitle.setVisibility(View.VISIBLE);
		AnimatorSet set=new AnimatorSet();
		set.playTogether(
				ObjectAnimator.ofFloat(timelineTitle, View.ALPHA, 1f),
				ObjectAnimator.ofFloat(timelineTitle, View.SCALE_X, 1f),
				ObjectAnimator.ofFloat(timelineTitle, View.SCALE_Y, 1f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.ALPHA, 0f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_X, .8f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_Y, .8f),
				ObjectAnimator.ofFloat(collapsedChevron, View.ALPHA, 0f),
				ObjectAnimator.ofFloat(collapsedChevron, View.SCALE_X, .8f),
				ObjectAnimator.ofFloat(collapsedChevron, View.SCALE_Y, .8f)
		);
		set.setDuration(GlobalUserPreferences.reduceMotion ? 0 : 300);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				toolbarShowNewPostsBtn.setVisibility(View.INVISIBLE);
				collapsedChevron.setVisibility(View.GONE);
				currentNewPostsAnim=null;
			}
		});
		currentNewPostsAnim=set;
		set.start();
	}

	public void showNewPostsButton(){
		if(newPostsBtnShown)
			return;
		newPostsBtnShown=true;
		if(currentNewPostsAnim!=null){
			currentNewPostsAnim.cancel();
		}
		toolbarShowNewPostsBtn.setVisibility(View.VISIBLE);
		collapsedChevron.setVisibility(View.VISIBLE);
		AnimatorSet set=new AnimatorSet();
		set.playTogether(
				ObjectAnimator.ofFloat(timelineTitle, View.ALPHA, 0f),
				ObjectAnimator.ofFloat(timelineTitle, View.SCALE_X, .8f),
				ObjectAnimator.ofFloat(timelineTitle, View.SCALE_Y, .8f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.ALPHA, 1f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_X, 1f),
				ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_Y, 1f),
				ObjectAnimator.ofFloat(collapsedChevron, View.ALPHA, 1f),
				ObjectAnimator.ofFloat(collapsedChevron, View.SCALE_X, 1f),
				ObjectAnimator.ofFloat(collapsedChevron, View.SCALE_Y, 1f)
		);
		set.setDuration(GlobalUserPreferences.reduceMotion ? 0 : 300);
		set.setInterpolator(CubicBezierInterpolator.DEFAULT);
		set.addListener(new AnimatorListenerAdapter(){
			@Override
			public void onAnimationEnd(Animator animation){
				timelineTitle.setVisibility(View.GONE);
				currentNewPostsAnim=null;
			}
		});
		currentNewPostsAnim=set;
		set.start();
	}

	public boolean isNewPostsBtnShown() {
		return newPostsBtnShown;
	}

	private void onNewPostsBtnClick(View view) {
		if(newPostsBtnShown){
			hideNewPostsButton();
			scrollToTop();
		}
	}

	@Override
	public void onFragmentResult(int reqCode, boolean noMoreUnread, Bundle result){
		if (reqCode == ANNOUNCEMENTS_RESULT && noMoreUnread) {
			announcements.setIcon(R.drawable.ic_fluent_megaphone_24_regular);
		}
	}

	private void updateUpdateState(GithubSelfUpdater.UpdateState state){
		if(state!=GithubSelfUpdater.UpdateState.NO_UPDATE && state!=GithubSelfUpdater.UpdateState.CHECKING)
			getToolbar().getMenu().findItem(R.id.settings).setIcon(R.drawable.ic_settings_24_badged);
	}

	@Subscribe
	public void onSelfUpdateStateChanged(SelfUpdateStateChangedEvent ev){
		updateUpdateState(ev.state);
	}

	@Override
	public boolean onBackPressed(){
		if(pager.getCurrentItem() > 0){
			navigateTo(0);
			return true;
		}
		return false;
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();
		if(GithubSelfUpdater.needSelfUpdating()){
			E.unregister(this);
		}
	}

	private class HomePagerAdapter extends RecyclerView.Adapter<SimpleViewHolder> {
		@NonNull
		@Override
		public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			FrameLayout tabView = tabViews.get(viewType % getItemCount());
			((ViewGroup)tabView.getParent()).removeView(tabView);
			tabView.setVisibility(View.VISIBLE);
			return new SimpleViewHolder(tabView);
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){}

		@Override
		public int getItemCount(){
			return fragments.size();
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}
}
