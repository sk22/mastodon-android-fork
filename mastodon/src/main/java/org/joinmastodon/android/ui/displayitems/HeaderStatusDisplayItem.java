package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Outline;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.accounts.GetAccountRelationships;
import org.joinmastodon.android.api.requests.statuses.CreateStatus;
import org.joinmastodon.android.api.requests.statuses.GetStatusSourceText;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.fragments.NotificationsListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.fragments.report.ReportReasonChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Attachment;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.model.Relationship;
import org.joinmastodon.android.model.ScheduledStatus;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import me.grishka.appkit.Nav;
import me.grishka.appkit.api.APIRequest;
import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class HeaderStatusDisplayItem extends StatusDisplayItem{
	private Account user;
	private Instant createdAt;
	private ImageLoaderRequest avaRequest;
	private String accountID;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private SpannableStringBuilder parsedName;
	public final Status status;
	private boolean hasVisibilityToggle;
	boolean needBottomPadding;
	private String extraText;
	private Notification notification;
	private ScheduledStatus scheduledStatus;

	public HeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, BaseStatusListFragment parentFragment, String accountID, Status status, String extraText, Notification notification, ScheduledStatus scheduledStatus){
		super(parentID, parentFragment);
		user=scheduledStatus != null ? AccountSessionManager.getInstance().getAccount(accountID).self : user;
		this.user=user;
		this.createdAt=createdAt;
		avaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? user.avatar : user.avatarStatic, V.dp(50), V.dp(50));
		this.accountID=accountID;
		parsedName=new SpannableStringBuilder(user.displayName);
		this.status=status;
		this.notification=notification;
		this.scheduledStatus=scheduledStatus;
		HtmlParser.parseCustomEmoji(parsedName, user.emojis);
		emojiHelper.setText(parsedName);
		if(status!=null){
			hasVisibilityToggle=status.sensitive || !TextUtils.isEmpty(status.spoilerText);
			if(!hasVisibilityToggle && !status.mediaAttachments.isEmpty()){
				for(Attachment att:status.mediaAttachments){
					if(att.type!=Attachment.Type.AUDIO){
						hasVisibilityToggle=true;
						break;
					}
				}
			}
		}
		this.extraText=extraText;
	}

	@Override
	public Type getType(){
		return Type.HEADER;
	}

	@Override
	public int getImageCount(){
		return 1+emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(index>0){
			return emojiHelper.getImageRequest(index-1);
		}
		return avaRequest;
	}

	public static class Holder extends StatusDisplayItem.Holder<HeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView name, username, timestamp, extraText;
		private final ImageView avatar, more, visibility, deleteNotification;
		private final PopupMenu optionsMenu;
		private Relationship relationship;
		private APIRequest<?> currentRelationshipRequest;

		private static final ViewOutlineProvider roundCornersOutline=new ViewOutlineProvider(){
			@Override
			public void getOutline(View view, Outline outline){
				outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), V.dp(12));
			}
		};

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_header, parent);
			name=findViewById(R.id.name);
			username=findViewById(R.id.username);
			timestamp=findViewById(R.id.timestamp);
			avatar=findViewById(R.id.avatar);
			more=findViewById(R.id.more);
			visibility=findViewById(R.id.visibility);
			deleteNotification=findViewById(R.id.delete_notification);
			extraText=findViewById(R.id.extra_text);
			avatar.setOnClickListener(this::onAvaClick);
			avatar.setOutlineProvider(roundCornersOutline);
			avatar.setClipToOutline(true);
			more.setOnClickListener(this::onMoreClick);
			visibility.setOnClickListener(v->item.parentFragment.onVisibilityIconClick(this));
			deleteNotification.setOnClickListener(v->UiUtils.confirmDeleteNotification(activity, item.parentFragment.getAccountID(), item.notification, ()->{
				if (item.parentFragment instanceof NotificationsListFragment fragment) {
					fragment.removeNotification(item.notification);
				}
			}));

			optionsMenu=new PopupMenu(activity, more);
			optionsMenu.inflate(R.menu.post);
			optionsMenu.setOnMenuItemClickListener(menuItem->{
				Account account=item.user;
				int id=menuItem.getItemId();

				if(id==R.id.edit || id==R.id.delete_and_redraft) {
					final Bundle args=new Bundle();
					args.putString("account", item.parentFragment.getAccountID());
					args.putParcelable("editStatus", Parcels.wrap(item.status));
					if (id==R.id.delete_and_redraft) {
						args.putBoolean("redraftStatus", true);
						if (item.parentFragment instanceof ThreadFragment thread && !thread.isItemEnabled(item.status.id)) {
							// ("enabled" = clickable; opened status is not clickable)
							// request navigation to the re-drafted status if status is currently opened
							args.putBoolean("navigateToStatus", true);
						}
					}
					if(TextUtils.isEmpty(item.status.content) && TextUtils.isEmpty(item.status.spoilerText)){
						Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
					}else if(item.scheduledStatus!=null){
						args.putString("sourceText", item.status.text);
						args.putString("sourceSpoiler", item.status.spoilerText);
						args.putBoolean("redraftStatus", true);
						args.putParcelable("scheduledStatus", Parcels.wrap(item.scheduledStatus));
						Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
					}else{
						new GetStatusSourceText(item.status.id)
								.setCallback(new Callback<>(){
									@Override
									public void onSuccess(GetStatusSourceText.Response result){
										args.putString("sourceText", result.text);
										args.putString("sourceSpoiler", result.spoilerText);
										if (id==R.id.delete_and_redraft) {
											UiUtils.confirmDeletePost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, s->{
												Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
											}, true);
										} else {
											Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
										}
									}

									@Override
									public void onError(ErrorResponse error){
										error.showToast(item.parentFragment.getActivity());
									}
								})
								.wrapProgress(item.parentFragment.getActivity(), R.string.loading, true)
								.exec(item.parentFragment.getAccountID());
					}
				}else if(id==R.id.delete){
					if (item.scheduledStatus != null) {
						UiUtils.confirmDeleteScheduledPost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.scheduledStatus, ()->{});
					} else {
						UiUtils.confirmDeletePost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, s->{});
					}
				}else if(id==R.id.pin || id==R.id.unpin) {
					UiUtils.confirmPinPost(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), item.status, !item.status.pinned, s->{});
				}else if(id==R.id.mute){
					UiUtils.confirmToggleMuteUser(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), account, relationship!=null && relationship.muting, r->{});
				}else if(id==R.id.block){
					UiUtils.confirmToggleBlockUser(item.parentFragment.getActivity(), item.parentFragment.getAccountID(), account, relationship!=null && relationship.blocking, r->{});
				}else if(id==R.id.report){
					Bundle args=new Bundle();
					args.putString("account", item.parentFragment.getAccountID());
					args.putParcelable("status", Parcels.wrap(item.status));
					args.putParcelable("reportAccount", Parcels.wrap(item.status.account));
					Nav.go(item.parentFragment.getActivity(), ReportReasonChoiceFragment.class, args);
				}else if(id==R.id.open_in_browser) {
					UiUtils.launchWebBrowser(activity, item.status.url);
				}else if(id==R.id.copy_link){
					UiUtils.copyText(parent, item.status.url);
				}else if(id==R.id.follow){
					if(relationship==null)
						return true;
					ProgressDialog progress=new ProgressDialog(activity);
					progress.setCancelable(false);
					progress.setMessage(activity.getString(R.string.loading));
					UiUtils.performAccountAction(activity, account, item.parentFragment.getAccountID(), relationship, null, visible->{
						if(visible)
							progress.show();
						else
							progress.dismiss();
					}, rel->{
						relationship=rel;
						Toast.makeText(activity, activity.getString(rel.following ? R.string.followed_user : R.string.unfollowed_user, account.getShortUsername()), Toast.LENGTH_SHORT).show();
					});
				}else if(id==R.id.block_domain){
					UiUtils.confirmToggleBlockDomain(activity, item.parentFragment.getAccountID(), account.getDomain(), relationship!=null && relationship.domainBlocking, ()->{});
				}else if(id==R.id.bookmark){
					AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setBookmarked(item.status, !item.status.bookmarked);
				}
				return true;
			});
			UiUtils.enablePopupMenuIcons(activity, optionsMenu);
		}

		private void populateAccountsMenu(Menu menu) {
			List<AccountSession> sessions=AccountSessionManager.getInstance().getLoggedInAccounts();
			sessions.stream().filter(s -> !s.getID().equals(item.accountID)).forEach(s -> {
				String username = "@"+s.self.username+"@"+s.domain;
				menu.add(username).setOnMenuItemClickListener(c->{
					UiUtils.openURL(item.parentFragment.getActivity(), s.getID(), item.status.url, false);
					return true;
				});
			});
		}

		@Override
		public void onBind(HeaderStatusDisplayItem item){
			name.setText(item.parsedName);
			username.setText('@'+item.user.acct);
			if (item.scheduledStatus!=null)
				if (item.scheduledStatus.scheduledAt.isAfter(CreateStatus.DRAFTS_AFTER_INSTANT)) {
					timestamp.setText(R.string.sk_draft);
				} else {
					DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault());
					timestamp.setText(item.scheduledStatus.scheduledAt.atZone(ZoneId.systemDefault()).format(formatter));
				}
			else if(item.status==null || item.status.editedAt==null)
				timestamp.setText(UiUtils.formatRelativeTimestamp(itemView.getContext(), item.createdAt));
			else
				timestamp.setText(item.parentFragment.getString(R.string.edited_timestamp, UiUtils.formatRelativeTimestamp(itemView.getContext(), item.status.editedAt)));
			visibility.setVisibility(item.hasVisibilityToggle && !item.inset ? View.VISIBLE : View.GONE);
			deleteNotification.setVisibility(GlobalUserPreferences.enableDeleteNotifications && item.notification!=null && !item.inset ? View.VISIBLE : View.GONE);
			if(item.hasVisibilityToggle){
				visibility.setImageResource(item.status.spoilerRevealed ? R.drawable.ic_visibility_off : R.drawable.ic_visibility);
				visibility.setContentDescription(item.parentFragment.getString(item.status.spoilerRevealed ? R.string.hide_content : R.string.reveal_content));
				if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
					visibility.setTooltipText(visibility.getContentDescription());
				}
			}
			itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), item.needBottomPadding ? V.dp(16) : 0);
			if(TextUtils.isEmpty(item.extraText)){
				extraText.setVisibility(View.GONE);
			}else{
				extraText.setVisibility(View.VISIBLE);
				extraText.setText(item.extraText);
			}
			more.setVisibility(item.inset ? View.GONE : View.VISIBLE);
			avatar.setClickable(!item.inset);
			avatar.setContentDescription(item.parentFragment.getString(R.string.avatar_description, item.user.acct));
			if(currentRelationshipRequest!=null){
				currentRelationshipRequest.cancel();
			}
			relationship=null;
		}

		@Override
		public void setImage(int index, Drawable drawable){
			if(index>0){
				item.emojiHelper.setImageDrawable(index-1, drawable);
				name.invalidate();
			}else{
				avatar.setImageDrawable(drawable);
			}
			if(drawable instanceof Animatable)
				((Animatable) drawable).start();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}

		private void onAvaClick(View v){
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.user));
			Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
		}

		private void onMoreClick(View v){
			updateOptionsMenu();
			optionsMenu.show();
			if(relationship==null && currentRelationshipRequest==null){
				currentRelationshipRequest=new GetAccountRelationships(Collections.singletonList(item.user.id))
						.setCallback(new Callback<>(){
							@Override
							public void onSuccess(List<Relationship> result){
								if(!result.isEmpty()){
									relationship=result.get(0);
									updateOptionsMenu();
								}
								currentRelationshipRequest=null;
							}

							@Override
							public void onError(ErrorResponse error){
								currentRelationshipRequest=null;
							}
						})
						.exec(item.parentFragment.getAccountID());
			}
		}

		private void updateOptionsMenu(){
			boolean hasMultipleAccounts = AccountSessionManager.getInstance().getLoggedInAccounts().size() > 1;
			Menu menu=optionsMenu.getMenu();

			MenuItem openWithAccounts = menu.findItem(R.id.open_with_account);
			SubMenu accountsMenu = openWithAccounts != null ? openWithAccounts.getSubMenu() : null;
			if (hasMultipleAccounts && accountsMenu != null) {
				openWithAccounts.setVisible(true);
				accountsMenu.clear();
				populateAccountsMenu(accountsMenu);
			} else if (openWithAccounts != null) {
				openWithAccounts.setVisible(false);
			}

			Account account=item.user;
			boolean isOwnPost=AccountSessionManager.getInstance().isSelf(item.parentFragment.getAccountID(), account);
			boolean isPostScheduled=item.scheduledStatus!=null;
			menu.findItem(R.id.open_with_account).setVisible(!isPostScheduled && hasMultipleAccounts);
			menu.findItem(R.id.edit).setVisible(item.status!=null && isOwnPost);
			menu.findItem(R.id.delete).setVisible(item.status!=null && isOwnPost);
			menu.findItem(R.id.delete_and_redraft).setVisible(!isPostScheduled && item.status!=null && isOwnPost);
			menu.findItem(R.id.pin).setVisible(!isPostScheduled && item.status!=null && isOwnPost && !item.status.pinned);
			menu.findItem(R.id.unpin).setVisible(!isPostScheduled && item.status!=null && isOwnPost && item.status.pinned);
			menu.findItem(R.id.open_in_browser).setVisible(!isPostScheduled && item.status!=null);
			menu.findItem(R.id.copy_link).setVisible(!isPostScheduled && item.status!=null);
			MenuItem blockDomain=menu.findItem(R.id.block_domain);
			MenuItem mute=menu.findItem(R.id.mute);
			MenuItem block=menu.findItem(R.id.block);
			MenuItem report=menu.findItem(R.id.report);
			MenuItem follow=menu.findItem(R.id.follow);
			MenuItem bookmark=menu.findItem(R.id.bookmark);
			bookmark.setVisible(false);
			/* disabled in megalodon: add/remove bookmark is already available through status footer
			if(item.status!=null){
				bookmark.setVisible(true);
				bookmark.setTitle(item.status.bookmarked ? R.string.remove_bookmark : R.string.add_bookmark);
			}else{
				bookmark.setVisible(false);
			}
			*/
			if(isPostScheduled || isOwnPost){
				mute.setVisible(false);
				block.setVisible(false);
				report.setVisible(false);
				follow.setVisible(false);
				blockDomain.setVisible(false);
			}else{
				mute.setVisible(true);
				block.setVisible(true);
				report.setVisible(true);
				follow.setVisible(relationship==null || relationship.following || (!relationship.blocking && !relationship.blockedBy && !relationship.domainBlocking && !relationship.muting));
				mute.setTitle(item.parentFragment.getString(relationship!=null && relationship.muting ? R.string.unmute_user : R.string.mute_user, account.getShortUsername()));
				mute.setIcon(relationship!=null && relationship.muting ? R.drawable.ic_fluent_speaker_0_24_regular : R.drawable.ic_fluent_speaker_off_24_regular);
				UiUtils.insetPopupMenuIcon(item.parentFragment.getContext(), mute);
				block.setTitle(item.parentFragment.getString(relationship!=null && relationship.blocking ? R.string.unblock_user : R.string.block_user, account.getShortUsername()));
				report.setTitle(item.parentFragment.getString(R.string.report_user, account.getShortUsername()));
				// disabled in megalodon. domain blocks from a post clutters the context menu and looks out of place
//				if(!account.isLocal()){
//					blockDomain.setVisible(true);
//					blockDomain.setTitle(item.parentFragment.getString(relationship!=null && relationship.domainBlocking ? R.string.unblock_domain : R.string.block_domain, account.getDomain()));
//				}else{
					blockDomain.setVisible(false);
//				}
				boolean following = relationship!=null && relationship.following;
				follow.setTitle(item.parentFragment.getString(following ? R.string.unfollow_user : R.string.follow_user, account.getShortUsername()));
				follow.setIcon(following ? R.drawable.ic_fluent_person_delete_24_regular : R.drawable.ic_fluent_person_add_24_regular);
				UiUtils.insetPopupMenuIcon(item.parentFragment.getContext(), follow);
			}
		}
	}
}
