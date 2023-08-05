package org.joinmastodon.android.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.MainActivity;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.oauth.RevokeOauthToken;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.HomeFragment;
import org.joinmastodon.android.fragments.SplashFragment;
import org.joinmastodon.android.fragments.onboarding.CustomWelcomeFragment;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.CheckableRelativeLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import me.grishka.appkit.Nav;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderRecyclerAdapter;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.ListImageLoaderWrapper;
import me.grishka.appkit.imageloader.RecyclerViewDelegate;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.BindableViewHolder;
import me.grishka.appkit.utils.MergeRecyclerAdapter;
import me.grishka.appkit.utils.SingleViewRecyclerAdapter;
import me.grishka.appkit.utils.V;
import me.grishka.appkit.views.BottomSheet;
import me.grishka.appkit.views.UsableRecyclerView;

public class AccountSwitcherSheet extends BottomSheet{
	private final Activity activity;
	private final HomeFragment fragment;
	private final boolean externalShare, openInApp;
	private BiConsumer<String, Boolean> onClick;
	private UsableRecyclerView list;
	private List<WrappedAccount> accounts;
	private ListImageLoaderWrapper imgLoader;
	private AccountsAdapter accountsAdapter;

	public AccountSwitcherSheet(@NonNull Activity activity, @Nullable HomeFragment fragment){
		this(activity, fragment, false, false);
	}

	public AccountSwitcherSheet(@NonNull Activity activity, @Nullable HomeFragment fragment, boolean externalShare, boolean openInApp){
		super(activity);
		this.activity=activity;
		this.fragment=fragment;
		this.externalShare = externalShare;
		this.openInApp = openInApp;

		accounts=AccountSessionManager.getInstance().getLoggedInAccounts().stream().map(WrappedAccount::new).collect(Collectors.toList());

		list=new UsableRecyclerView(activity);
		imgLoader=new ListImageLoaderWrapper(activity, list, new RecyclerViewDelegate(list), null);
		list.setClipToPadding(false);
		list.setLayoutManager(new LinearLayoutManager(activity));

		MergeRecyclerAdapter adapter=new MergeRecyclerAdapter();
		View handle=new View(activity);
		handle.setBackgroundResource(R.drawable.bg_bottom_sheet_handle);
		handle.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, V.dp(36)));

		adapter.addAdapter(new SingleViewRecyclerAdapter(handle));

		if (externalShare) {
			FrameLayout shareHeading = new FrameLayout(activity);
			activity.getLayoutInflater().inflate(R.layout.item_external_share_heading, shareHeading);
			((TextView) shareHeading.findViewById(R.id.title)).setText(openInApp
					? R.string.sk_external_share_or_open_title
					: R.string.sk_external_share_title);
			adapter.addAdapter(new SingleViewRecyclerAdapter(shareHeading));

			setOnDismissListener((d) -> activity.finish());
		}

		adapter.addAdapter(accountsAdapter = new AccountsAdapter());

		if (!externalShare) {
			adapter.addAdapter(new ClickableSingleViewRecyclerAdapter(makeSimpleListItem(R.string.add_account, R.drawable.ic_fluent_add_24_regular), () -> {
				Nav.go(activity, CustomWelcomeFragment.class, null);
				dismiss();
			}));
			// disabled in megalodon
//			adapter.addAdapter(new ClickableSingleViewRecyclerAdapter(makeSimpleListItem(R.string.log_out_all_accounts, R.drawable.ic_fluent_person_arrow_right_24_filled), this::confirmLogOutAll));
		}

		list.setAdapter(adapter);

		FrameLayout content=new FrameLayout(activity);
		content.setBackgroundResource(R.drawable.bg_bottom_sheet);
		content.addView(list);
		setContentView(content);
		setNavigationBarBackground(new ColorDrawable(UiUtils.alphaBlendColors(UiUtils.getThemeColor(activity, R.attr.colorM3Surface),
				UiUtils.getThemeColor(activity, R.attr.colorM3Primary), 0.05f)), !UiUtils.isDarkTheme());
	}

	public void setOnClick(BiConsumer<String, Boolean> onClick) {
		this.onClick = onClick;
	}

	private void confirmLogOut(String accountID){
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		new M3AlertDialogBuilder(activity)
				.setMessage(activity.getString(R.string.confirm_log_out, session.getFullUsername()))
				.setPositiveButton(R.string.log_out, (dialog, which) -> logOut(accountID))
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void confirmLogOutAll(){
		new M3AlertDialogBuilder(activity)
				.setMessage(R.string.confirm_log_out_all_accounts)
				.setPositiveButton(R.string.log_out, (dialog, which) -> logOutAll())
				.setNegativeButton(R.string.cancel, null)
				.show();
	}

	private void logOut(String accountID){
		AccountSession session=AccountSessionManager.getInstance().getAccount(accountID);
		new RevokeOauthToken(session.app.clientId, session.app.clientSecret, session.token.accessToken)
				.setCallback(new Callback<>(){
					@Override
					public void onSuccess(Object result){
						onLoggedOut(accountID);
					}

					@Override
					public void onError(ErrorResponse error){
						onLoggedOut(accountID);
					}
				})
				.wrapProgress(activity, R.string.loading, false)
				.exec(accountID);
	}

	private void logOutAll(){
		final ProgressDialog progress=new ProgressDialog(activity);
		progress.setMessage(activity.getString(R.string.loading));
		progress.setCancelable(false);
		progress.show();
		ArrayList<AccountSession> sessions=new ArrayList<>(AccountSessionManager.getInstance().getLoggedInAccounts());
		for(AccountSession session:sessions){
			new RevokeOauthToken(session.app.clientId, session.app.clientSecret, session.token.accessToken)
					.setCallback(new Callback<>(){
						@Override
						public void onSuccess(Object result){
							AccountSessionManager.getInstance().removeAccount(session.getID());
							sessions.remove(session);
							if(sessions.isEmpty()){
								progress.dismiss();
								Nav.goClearingStack(activity, SplashFragment.class, null);
								dismiss();
							}
						}

						@Override
						public void onError(ErrorResponse error){
							AccountSessionManager.getInstance().removeAccount(session.getID());
							sessions.remove(session);
							if(sessions.isEmpty()){
								progress.dismiss();
								Nav.goClearingStack(activity, SplashFragment.class, null);
								dismiss();
							}
						}
					})
					.exec(session.getID());
		}
	}

	private void onLoggedOut(String accountID){
		AccountSessionManager.getInstance().removeAccount(accountID);
		String activeAccountID = fragment != null
				? fragment.getAccountID()
				: AccountSessionManager.getInstance().getLastActiveAccountID();
		if (accountID.equals(activeAccountID)) {
			activity.finish();
			activity.startActivity(new Intent(activity, MainActivity.class));
		} else {
			accounts.stream().filter(w -> accountID.equals(w.session.getID())).findAny().ifPresent(w -> {
				accountsAdapter.notifyItemRemoved(accounts.indexOf(w));
				accounts.remove(w);
			});
		}
	}

	@Override
	protected void onWindowInsetsUpdated(WindowInsets insets){
		if(Build.VERSION.SDK_INT>=29){
			int tappableBottom=insets.getTappableElementInsets().bottom;
			int insetBottom=insets.getSystemWindowInsetBottom();
			if(tappableBottom==0 && insetBottom>0){
				list.setPadding(0, 0, 0, V.dp(48)-insetBottom);
			}else{
				list.setPadding(0, 0, 0, V.dp(24));
			}
		}else{
			list.setPadding(0, 0, 0, V.dp(24));
		}
	}

	private View makeSimpleListItem(@StringRes int title, @DrawableRes int icon){
		TextView tv=(TextView) activity.getLayoutInflater().inflate(R.layout.item_text_with_icon, list, false);
		tv.setText(title);
		tv.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
		return tv;
	}

	private class AccountsAdapter extends UsableRecyclerView.Adapter<AccountViewHolder> implements ImageLoaderRecyclerAdapter{
		public AccountsAdapter(){
			super(imgLoader);
		}

		@NonNull
		@Override
		public AccountViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
			return new AccountViewHolder();
		}

		@Override
		public int getItemCount(){
			return accounts.size();
		}

		@Override
		public void onBindViewHolder(AccountViewHolder holder, int position){
			holder.bind(accounts.get(position).session);
			super.onBindViewHolder(holder, position);
		}

		@Override
		public int getImageCountForItem(int position){
			return 1;
		}

		@Override
		public ImageLoaderRequest getImageRequest(int position, int image){
			return accounts.get(position).req;
		}
	}

	private class AccountViewHolder extends BindableViewHolder<AccountSession> implements ImageLoaderViewHolder, UsableRecyclerView.Clickable, UsableRecyclerView.LongClickable{
		private final TextView name, username;
		private final ImageView avatar;
		private final CheckableRelativeLayout view;
		private final View radioButton, extraBtnWrap;
		private final ImageButton extraBtn;

		public AccountViewHolder(){
			super(activity, R.layout.item_account_switcher, list);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			radioButton=findViewById(R.id.radiobtn);
			radioButton.setBackground(new RadioButton(activity).getButtonDrawable());
			avatar=findViewById(R.id.avatar);
			avatar.setOutlineProvider(OutlineProviders.roundedRect(OutlineProviders.RADIUS_MEDIUM));
			avatar.setClipToOutline(true);
			view=(CheckableRelativeLayout) itemView;
			extraBtnWrap = findViewById(R.id.extra_btn_wrap);
			extraBtn = findViewById(R.id.extra_btn);
			extraBtn.setOnClickListener(this::onExtraBtnClick);
		}

		@SuppressLint("SetTextI18n")
		@Override
		public void onBind(AccountSession item){
			name.setText(item.self.displayName);
			username.setText(item.getFullUsername());
			radioButton.setVisibility(externalShare ? View.GONE : View.VISIBLE);
			extraBtnWrap.setVisibility(externalShare && openInApp ? View.VISIBLE : View.GONE);
			if (externalShare) view.setCheckable(false);
			else {
				String accountId = fragment != null
						? fragment.getAccountID()
						: AccountSessionManager.getInstance().getLastActiveAccountID();
				view.setChecked(accountId.equals(item.getID()));
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			avatar.setImageDrawable(image);
			if(image instanceof Animatable a)
				a.start();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}

		private void onExtraBtnClick(View view) {
			setOnDismissListener(null);
			dismiss();
			onClick.accept(item.getID(), true);
		}

		@Override
		public void onClick(){
			setOnDismissListener(null);
			if (onClick != null) {
				dismiss();
				onClick.accept(item.getID(), false);
				return;
			}
			if(AccountSessionManager.getInstance().tryGetAccount(item.getID())!=null)
				AccountSessionManager.getInstance().setLastActiveAccountID(item.getID());
			activity.finish();
			activity.startActivity(new Intent(activity, MainActivity.class));
		}

		@Override
		public boolean onLongClick(){
			if (externalShare) return false;
			confirmLogOut(item.getID());
			return true;
		}
	}

	private static class WrappedAccount{
		public final AccountSession session;
		public final ImageLoaderRequest req;

		public WrappedAccount(AccountSession session){
			this.session=session;
			if(session.self.avatar!=null)
				req=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? session.self.avatar : session.self.avatarStatic, V.dp(50), V.dp(50));
			else
				req=null;
		}
	}
}
