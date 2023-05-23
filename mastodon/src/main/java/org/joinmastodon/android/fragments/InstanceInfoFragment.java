package org.joinmastodon.android.fragments;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Outline;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.instance.GetExtendedDescription;
import org.joinmastodon.android.api.requests.instance.GetInstance;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.onboarding.InstanceRulesFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.AccountField;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.ExtendedDescription;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.SingleImagePhotoViewerListener;
import org.joinmastodon.android.ui.drawables.CoverOverlayGradientDrawable;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CoverImageView;
import org.joinmastodon.android.ui.views.LinkedTextView;
import org.parceler.Parcels;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.SimpleCallback;
import me.grishka.appkit.fragments.LoaderFragment;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class InstanceInfoFragment extends LoaderFragment {

	private Instance instance;
	private String extendedDescription;
	private CoverImageView cover;
	private TextView uri, description, readMore;
	private SwipeRefreshLayout refreshLayout;
	private final CoverOverlayGradientDrawable coverGradient=new CoverOverlayGradientDrawable();
	private LinearLayout textWrap;

	private ScrollView scrollView, textScrollView;
	private float titleTransY;

	private String accountID;
	private Account account;
	private String targetDomain;
	private final ArrayList<AccountField> fields=new ArrayList<>();

	private boolean refreshing;
	private boolean isExpanded = false;

	public UsableRecyclerView list;
	private List<AccountField> metadataListData=Collections.emptyList();
	private MetadataAdapter adapter;
	private ListImageLoaderWrapper imgLoader;

	private float textMaxHeight, textCollapsedHeight;
	private LinearLayout.LayoutParams collapseParams, wrapParams;

	public InstanceInfoFragment(){
		super(R.layout.loader_fragment_overlay_toolbar);
	}

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N)
			setRetainInstance(true);

		accountID=getArguments().getString("account");
		account= AccountSessionManager.getInstance().getAccount(accountID).self;
		targetDomain=getArguments().getString("instanceDomain");
		loadData();
		loadExtendedDescription();
	}

	@Override
	public void onAttach(Activity activity){
		super.onAttach(activity);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateContentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		View content=inflater.inflate(R.layout.fragment_instance_info, container, false);

		refreshLayout=content.findViewById(R.id.refresh_layout);
		scrollView=content.findViewById(R.id.scroll_view);
		cover=content.findViewById(R.id.cover);
		uri =content.findViewById(R.id.uri);
		description=content.findViewById(R.id.description);
		list=content.findViewById(R.id.metadata);
		textScrollView=content.findViewById(R.id.text_scroll_view);
		textWrap=content.findViewById(R.id.text_wrap);
		readMore=content.findViewById(R.id.read_more);
		textMaxHeight=getActivity().getResources().getDimension(R.dimen.description_max_height);
		textCollapsedHeight=getActivity().getResources().getDimension(R.dimen.description_collapsed_height);
		collapseParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) textCollapsedHeight);
		wrapParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		cover.setForeground(coverGradient);
		cover.setOnClickListener(this::onCoverClick);
		readMore.setOnClickListener(this::onReadMoreClick);
		refreshLayout.setOnRefreshListener(this);


		if(loaded){
			bindViews();
			dataLoaded();
		}

		list.setItemAnimator(new BetterItemAnimator());
		list.setDrawSelectorOnTop(true);
		list.setLayoutManager(new LinearLayoutManager(getActivity()));
		imgLoader=new ListImageLoaderWrapper(getActivity(), list, new RecyclerViewDelegate(list), null);
		list.setAdapter(adapter=new MetadataAdapter());
		list.setClipToPadding(false);

		return content;
	}

	@Override
	protected void doLoadData(){
		currentRequest=new GetInstance()
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(Instance result){
						if (getActivity() == null) return;
						instance = result;
						bindViews();
						dataLoaded();
						if(refreshing) {
							refreshing = false;
							refreshLayout.setRefreshing(false);
						}
					}
				})
				.execNoAuth(targetDomain);
	}

	private void loadExtendedDescription() {
		new GetExtendedDescription()
				.setCallback(new SimpleCallback<>(this){
					@Override
					public void onSuccess(ExtendedDescription result){
						if (getActivity() == null || result == null || TextUtils.isEmpty(result.content)) return;
						extendedDescription = result.content;
						updateDescription();
						collapseDescription();
					}
				})
				.execNoAuth(targetDomain);
	}

	private void updateDescription() {
		if (instance == null || description == null)
			return;

		description.setText(HtmlParser.parse(TextUtils.isEmpty(extendedDescription) ?
				TextUtils.isEmpty(instance.description) ? instance.shortDescription : instance.description
				: extendedDescription,
				account.emojis, Collections.emptyList(), Collections.emptyList(), accountID));

		description.measure(
					View.MeasureSpec.makeMeasureSpec(textWrap.getWidth(), View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
	}

	@Override
	public void onRefresh(){
		if(refreshing)
			return;
		refreshing=true;
		doLoadData();
		loadExtendedDescription();
	}

	@Override
	public void dataLoaded(){
		if(getActivity()==null)
			return;
		setFields(fields);
		super.dataLoaded();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		updateToolbar();
		titleTransY=getToolbar().getLayoutParams().height;
		if(toolbarTitleView!=null){
			toolbarTitleView.setTranslationY(titleTransY);
			toolbarSubtitleView.setTranslationY(titleTransY);
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		updateToolbar();
	}

	@Override
	public void onApplyWindowInsets(WindowInsets insets){
		int statusBarHeight = insets.getSystemWindowInsetTop();
		if(contentView!=null){
			((ViewGroup.MarginLayoutParams) getToolbar().getLayoutParams()).topMargin= statusBarHeight;
		}
		super.onApplyWindowInsets(insets.replaceSystemWindowInsets(insets.getSystemWindowInsetLeft(), 0, insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom()));
	}



	private void bindViews(){
		ViewImageLoader.load(cover, null, new UrlImageLoaderRequest(instance.thumbnail, 1000, 1000));
		uri.setText(instance.title);
		setTitle(instance.title);

		updateDescription();
		collapseDescription();

		fields.clear();


		if (instance.contactAccount != null) {
			AccountField admin = new AccountField();
			admin.parsedName=admin.name=getContext().getString(R.string.mo_instance_admin);
			admin.parsedValue=buildLinkText(instance.contactAccount.url, instance.contactAccount.getDisplayUsername() + "@" + instance.uri);
			fields.add(admin);
		}

		if (instance.email != null) {
			AccountField contact = new AccountField();
			contact.parsedName=getContext().getString(R.string.mo_instance_contact);
			contact.parsedValue=buildLinkText("mailto:" + instance.email, instance.email);
			fields.add(contact);
		}

		if (instance.stats != null) {
			AccountField activeUsers = new AccountField();
			activeUsers.parsedName=getContext().getString(R.string.mo_instance_users);
			activeUsers.parsedValue= NumberFormat.getInstance().format(instance.stats.userCount);
			fields.add(activeUsers);

			AccountField statusCount = new AccountField();
			statusCount.parsedName=getContext().getString(R.string.mo_instance_status);
			statusCount.parsedValue= NumberFormat.getInstance().format(instance.stats.statusCount);
			fields.add(statusCount);
		}

		AccountField registration = new AccountField();
		registration.parsedName=getContext().getString(R.string.mo_instance_registration);
		registration.parsedValue=getContext().getString(instance.registrations ? instance.approvalRequired ? R.string.mo_instance_registration_approval : R.string.mo_instance_registration_open : R.string.instance_signup_closed);
		fields.add(registration);

		setFields(fields);
	}

	private SpannableStringBuilder buildLinkText(String link, String text) {
		String value = "<span class=\"h-card\"><a href=" + link + " class=\"u-url mention\" rel=\"nofollow noopener noreferrer\" target=\"_blank\">" + text + "</a></span>";
		return HtmlParser.parse(value, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID);
	}

	private void collapseDescription() {
		textScrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		description.measure(
				View.MeasureSpec.makeMeasureSpec(textWrap.getWidth(), View.MeasureSpec.EXACTLY),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

		readMore.setText(isExpanded ? R.string.sk_collapse : R.string.sk_expand);
		description.post(() -> {
			boolean tooBig = description.getMeasuredHeight() > textMaxHeight;
			readMore.setVisibility(tooBig ? View.VISIBLE : View.GONE);
			textScrollView.setLayoutParams(tooBig && !isExpanded ? collapseParams : wrapParams);
		});
	}

	private void updateToolbar(){
		getToolbar().setBackgroundColor(0);
		if(toolbarTitleView!=null){
			toolbarTitleView.setTranslationY(titleTransY);
			toolbarSubtitleView.setTranslationY(titleTransY);
		}
		getToolbar().setOnClickListener(v->scrollToTop());
		getToolbar().setNavigationContentDescription(R.string.back);
	}

	public void scrollToTop(){
		scrollView.smoothScrollTo(0, 0);
	}


	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
		inflater.inflate(R.menu.instance_info, menu);
		UiUtils.enableOptionsMenuIcons(getActivity(), menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		int id=item.getItemId();
		if (id==R.id.open_timeline) {
			Bundle args=new Bundle();
			args.putString("account", accountID);
			args.putString("domain", instance.uri);
			Nav.go(getActivity(), CustomLocalTimelineFragment.class, args);
		}else if (id==R.id.rules) {
			Bundle args=new Bundle();
			args.putParcelable("instance", Parcels.wrap(instance));
			Nav.go(getActivity(), InstanceRulesFragment.class, args);
		} else if (id==R.id.moderated_servers) {
			Bundle args=new Bundle();
			args.putParcelable("instance", Parcels.wrap(instance));
			Nav.go(getActivity(), InstanceBlockListFragment.class, args);
		}
		return true;
	}


	@Override
	public boolean wantsLightStatusBar(){
		return false;
	}
	@Override
	protected int getToolbarResource(){
		return R.layout.profile_toolbar;
	}
	private void onReadMoreClick(View view) {
		isExpanded = !isExpanded;
		bindViews();
	}


	private void onCoverClick(View v){
			Drawable drawable=cover.getDrawable();
			if(drawable==null || drawable instanceof ColorDrawable)
				return;
		new PhotoViewer(getActivity(), Attachment.createFakeAttachments(instance.thumbnail, drawable), 0,
				new SingleImagePhotoViewerListener(cover, cover, null, this, () -> {
				}, () -> drawable, null, null));
	}

	public void setFields(ArrayList<AccountField> fields){
		metadataListData=fields;
		if (adapter != null) adapter.notifyDataSetChanged();
	}

	private class MetadataAdapter extends UsableRecyclerView.Adapter<BaseViewHolder> {
		public MetadataAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new AboutViewHolder();
		}

		@Override
		public void onBindViewHolder(BaseViewHolder holder, int position){
			holder.bind(metadataListData.get(position));
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getItemCount(){
				return metadataListData.size();
		}

		@Override
		public int getItemViewType(int position){
			return 0;
		}

	}

	private abstract class BaseViewHolder extends BindableViewHolder<AccountField> {
		public BaseViewHolder(int layout){
			super(getActivity(), layout, list);
		}
	}

	private class AboutViewHolder extends BaseViewHolder {
		private final TextView title;
		private final LinkedTextView value;

		public AboutViewHolder(){
			super(R.layout.item_profile_about);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
		}

		@Override
		public void onBind(AccountField item){
			title.setText(item.parsedName);
			value.setText(item.parsedValue);
			value.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
			value.setLinkTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorAccent));
			value.setCompoundDrawables(null, null, null, null);
		}
	}
}
