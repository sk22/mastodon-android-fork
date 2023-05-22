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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import org.joinmastodon.android.GlobalUserPreferences;
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
import org.joinmastodon.android.model.TimelineDefinition;
import org.joinmastodon.android.ui.BetterItemAnimator;
import org.joinmastodon.android.ui.SingleImagePhotoViewerListener;
import org.joinmastodon.android.ui.drawables.CoverOverlayGradientDrawable;
import org.joinmastodon.android.ui.photoviewer.PhotoViewer;
import org.joinmastodon.android.ui.text.CustomEmojiSpan;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.SimpleTextWatcher;
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
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.ViewImageLoader;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.views.UsableRecyclerView;

public class InstanceInfoFragment extends LoaderFragment {

	private Instance instance;
	private String extendedDescription;
	private CoverImageView cover;
	private TextView uri, description, readMore;

	private View spaceBelowText;
	private final CoverOverlayGradientDrawable coverGradient=new CoverOverlayGradientDrawable();

	private ScrollView scrollView, textScrollView;
	private float titleTransY;

	private String accountID;
	private Account account;
	private String targetDomain;
	private final ArrayList<AccountField> fields=new ArrayList<>();

	private boolean refreshing;
	private boolean isExpanded = false;
	private boolean updatedTimelines = false;

	private static final int MAX_FIELDS=4;

	// from ProfileAboutFragment
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

		scrollView=content.findViewById(R.id.scroll_view);
		cover=content.findViewById(R.id.cover);
		uri =content.findViewById(R.id.uri);
		description=content.findViewById(R.id.description);
		list=content.findViewById(R.id.metadata);
		textScrollView=content.findViewById(R.id.text_scroll_view);
		readMore=content.findViewById(R.id.read_more);
		spaceBelowText=content.findViewById(R.id.space_below_text);
		textMaxHeight=getActivity().getResources().getDimension(R.dimen.description_max_height);
		textCollapsedHeight=getActivity().getResources().getDimension(R.dimen.description_collapsed_height);
		collapseParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) textCollapsedHeight);
		wrapParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		cover.setForeground(coverGradient);
		cover.setOutlineProvider(new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setEmpty();
			}
		});

		cover.setOnClickListener(this::onCoverClick);
		readMore.setOnClickListener(this::onReadMoreClick);

		if(loaded){
			bindHeaderView();
			dataLoaded();
		}

		// from ProfileAboutFragment
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
						bindHeaderView();
						dataLoaded();
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
	}

	@Override
	public void onRefresh(){
		if(refreshing)
			return;
		refreshing=true;
		doLoadData();
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
		// To avoid the callback triggering on first layout with position=0 before anything is instantiated

		titleTransY=getToolbar().getLayoutParams().height;
		if(toolbarTitleView!=null){
			toolbarTitleView.setTranslationY(titleTransY);
			toolbarSubtitleView.setTranslationY(titleTransY);
		}
	}

	@Override
	public void onDestroyView(){
		super.onDestroyView();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (updatedTimelines) UiUtils.restartApp();
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



	private void bindHeaderView(){
		ViewImageLoader.load(cover, null, new UrlImageLoaderRequest(instance.thumbnail, 1000, 1000));
		uri.setText(instance.title);
		setTitle(instance.title);

		//set description text and collapse
		updateDescription();

		textScrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		readMore.setText(isExpanded ? R.string.sk_collapse : R.string.sk_expand);
		description.post(() -> {
			readMore.setVisibility(View.VISIBLE);
			textScrollView.setLayoutParams(isExpanded ? wrapParams : collapseParams);
			spaceBelowText.setVisibility(View.VISIBLE);
		});

		fields.clear();


		if (instance.contactAccount != null) {
			AccountField admin = new AccountField();
			admin.parsedName=admin.name= "Administered by";
			admin.parsedValue=buildLinkText(instance.contactAccount.url, instance.contactAccount.getDisplayUsername() + "@" + instance.uri);
			fields.add(admin);
		}

		if (instance.email != null) {
			AccountField contact = new AccountField();
			contact.parsedName = contact.name = "Contact";
			contact.parsedValue=buildLinkText("mailto:" + instance.email, instance.email);
			fields.add(contact);
		}

		if (instance.stats != null) {
			AccountField activeUsers = new AccountField();
			activeUsers.parsedName = activeUsers.name = "users";
			activeUsers.parsedValue= NumberFormat.getInstance().format(instance.stats.userCount);
			fields.add(activeUsers);
		}


		setFields(fields);
	}

	private SpannableStringBuilder buildLinkText(String link, String text) {
		String value = "<span class=\"h-card\"><a href=" + link + " class=\"u-url mention\" rel=\"nofollow noopener noreferrer\" target=\"_blank\">" + text + "</a></span>";
		return HtmlParser.parse(value, account.emojis, Collections.emptyList(), Collections.emptyList(), accountID);
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
		bindHeaderView();
	}


	private void onCoverClick(View v){
			Drawable drawable=cover.getDrawable();
			if(drawable==null || drawable instanceof ColorDrawable)
				return;
		new PhotoViewer(getActivity(), createFakeAttachments(instance.thumbnail, drawable), 0,
				new SingleImagePhotoViewerListener(cover, cover, null, this, () -> {
				}, () -> drawable, null, null));
	}

	private List<Attachment> createFakeAttachments(String url, Drawable drawable){
		Attachment att=new Attachment();
		att.type=Attachment.Type.IMAGE;
		att.url=url;
		att.meta=new Attachment.Metadata();
		att.meta.width=drawable.getIntrinsicWidth();
		att.meta.height=drawable.getIntrinsicHeight();
		return Collections.singletonList(att);
	}



	// from ProfileAboutFragment
	public void setFields(ArrayList<AccountField> fields){
		metadataListData=fields;
		if (adapter != null) adapter.notifyDataSetChanged();
	}

	private class MetadataAdapter extends UsableRecyclerView.Adapter<BaseViewHolder> implements ImageLoaderRecyclerAdapter {
		public MetadataAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return switch(viewType){
				case 0 -> new AboutViewHolder();
				case 1 -> new EditableAboutViewHolder();
				case 2 -> new AddRowViewHolder();
				default -> throw new IllegalStateException("Unexpected value: "+viewType);
			};
		}

		@Override
		public void onBindViewHolder(BaseViewHolder holder, int position){
			if(position<metadataListData.size()){
				holder.bind(metadataListData.get(position));
			}else{
				holder.bind(null);
			}
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

		@Override
		public int getImageCountForItem(int position){
//			return metadataListData.get(position).emojiRequests.size();
			return 0;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return metadataListData.get(position).emojiRequests.get(image);
		}
	}

	private abstract class BaseViewHolder extends BindableViewHolder<AccountField> {
		public BaseViewHolder(int layout){
			super(getActivity(), layout, list);
		}
	}

	private class AboutViewHolder extends BaseViewHolder implements ImageLoaderViewHolder {
		private TextView title;
		private LinkedTextView value;

		public AboutViewHolder(){
			super(R.layout.item_profile_about);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
		}

		@Override
		public void onBind(AccountField item){
			title.setText(item.parsedName);
			value.setText(item.parsedValue);
			if(item.verifiedAt!=null){
				int textColor=UiUtils.isDarkTheme() ? 0xFF89bb9c : 0xFF5b8e63;
				value.setTextColor(textColor);
				value.setLinkTextColor(textColor);
				Drawable check=getResources().getDrawable(R.drawable.ic_fluent_checkmark_24_regular, getActivity().getTheme()).mutate();
				check.setTint(textColor);
				value.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, check, null);
			}else{
				value.setTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.textColorPrimary));
				value.setLinkTextColor(UiUtils.getThemeColor(getActivity(), android.R.attr.colorAccent));
				value.setCompoundDrawables(null, null, null, null);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			CustomEmojiSpan span=index>=item.nameEmojis.length ? item.valueEmojis[index-item.nameEmojis.length] : item.nameEmojis[index];
			span.setDrawable(image);
			title.invalidate();
			value.invalidate();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}

	private class EditableAboutViewHolder extends BaseViewHolder {
		private EditText title;
		private EditText value;

		public EditableAboutViewHolder(){
			super(R.layout.item_profile_about_editable);
			title=findViewById(R.id.title);
			value=findViewById(R.id.value);
			findViewById(R.id.dragger_thingy).setOnLongClickListener(v-> true);
			title.addTextChangedListener(new SimpleTextWatcher(e->item.name=e.toString()));
			value.addTextChangedListener(new SimpleTextWatcher(e->item.value=e.toString()));
			findViewById(R.id.remove_row_btn).setOnClickListener(this::onRemoveRowClick);
		}

		@Override
		public void onBind(AccountField item){
			title.setText(item.name);
			value.setText(item.value);
		}

		private void onRemoveRowClick(View v){
			int pos=getAbsoluteAdapterPosition();
			metadataListData.remove(pos);
			adapter.notifyItemRemoved(pos);
			for(int i=0;i<list.getChildCount();i++){
				BaseViewHolder vh=(BaseViewHolder) list.getChildViewHolder(list.getChildAt(i));
				vh.rebind();
			}
		}
	}

	private class AddRowViewHolder extends BaseViewHolder implements UsableRecyclerView.Clickable{
		public AddRowViewHolder(){
			super(R.layout.item_profile_about_add_row);
		}

		@Override
		public void onClick(){
			metadataListData.add(new AccountField());
			if(metadataListData.size()==MAX_FIELDS){ // replace this row with new row
				adapter.notifyItemChanged(metadataListData.size()-1);
			}else{
				adapter.notifyItemInserted(metadataListData.size()-1);
				rebind();
			}
		}

		@Override
		public void onBind(AccountField item) {}
	}

}
