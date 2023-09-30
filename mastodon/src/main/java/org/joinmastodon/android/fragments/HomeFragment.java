package org.joinmastodon.android.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.NotificationManager;
import android.app.assist.AssistContent;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import com.squareup.otto.Subscribe;

import org.joinmastodon.android.E;
import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.NotificationsMarkerUpdatedEvent;
import org.joinmastodon.android.events.StatusDisplaySettingsChangedEvent;
import org.joinmastodon.android.fragments.discover.DiscoverFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.PaginatedResponse;
import org.joinmastodon.android.ui.AccountSwitcherSheet;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.TabBar;
import org.joinmastodon.android.utils.ObjectIdComparator;
import org.joinmastodon.android.utils.ProvidesAssistContent;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import me.grishka.appkit.FragmentStackActivity;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.FragmentRootLinearLayout;

public class HomeFragment extends AppKitFragment implements OnBackPressedListener, ProvidesAssistContent, HasAccountID {
	private FragmentRootLinearLayout content;
	private HomeTabFragment homeTabFragment;
	private NotificationsFragment notificationsFragment;
	private DiscoverFragment discoverFragment;
	private ProfileFragment profileFragment;
	private TabBar tabBar;
	private View tabBarWrap;
	private ImageView tabBarAvatar;
	@IdRes
	private int currentTab=R.id.tab_home;
	private TextView notificationsBadge;

	private String accountID;
	private boolean isAkkoma;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		accountID=getArguments().getString("account");
		setTitle(R.string.sk_app_name);
		isAkkoma = getInstance().map(Instance::isAkkoma).orElse(false);

		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		if(savedInstanceState==null){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			homeTabFragment=new HomeTabFragment();
			homeTabFragment.setArguments(args);
			args=new Bundle(args);
			args.putBoolean("disableDiscover", isAkkoma);
			args.putBoolean("noAutoLoad", true);
			discoverFragment=new DiscoverFragment();
			discoverFragment.setArguments(args);
			notificationsFragment=new NotificationsFragment();
			notificationsFragment.setArguments(args);
			args=new Bundle(args);
			args.putParcelable("profileAccount", Parcels.wrap(AccountSessionManager.getInstance().getAccount(accountID).self));
			args.putBoolean("noAutoLoad", true);
			profileFragment=new ProfileFragment();
			profileFragment.setArguments(args);
		}

		E.register(this);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		E.unregister(this);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		content=new FragmentRootLinearLayout(getActivity());
		content.setOrientation(LinearLayout.VERTICAL);

		FrameLayout fragmentContainer=new FrameLayout(getActivity());
		fragmentContainer.setId(me.grishka.appkit.R.id.fragment_wrap);
		content.addView(fragmentContainer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

		inflater.inflate(R.layout.tab_bar, content);
		tabBar=content.findViewById(R.id.tabbar);
		tabBar.setListeners(this::onTabSelected, this::onTabLongClick);
		tabBarWrap=content.findViewById(R.id.tabbar_wrap);

		// this one's for the pill haters (https://m3.material.io/components/navigation-bar/overview)
		if(GlobalUserPreferences.disableM3PillActiveIndicator){
			tabBar.findViewById(R.id.tab_home_pill).setBackground(null);
			tabBar.findViewById(R.id.tab_search_pill).setBackground(null);
			tabBar.findViewById(R.id.tab_notifications_pill).setBackground(null);
			tabBar.findViewById(R.id.tab_profile_pill).setBackgroundResource(R.drawable.bg_tab_profile);

			View[] tabs={
					tabBar.findViewById(R.id.tab_home),
					tabBar.findViewById(R.id.tab_search),
					tabBar.findViewById(R.id.tab_notifications),
					tabBar.findViewById(R.id.tab_profile)
			};

			for(View tab : tabs){
				tab.setBackgroundResource(R.drawable.bg_tabbar_tab_ripple);
				((RippleDrawable) tab.getBackground())
						.setRadius(V.dp(GlobalUserPreferences.showNavigationLabels ? 56 : 42));
			}
		}

		if(!GlobalUserPreferences.showNavigationLabels){
			tabBar.findViewById(R.id.tab_home_label).setVisibility(View.GONE);
			tabBar.findViewById(R.id.tab_search_label).setVisibility(View.GONE);
			tabBar.findViewById(R.id.tab_notifications_label).setVisibility(View.GONE);
			tabBar.findViewById(R.id.tab_profile_label).setVisibility(View.GONE);
		}

		tabBarAvatar=tabBar.findViewById(R.id.tab_profile_ava);
		tabBarAvatar.setOutlineProvider(OutlineProviders.OVAL);
		tabBarAvatar.setClipToOutline(true);
		Account self=AccountSessionManager.getInstance().getAccount(accountID).self;
		ViewImageLoader.loadWithoutAnimation(tabBarAvatar, null, new UrlImageLoaderRequest(self.avatar, V.dp(24), V.dp(24)));

		notificationsBadge=tabBar.findViewById(R.id.notifications_badge);
		notificationsBadge.setVisibility(View.GONE);

		if(savedInstanceState==null){
			getChildFragmentManager().beginTransaction()
					.add(me.grishka.appkit.R.id.fragment_wrap, homeTabFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, discoverFragment).hide(discoverFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, notificationsFragment).hide(notificationsFragment)
					.add(me.grishka.appkit.R.id.fragment_wrap, profileFragment).hide(profileFragment)
					.commit();

			String defaultTab=getArguments().getString("tab");
			if("notifications".equals(defaultTab)){
				tabBar.selectTab(R.id.tab_notifications);
				fragmentContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener(){
					@Override
					public boolean onPreDraw(){
						fragmentContainer.getViewTreeObserver().removeOnPreDrawListener(this);
						onTabSelected(R.id.tab_notifications);
						return true;
					}
				});
			}
		}
		tabBar.selectTab(currentTab);

		return content;
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState){
		super.onViewStateRestored(savedInstanceState);
		if(savedInstanceState==null) return;
		homeTabFragment=(HomeTabFragment) getChildFragmentManager().getFragment(savedInstanceState, "homeTabFragment");
		discoverFragment=(DiscoverFragment) getChildFragmentManager().getFragment(savedInstanceState, "searchFragment");
		notificationsFragment=(NotificationsFragment) getChildFragmentManager().getFragment(savedInstanceState, "notificationsFragment");
		profileFragment=(ProfileFragment) getChildFragmentManager().getFragment(savedInstanceState, "profileFragment");
		currentTab=savedInstanceState.getInt("selectedTab");
		tabBar.selectTab(currentTab);
		Fragment current=fragmentForTab(currentTab);
		getChildFragmentManager().beginTransaction()
				.hide(homeTabFragment)
				.hide(discoverFragment)
				.hide(notificationsFragment)
				.hide(profileFragment)
				.show(current)
				.commit();
		maybeTriggerLoading(current);
	}

	@Override
	public void onHiddenChanged(boolean hidden){
		super.onHiddenChanged(hidden);
		fragmentForTab(currentTab).onHiddenChanged(hidden);
	}

	@Override
	public boolean wantsLightStatusBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public boolean wantsLightNavigationBar(){
		return !UiUtils.isDarkTheme();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=27){
			int inset=insets.getSystemWindowInsetBottom();
			tabBarWrap.setPadding(0, 0, 0, inset>0 ? Math.max(inset, V.dp(24)) : 0);
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), 0));
		}else{
			super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
		}
		WindowInsets topOnlyInsets=insets.replaceSystemWindowInsets(0, insets.getSystemWindowInsetTop(), 0, 0);
		homeTabFragment.onApplyWindowInsets(topOnlyInsets);
		discoverFragment.onApplyWindowInsets(topOnlyInsets);
		notificationsFragment.onApplyWindowInsets(topOnlyInsets);
		profileFragment.onApplyWindowInsets(topOnlyInsets);
	}

	private Fragment fragmentForTab(@IdRes int tab){
		if(tab==R.id.tab_home){
			return homeTabFragment;
		}else if(tab==R.id.tab_search){
			return discoverFragment;
		}else if(tab==R.id.tab_notifications){
			return notificationsFragment;
		}else if(tab==R.id.tab_profile){
			return profileFragment;
		}
		throw new IllegalArgumentException();
	}

	public void setCurrentTab(@IdRes int tab){
		if(tab==currentTab)
			return;
		tabBar.selectTab(tab);
		onTabSelected(tab);
	}

	private void onTabSelected(@IdRes int tab){
		Fragment newFragment=fragmentForTab(tab);
		if(tab==currentTab && newFragment instanceof ScrollableToTop scrollable) {
			scrollable.scrollToTop();
			return;
		}
		getChildFragmentManager().beginTransaction().hide(fragmentForTab(currentTab)).show(newFragment).commit();
		maybeTriggerLoading(newFragment);
		if (newFragment instanceof HasFab fabulous && !fabulous.isScrolling()) fabulous.showFab();
		currentTab=tab;
		((FragmentStackActivity)getActivity()).invalidateSystemBarColors(this);
	}

	private void maybeTriggerLoading(Fragment newFragment){
		if(newFragment instanceof LoaderFragment lf){
			if(!lf.loaded && !lf.dataLoading)
				lf.loadData();
		}else if(newFragment instanceof DiscoverFragment){
			((DiscoverFragment) newFragment).loadData();
		}else if(newFragment instanceof NotificationsFragment){
			((NotificationsFragment) newFragment).loadData();
			NotificationManager nm=getActivity().getSystemService(NotificationManager.class);
			for (StatusBarNotification notification : nm.getActiveNotifications()) {
				if (accountID.equals(notification.getTag())) {
					nm.cancel(accountID, notification.getId());
				}
			}
		}
	}

	private boolean onTabLongClick(@IdRes int tab){
		if(tab==R.id.tab_profile){
			ArrayList<String> options=new ArrayList<>();
			for(AccountSession session:AccountSessionManager.getInstance().getLoggedInAccounts()){
				options.add(session.self.displayName+"\n("+session.self.username+"@"+session.domain+")");
			}
			new AccountSwitcherSheet(getActivity(), this).show();
			return true;
		} else if(tab==R.id.tab_search){
			tabBar.selectTab(R.id.tab_search);
			onTabSelected(R.id.tab_search);
			discoverFragment.openSearch();
			return true;
		}
		return false;
	}

	@Override
	public boolean onBackPressed(){
		if(currentTab==R.id.tab_profile)
			if (profileFragment.onBackPressed()) return true;
		if(currentTab==R.id.tab_search)
			if (discoverFragment.onBackPressed()) return true;
		if (currentTab!=R.id.tab_home) {
			tabBar.selectTab(R.id.tab_home);
			onTabSelected(R.id.tab_home);
			return true;
		} else {
			return homeTabFragment.onBackPressed();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState){
		super.onSaveInstanceState(outState);
		outState.putInt("selectedTab", currentTab);
		if (homeTabFragment.isAdded()) getChildFragmentManager().putFragment(outState, "homeTabFragment", homeTabFragment);
		if (discoverFragment.isAdded()) getChildFragmentManager().putFragment(outState, "searchFragment", discoverFragment);
		if (notificationsFragment.isAdded()) getChildFragmentManager().putFragment(outState, "notificationsFragment", notificationsFragment);
		if (profileFragment.isAdded()) getChildFragmentManager().putFragment(outState, "profileFragment", profileFragment);
	}

	@Override
	protected void onShown(){
		super.onShown();
		reloadNotificationsForUnreadCount();
	}

	public void reloadNotificationsForUnreadCount(){
		List<Notification>[] notifications=new List[]{null};
		String[] marker={null};

		AccountSessionManager.get(accountID).reloadNotificationsMarker(m->{
			marker[0]=m;
			if(notifications[0]!=null){
				updateUnreadCount(notifications[0], marker[0]);
			}
		});

		AccountSessionManager.get(accountID).getCacheController().getNotifications(null, 40, false, false, true, new Callback<>(){
			@Override
			public void onSuccess(PaginatedResponse<List<Notification>> result){
				notifications[0]=result.items;
				if(marker[0]!=null)
					updateUnreadCount(notifications[0], marker[0]);
			}

			@Override
			public void onError(ErrorResponse error){}
		});
	}

	@SuppressLint("DefaultLocale")
	private void updateUnreadCount(List<Notification> notifications, String marker){
		if(notifications.isEmpty() || ObjectIdComparator.INSTANCE.compare(notifications.get(0).id, marker)<=0){
			V.setVisibilityAnimated(notificationsBadge, View.GONE);
		}else{
			V.setVisibilityAnimated(notificationsBadge, View.VISIBLE);
			if(ObjectIdComparator.INSTANCE.compare(notifications.get(notifications.size()-1).id, marker)>0){
				notificationsBadge.setText(String.format("%d+", notifications.size()));
			}else{
				int count=0;
				for(Notification n:notifications){
					if(n.id.equals(marker))
						break;
					count++;
				}
				notificationsBadge.setText(String.format("%d", count));
			}
		}
	}

	@Subscribe
	public void onNotificationsMarkerUpdated(NotificationsMarkerUpdatedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		if(ev.clearUnread)
			V.setVisibilityAnimated(notificationsBadge, View.GONE);
	}

	@Subscribe
	public void onStatusDisplaySettingsChanged(StatusDisplaySettingsChangedEvent ev){
		if(!ev.accountID.equals(accountID))
			return;
		if(homeTabFragment.getCurrentFragment() instanceof LoaderFragment lf && lf.loaded
			&& lf instanceof BaseStatusListFragment<?> homeTimelineFragment)
			homeTimelineFragment.rebuildAllDisplayItems();
		if(notificationsFragment.getCurrentFragment() instanceof LoaderFragment lf && lf.loaded
			&& lf instanceof BaseStatusListFragment<?> l)
			l.rebuildAllDisplayItems();
	}

	@Override
	public String getAccountID() {
		return accountID;
	}

	@Override
	public void onProvideAssistContent(AssistContent assistContent) {
		callFragmentToProvideAssistContent(fragmentForTab(currentTab), assistContent);
	}
}
