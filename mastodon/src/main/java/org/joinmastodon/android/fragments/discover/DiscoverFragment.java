package org.joinmastodon.android.fragments.discover;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.ScrollableToTop;
import org.joinmastodon.android.ui.SimpleViewHolder;
import org.joinmastodon.android.ui.tabs.TabLayout;
import org.joinmastodon.android.ui.tabs.TabLayoutMediator;
import org.joinmastodon.android.ui.utils.UiUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import me.grishka.appkit.fragments.AppKitFragment;
import me.grishka.appkit.fragments.BaseRecyclerFragment;
import me.grishka.appkit.fragments.OnBackPressedListener;
import me.grishka.appkit.utils.V;

public class DiscoverFragment extends AppKitFragment implements ScrollableToTop, OnBackPressedListener{

	private TabLayout tabLayout;
	private ViewPager2 pager;
	private FrameLayout[] tabViews;
	private TabLayoutMediator tabLayoutMediator;
	private EditText searchEdit;
	private boolean searchActive;
	private FrameLayout searchView;
	private ImageButton searchBack, searchClear;
	private ProgressBar searchProgress;

	private DiscoverPostsFragment postsFragment;
	private TrendingHashtagsFragment hashtagsFragment;
	private DiscoverNewsFragment newsFragment;
	private DiscoverAccountsFragment accountsFragment;
	private SearchFragment searchFragment;
	private LocalTimelineFragment localTimelineFragment;
	private FederatedTimelineFragment federatedTimelineFragment;

	private String accountID;
	private Runnable searchDebouncer=this::onSearchChangedDebounced;

	private final boolean noFederated = !GlobalUserPreferences.showFederatedTimeline;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		accountID=getArguments().getString("account");
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState){
		LinearLayout view=(LinearLayout) inflater.inflate(R.layout.fragment_discover, container, false);

		tabLayout=view.findViewById(R.id.tabbar);
		pager=view.findViewById(R.id.pager);

		tabViews=new FrameLayout[noFederated ? 5 : 6];
		for(int i=0;i<tabViews.length;i++){
			FrameLayout tabView=new FrameLayout(getActivity());
			int switchIndex = noFederated && i > 0 ? i + 1 : i;
			tabView.setId(switch(switchIndex){
				case 0 -> R.id.discover_local_timeline;
				case 1 -> R.id.discover_federated_timeline;
				case 2 -> R.id.discover_hashtags;
				case 3 -> R.id.discover_posts;
				case 4 -> R.id.discover_news;
				case 5 -> R.id.discover_users;
				default -> throw new IllegalStateException("Unexpected value: "+switchIndex);
			});
			tabView.setVisibility(View.GONE);
			view.addView(tabView); // needed so the fragment manager will have somewhere to restore the tab fragment
			tabViews[i]=tabView;
		}

		tabLayout.setTabTextSize(V.dp(16));
		tabLayout.setTabTextColors(UiUtils.getThemeColor(getActivity(), R.attr.colorTabInactive), UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));

		UiUtils.reduceSwipeSensitivity(pager);
		pager.setOffscreenPageLimit(4);
		pager.setUserInputEnabled(!GlobalUserPreferences.disableSwipe);
		pager.setAdapter(new DiscoverPagerAdapter());
		pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback(){
			@Override
			public void onPageSelected(int position){
				if(position==0)
					return;
				Fragment _page=getFragmentForPage(position);
				if(_page instanceof BaseRecyclerFragment<?> page){
					if(!page.loaded && !page.isDataLoading())
						page.loadData();
				}
			}
		});

		if(localTimelineFragment==null){
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putBoolean("__is_tab", true);

			postsFragment=new DiscoverPostsFragment();
			postsFragment.setArguments(args);

			hashtagsFragment=new TrendingHashtagsFragment();
			hashtagsFragment.setArguments(args);

			newsFragment=new DiscoverNewsFragment();
			newsFragment.setArguments(args);

			accountsFragment=new DiscoverAccountsFragment();
			accountsFragment.setArguments(args);

			localTimelineFragment=new LocalTimelineFragment();
			localTimelineFragment.setArguments(args);

			FragmentTransaction transaction = getChildFragmentManager().beginTransaction()
					.add(R.id.discover_posts, postsFragment)
					.add(R.id.discover_local_timeline, localTimelineFragment)
					.add(R.id.discover_hashtags, hashtagsFragment)
					.add(R.id.discover_news, newsFragment)
					.add(R.id.discover_users, accountsFragment);

			if (!noFederated) {
				federatedTimelineFragment=new FederatedTimelineFragment();
				federatedTimelineFragment.setArguments(args);
				transaction.add(R.id.discover_federated_timeline, federatedTimelineFragment);
			}

			transaction.commit();
		}

		tabLayoutMediator=new TabLayoutMediator(tabLayout, pager, new TabLayoutMediator.TabConfigurationStrategy(){
			@Override
			public void onConfigureTab(@NonNull TabLayout.Tab tab, int position){
				if (noFederated && position > 0) position++;
				tab.setText(switch(position){
					case 0 -> R.string.local_timeline;
					case 1 -> R.string.sk_federated_timeline;
					case 2 -> R.string.hashtags;
					case 3 -> R.string.posts;
					case 4 -> R.string.news;
					case 5 -> R.string.for_you;
					default -> throw new IllegalStateException("Unexpected value: "+position);
				});
				tab.view.textView.setAllCaps(true);
			}
		});
		tabLayoutMediator.attach();
		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
			@Override
			public void onTabSelected(TabLayout.Tab tab){}

			@Override
			public void onTabUnselected(TabLayout.Tab tab){}

			@Override
			public void onTabReselected(TabLayout.Tab tab){
				scrollToTop();
			}
		});

		searchEdit=view.findViewById(R.id.search_edit);
		searchEdit.setOnFocusChangeListener(this::onSearchEditFocusChanged);
		searchEdit.setOnEditorActionListener(this::onSearchEnterPressed);
		searchEdit.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after){
				if(s.length()==0){
					V.setVisibilityAnimated(searchClear, View.VISIBLE);
				}
			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count){
				searchEdit.removeCallbacks(searchDebouncer);
				searchEdit.postDelayed(searchDebouncer, 300);
			}

			@Override
			public void afterTextChanged(Editable s){
				if(s.length()==0){
					V.setVisibilityAnimated(searchClear, View.INVISIBLE);
				}
			}
		});

		searchView=view.findViewById(R.id.search_fragment);
		if(searchFragment==null){
			searchFragment=new SearchFragment();
			Bundle args=new Bundle();
			args.putString("account", accountID);
			searchFragment.setArguments(args);
			searchFragment.setProgressVisibilityListener(this::onSearchProgressVisibilityChanged);
			getChildFragmentManager().beginTransaction().add(R.id.search_fragment, searchFragment).commit();
		}

		searchBack=view.findViewById(R.id.search_back);
		searchClear=view.findViewById(R.id.search_clear);
		searchProgress=view.findViewById(R.id.search_progress);
		searchBack.setEnabled(searchActive);
		searchBack.setImportantForAccessibility(searchActive ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		searchBack.setOnClickListener(v->exitSearch());
		if(searchActive){
			searchBack.setImageResource(R.drawable.ic_fluent_arrow_left_24_regular);
			pager.setVisibility(View.GONE);
			tabLayout.setVisibility(View.GONE);
			searchView.setVisibility(View.VISIBLE);
		}
		searchClear.setOnClickListener(v->{
			searchEdit.setText("");
			searchEdit.removeCallbacks(searchDebouncer);
			onSearchChangedDebounced();
		});

		return view;
	}

	@Override
	public void scrollToTop(){
		if(!searchActive){
			((ScrollableToTop)getFragmentForPage(pager.getCurrentItem())).scrollToTop();
		}else{
			searchFragment.scrollToTop();
		}
	}

	public void loadData(){
		if(localTimelineFragment!=null && !localTimelineFragment.loaded && !localTimelineFragment.dataLoading)
			localTimelineFragment.loadData();
	}

	private void onSearchEditFocusChanged(View v, boolean hasFocus){
		if(!searchActive && hasFocus){
			searchActive=true;
			pager.setVisibility(View.GONE);
			tabLayout.setVisibility(View.GONE);
			searchView.setVisibility(View.VISIBLE);
			searchBack.setImageResource(R.drawable.ic_fluent_arrow_left_24_regular);
			searchBack.setEnabled(true);
			searchBack.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
		}
	}

	private void exitSearch(){
		searchActive=false;
		pager.setVisibility(View.VISIBLE);
		tabLayout.setVisibility(View.VISIBLE);
		searchView.setVisibility(View.GONE);
		searchEdit.clearFocus();
		searchEdit.setText("");
		searchBack.setImageResource(R.drawable.ic_fluent_search_24_regular);
		searchBack.setEnabled(false);
		searchBack.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
		getActivity().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
	}

	@Override
	protected void onHidden(){
		super.onHidden();
		getActivity().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
	}

	private Fragment getFragmentForPage(int page){
		if (noFederated && page > 0) page++;
		return switch(page){
			case 0 -> localTimelineFragment;
			case 1 -> federatedTimelineFragment;
			case 2 -> hashtagsFragment;
			case 3 -> postsFragment;
			case 4 -> newsFragment;
			case 5 -> accountsFragment;
			default -> throw new IllegalStateException("Unexpected value: "+page);
		};
	}

	@Override
	public boolean onBackPressed(){
		if(searchActive){
			exitSearch();
			return true;
		}
		return false;
	}

	private void onSearchChangedDebounced(){
		searchFragment.setQuery(searchEdit.getText().toString());
	}

	private boolean onSearchEnterPressed(TextView v, int actionId, KeyEvent event){
		if(event!=null && event.getAction()!=KeyEvent.ACTION_DOWN)
			return true;
		searchEdit.removeCallbacks(searchDebouncer);
		onSearchChangedDebounced();
		getActivity().getSystemService(InputMethodManager.class).hideSoftInputFromWindow(searchEdit.getWindowToken(), 0);
		return true;
	}

	private void onSearchProgressVisibilityChanged(boolean visible){
		V.setVisibilityAnimated(searchProgress, visible ? View.VISIBLE : View.INVISIBLE);
		if(searchEdit.length()>0)
			V.setVisibilityAnimated(searchClear, visible ? View.INVISIBLE : View.VISIBLE);
	}

	private class DiscoverPagerAdapter extends RecyclerView.Adapter<SimpleViewHolder>{
		@NonNull
		@Override
		public SimpleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			FrameLayout view=tabViews[viewType];
			((ViewGroup)view.getParent()).removeView(view);
			view.setVisibility(View.VISIBLE);
			view.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			return new SimpleViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull SimpleViewHolder holder, int position){}

		@Override
		public int getItemCount(){
			return tabViews.length;
		}

		@Override
		public int getItemViewType(int position){
			return position;
		}
	}
}
