package org.joinmastodon.android.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.announcements.GetAnnouncements;
import org.joinmastodon.android.events.SelfUpdateStateChangedEvent;
import org.joinmastodon.android.fragments.discover.DiscoverPostsFragment;
import org.joinmastodon.android.fragments.discover.FederatedTimelineFragment;
import org.joinmastodon.android.fragments.discover.LocalTimelineFragment;
import org.joinmastodon.android.model.Announcement;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.updater.GithubSelfUpdater;

import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class HomeTabFragment extends MastodonToolbarFragment implements ScrollableToTop, OnBackPressedListener {
    private static final int ANNOUNCEMENTS_RESULT = 654;

    private String accountID;
    private MenuItem announcements;
    private ImageView toolbarLogo;
    private Button toolbarShowNewPostsBtn;
    private boolean newPostsBtnShown;
    private AnimatorSet currentNewPostsAnim;
    private FrameLayout view;
    private ViewPager2 pager;
    private final List<Fragment> fragments = new ArrayList<>();
    private final List<FrameLayout> tabViews = new ArrayList<>();
    private Button switcher;
    private PopupMenu switcherPopup;
    private Drawable chevron;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        accountID = getArguments().getString("account");
        chevron = getActivity().getDrawable(R.drawable.ic_fluent_chevron_down_16_filled);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        view = new FrameLayout(getContext());
        pager = new ViewPager2(getContext());

        if (fragments.size() == 0) {
            Bundle args = new Bundle();
            args.putString("account", accountID);
            args.putBoolean("__is_tab", true);

            fragments.add(new HomeTimelineFragment());
            fragments.add(new LocalTimelineFragment());
            fragments.add(new FederatedTimelineFragment());

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

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        UiUtils.reduceSwipeSensitivity(pager);
        pager.setUserInputEnabled(!GlobalUserPreferences.disableSwipe);
        pager.setAdapter(new HomePagerAdapter());
        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
            @Override
            public void onPageSelected(int position){
                updateSwitcherIcon(position);
                if (position==0) return;
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
    }

    public void updateToolbarLogo(){
        Toolbar toolbar=getToolbar();

        if (toolbar.getChildCount() > 2) {
            return;
            // so that home, local and federated timelines don't invoke this 3 times
            // and add the logo 3 times via onConfigurationChanged
        }

        updateSwitcherIcon(pager.getCurrentItem());
        toolbar.setOnClickListener(v->scrollToTop());
        toolbar.setNavigationContentDescription(R.string.back);

        toolbarLogo=new ImageView(getActivity());
        toolbarLogo.setScaleType(ImageView.ScaleType.CENTER);
        toolbarLogo.setImageResource(R.drawable.logo);
        toolbarLogo.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary)));

        toolbarShowNewPostsBtn=new Button(getActivity());
        toolbarShowNewPostsBtn.setTextAppearance(R.style.m3_title_medium);
        toolbarShowNewPostsBtn.setTextColor(0xffffffff);
        toolbarShowNewPostsBtn.setStateListAnimator(null);
        toolbarShowNewPostsBtn.setBackgroundResource(R.drawable.bg_button_new_posts);
        toolbarShowNewPostsBtn.setText(R.string.see_new_posts);
        toolbarShowNewPostsBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_fluent_arrow_up_16_filled, 0, 0, 0);
        toolbarShowNewPostsBtn.setCompoundDrawableTintList(toolbarShowNewPostsBtn.getTextColors());
        toolbarShowNewPostsBtn.setCompoundDrawablePadding(V.dp(8));
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N)
            UiUtils.fixCompoundDrawableTintOnAndroid6(toolbarShowNewPostsBtn);
        toolbarShowNewPostsBtn.setOnClickListener(this::onNewPostsBtnClick);

        if(newPostsBtnShown){
            toolbarShowNewPostsBtn.setVisibility(View.VISIBLE);
            toolbarLogo.setVisibility(View.INVISIBLE);
            toolbarLogo.setAlpha(0f);
        }else{
            toolbarShowNewPostsBtn.setVisibility(View.INVISIBLE);
            toolbarShowNewPostsBtn.setAlpha(0f);
            toolbarShowNewPostsBtn.setScaleX(.8f);
            toolbarShowNewPostsBtn.setScaleY(.8f);
            toolbarLogo.setVisibility(View.VISIBLE);
        }

        FrameLayout logoWrap=new FrameLayout(getActivity());
        FrameLayout.LayoutParams logoParams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        logoParams.setMargins(0, V.dp(2), 0, 0);
        logoWrap.addView(toolbarLogo, logoParams);
        logoWrap.addView(toolbarShowNewPostsBtn, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, V.dp(32), Gravity.CENTER));
        toolbar.addView(logoWrap, new Toolbar.LayoutParams(Gravity.CENTER));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.home, menu);
        announcements = menu.findItem(R.id.announcements);

        Toolbar toolbar = getToolbar();
        switcher = (Button) LayoutInflater.from(getContext()).inflate(R.layout.home_switcher, toolbar, false);
        switcherPopup = new PopupMenu(getContext(), switcher);
        switcherPopup.inflate(R.menu.home_switcher);
        switcherPopup.setOnMenuItemClickListener(this::onSwitcherItemSelected);
        UiUtils.enablePopupMenuIcons(getContext(), switcherPopup);
        switcher.setOnClickListener(v->switcherPopup.show());
        switcher.setOnTouchListener(switcherPopup.getDragToOpenListener());
        toolbar.setContentInsetsAbsolute(0, toolbar.getContentInsetRight());
        toolbar.addView(switcher);

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

    private boolean onSwitcherItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.home) navigateTo(0);
        else if (item.getItemId() == R.id.local) navigateTo(1);
        else if (item.getItemId() == R.id.federated) navigateTo(2);
        return false;
    }

    private void navigateTo(int i) {
        pager.setCurrentItem(i);
        updateSwitcherIcon(i);
    }

    private void updateSwitcherIcon(int i) {
        Drawable d = getActivity().getDrawable(switch (i) {
            default -> R.drawable.ic_fluent_home_24_regular;
            case 1 -> R.drawable.ic_fluent_people_community_24_regular;
            case 2 -> R.drawable.ic_fluent_earth_24_regular;
        });
        switcher.setCompoundDrawablesWithIntrinsicBounds(d, null, chevron, null);

        MenuItem home = switcherPopup.getMenu().findItem(R.id.home);
        MenuItem local = switcherPopup.getMenu().findItem(R.id.local);
        MenuItem federated = switcherPopup.getMenu().findItem(R.id.federated);
        home.setIcon(R.drawable.ic_fluent_home_24_regular);
        local.setIcon(R.drawable.ic_fluent_people_community_24_regular);
        federated.setIcon(R.drawable.ic_fluent_earth_24_regular);
        home.setEnabled(true);
        local.setEnabled(true);
        federated.setEnabled(true);

        MenuItem selectedItem = switch (i) {
            case 0 -> home;
            case 1 -> local;
            case 2 -> federated;
            default -> null;
        };
        if (selectedItem != null) {
            selectedItem.setIcon(switch (i) {
                case 0 -> R.drawable.ic_fluent_home_24_filled;
                case 1 -> R.drawable.ic_fluent_people_community_24_filled;
                case 2 -> R.drawable.ic_fluent_earth_24_filled;
                default -> 0;
            });
            selectedItem.setEnabled(false);
        }

        UiUtils.insetPopupMenuIcon(getContext(), home);
        UiUtils.insetPopupMenuIcon(getContext(), local);
        UiUtils.insetPopupMenuIcon(getContext(), federated);
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
        toolbarLogo.setVisibility(View.VISIBLE);
        AnimatorSet set=new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(toolbarLogo, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_X, .8f),
                ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_Y, .8f)
        );
        set.setDuration(300);
        set.setInterpolator(CubicBezierInterpolator.DEFAULT);
        set.addListener(new AnimatorListenerAdapter(){
            @Override
            public void onAnimationEnd(Animator animation){
                toolbarShowNewPostsBtn.setVisibility(View.INVISIBLE);
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
        AnimatorSet set=new AnimatorSet();
        set.playTogether(
                ObjectAnimator.ofFloat(toolbarLogo, View.ALPHA, 0f),
                ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.ALPHA, 1f),
                ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_X, 1f),
                ObjectAnimator.ofFloat(toolbarShowNewPostsBtn, View.SCALE_Y, 1f)
        );
        set.setDuration(300);
        set.setInterpolator(CubicBezierInterpolator.DEFAULT);
        set.addListener(new AnimatorListenerAdapter(){
            @Override
            public void onAnimationEnd(Animator animation){
                toolbarLogo.setVisibility(View.INVISIBLE);
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
