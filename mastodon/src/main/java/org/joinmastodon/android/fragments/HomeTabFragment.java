package org.joinmastodon.android.fragments;

import android.animation.AnimatorSet;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.res.ColorStateList;
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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.announcements.GetAnnouncements;
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
import me.grishka.appkit.utils.V;

public class HomeTabFragment extends MastodonToolbarFragment {
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
                if (position==0) return;
                if (fragments.get(position) instanceof BaseRecyclerFragment<?> page){
                    if(!page.loaded && !page.isDataLoading()) page.loadData();
                }
            }
        });

        updateToolbarLogo();
//        list.addOnScrollListener(new RecyclerView.OnScrollListener(){
//            @Override
//            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy){
//                if(newPostsBtnShown && list.getChildAdapterPosition(list.getChildAt(0))<=getMainAdapterOffset()){
//                    hideNewPostsButton();
//                }
//            }
//        });
//
//        if(GithubSelfUpdater.needSelfUpdating()){
//            E.register(this);
//            updateUpdateState(GithubSelfUpdater.getInstance().getState());
//        }
    }

    private void updateToolbarLogo(){
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
//        toolbarShowNewPostsBtn.setOnClickListener(this::onNewPostsBtnClick);

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

        Toolbar toolbar=getToolbar();
        toolbar.addView(logoWrap, new Toolbar.LayoutParams(Gravity.CENTER));
        View switcher = LayoutInflater.from(getContext()).inflate(R.layout.home_switcher, toolbar, false);
        toolbar.addView(switcher);
        PopupMenu switcherPopup = new PopupMenu(getContext(), switcher);
        switcherPopup.inflate(R.menu.home_switcher);
        UiUtils.enablePopupMenuIcons(getContext(), switcherPopup);
        switcher.setOnClickListener(v->switcherPopup.show());
        switcher.setOnTouchListener(switcherPopup.getDragToOpenListener());
        toolbar.setContentInsetsAbsolute(0, toolbar.getContentInsetRight());
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

    @Override
    public void onFragmentResult(int reqCode, boolean noMoreUnread, Bundle result){
        if (reqCode == ANNOUNCEMENTS_RESULT && noMoreUnread) {
            announcements.setIcon(R.drawable.ic_fluent_megaphone_24_regular);
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
