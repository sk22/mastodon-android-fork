package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ComposeFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.M3AlertDialogBuilder;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.util.function.Consumer;

import me.grishka.appkit.Nav;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class FooterStatusDisplayItem extends StatusDisplayItem{
	public final Status status;
	private final String accountID;
	public boolean hideCounts;

	public FooterStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Status status, String accountID){
		super(parentID, parentFragment);
		this.status=status;
		this.accountID=accountID;
	}

	@Override
	public Type getType(){
		return Type.FOOTER;
	}

	public static class Holder extends StatusDisplayItem.Holder<FooterStatusDisplayItem>{
		private final TextView replies, boosts, favorites;
		private final View reply, boost, favorite, share, bookmark;
		private static final Animation opacityOut, opacityIn;

		private View touchingView = null;
		private boolean longClickPerformed = false;
		private final Runnable longClickRunnable = () -> {
			longClickPerformed = touchingView != null && touchingView.performLongClick();
			if (longClickPerformed && touchingView != null) {
				touchingView.startAnimation(opacityIn);
				touchingView.animate().scaleX(1).scaleY(1).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(150).start();
			}
		};

		private final View.AccessibilityDelegate buttonAccessibilityDelegate=new View.AccessibilityDelegate(){
			@Override
			public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info){
				super.onInitializeAccessibilityNodeInfo(host, info);
				info.setClassName(Button.class.getName());
				info.setText(item.parentFragment.getString(descriptionForId(host.getId())));
			}
		};

		static {
			opacityOut = new AlphaAnimation(1, 0.55f);
			opacityOut.setDuration(300);
			opacityOut.setInterpolator(CubicBezierInterpolator.DEFAULT);
			opacityOut.setFillAfter(true);
			opacityIn = new AlphaAnimation(0.55f, 1);
			opacityIn.setDuration(400);
			opacityIn.setInterpolator(CubicBezierInterpolator.DEFAULT);
		}

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_footer, parent);
			replies=findViewById(R.id.reply);
			boosts=findViewById(R.id.boost);
			favorites=findViewById(R.id.favorite);

			reply=findViewById(R.id.reply_btn);
			boost=findViewById(R.id.boost_btn);
			favorite=findViewById(R.id.favorite_btn);
			share=findViewById(R.id.share_btn);
			bookmark=findViewById(R.id.bookmark_btn);

			reply.setOnTouchListener(this::onButtonTouch);
			reply.setOnClickListener(this::onReplyClick);
			reply.setOnLongClickListener(this::onReplyLongClick);
			reply.setAccessibilityDelegate(buttonAccessibilityDelegate);
			boost.setOnTouchListener(this::onButtonTouch);
			boost.setOnClickListener(this::onBoostClick);
			boost.setOnLongClickListener(this::onBoostLongClick);
			boost.setAccessibilityDelegate(buttonAccessibilityDelegate);
			favorite.setOnTouchListener(this::onButtonTouch);
			favorite.setOnClickListener(this::onFavoriteClick);
			favorite.setOnLongClickListener(this::onFavoriteLongClick);
			favorite.setAccessibilityDelegate(buttonAccessibilityDelegate);
			bookmark.setOnTouchListener(this::onButtonTouch);
			bookmark.setOnClickListener(this::onBookmarkClick);
			bookmark.setOnLongClickListener(this::onBookmarkLongClick);
			bookmark.setAccessibilityDelegate(buttonAccessibilityDelegate);
			share.setOnTouchListener(this::onButtonTouch);
			share.setOnClickListener(this::onShareClick);
			share.setOnLongClickListener(this::onShareLongClick);
			share.setAccessibilityDelegate(buttonAccessibilityDelegate);
		}

		@Override
		public void onBind(FooterStatusDisplayItem item){
			bindText(replies, item.status.repliesCount);
			bindText(boosts, item.status.reblogsCount);
			bindText(favorites, item.status.favouritesCount);
			// in thread view, direct descendant posts display one direct reply to themselves,
			// hence in that case displaying whether there is another reply
			int compareTo = item.isMainStatus || !item.hasDescendantNeighbor ? 0 : 1;
			reply.setSelected(item.status.repliesCount > compareTo);
			boost.setSelected(item.status.reblogged);
			favorite.setSelected(item.status.favourited);
			bookmark.setSelected(item.status.bookmarked);
			boost.setEnabled(item.status.isReblogPermitted(item.accountID));

			int nextPos = getAbsoluteAdapterPosition() + 1;
			boolean nextIsWarning = item.parentFragment.getDisplayItems().size() > nextPos &&
					item.parentFragment.getDisplayItems().get(nextPos) instanceof WarningFilteredStatusDisplayItem;
			boolean condenseBottom = !item.isMainStatus && item.hasDescendantNeighbor &&
					!nextIsWarning;

			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
			params.setMargins(params.leftMargin, params.topMargin, params.rightMargin,
					condenseBottom ? V.dp(-5) : 0);

			itemView.requestLayout();
		}

		private void bindText(TextView btn, long count){
			if(GlobalUserPreferences.showInteractionCounts && count>0 && !item.hideCounts){
				btn.setText(UiUtils.abbreviateNumber(count));
				btn.setCompoundDrawablePadding(V.dp(8));
			}else{
				btn.setText("");
				btn.setCompoundDrawablePadding(0);
			}
		}

		private boolean onButtonTouch(View v, MotionEvent event){
			boolean disabled = !v.isEnabled() || (v instanceof FrameLayout parentFrame &&
					parentFrame.getChildCount() > 0 && !parentFrame.getChildAt(0).isEnabled());
			int action = event.getAction();
			if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
				touchingView = null;
				v.removeCallbacks(longClickRunnable);
				if (!longClickPerformed) v.animate().scaleX(1).scaleY(1).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(150).start();
				if (disabled) return true;
				if (action == MotionEvent.ACTION_UP && !longClickPerformed) v.performClick();
				else if (!longClickPerformed) v.startAnimation(opacityIn);
			} else if (action == MotionEvent.ACTION_DOWN) {
				longClickPerformed = false;
				touchingView = v;
				v.setPivotX(V.sp(28));
				v.animate().scaleX(0.85f).scaleY(0.85f).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(75).start();
				if (disabled) return true;
				v.postDelayed(longClickRunnable, ViewConfiguration.getLongPressTimeout());
				v.startAnimation(opacityOut);
			}
			return true;
		}

		private void onReplyClick(View v){
			v.startAnimation(opacityIn);
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("replyTo", Parcels.wrap(item.status));
			Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
		}

		private boolean onReplyLongClick(View v) {
			if (AccountSessionManager.getInstance().getLoggedInAccounts().size() < 2) return false;
			UiUtils.pickAccount(v.getContext(), item.accountID, R.string.sk_reply_as, R.drawable.ic_fluent_arrow_reply_28_regular, session -> {
				Bundle args=new Bundle();
				String accountID = session.getID();
				args.putString("account", accountID);
				UiUtils.lookupStatus(v.getContext(), item.status, accountID, item.accountID, status -> {
					if (status == null) return;
					args.putParcelable("replyTo", Parcels.wrap(status));
					Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
				});
			}, null);
			return true;
		}

		private void onBoostClick(View v){
			if (GlobalUserPreferences.confirmBeforeReblog) {
				v.startAnimation(opacityIn);
				onBoostLongClick(v);
				return;
			}
			boost.setSelected(!item.status.reblogged);
			AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setReblogged(item.status, !item.status.reblogged, null, r->boostConsumer(v, r));
		}

		private void boostConsumer(View v, Status r) {
			v.startAnimation(opacityIn);
			bindText(boosts, r.reblogsCount);
		}

		private boolean onBoostLongClick(View v){
			Context ctx = itemView.getContext();
			View menu = LayoutInflater.from(ctx).inflate(R.layout.item_boost_menu, null);
			Dialog dialog = new M3AlertDialogBuilder(ctx).setView(menu).create();
			AccountSession session = AccountSessionManager.getInstance().getAccount(item.accountID);

			Consumer<StatusPrivacy> doReblog = (visibility) -> {
				v.startAnimation(opacityOut);
				session.getStatusInteractionController()
						.setReblogged(item.status, !item.status.reblogged, visibility, r->boostConsumer(v, r));
				dialog.dismiss();
			};

			View separator = menu.findViewById(R.id.separator);
			TextView reblogHeader = menu.findViewById(R.id.reblog_header);
			TextView undoReblog = menu.findViewById(R.id.delete_reblog);
			TextView reblogAs = menu.findViewById(R.id.reblog_as);
			TextView itemPublic = menu.findViewById(R.id.vis_public);
			TextView itemUnlisted = menu.findViewById(R.id.vis_unlisted);
			TextView itemFollowers = menu.findViewById(R.id.vis_followers);

			undoReblog.setVisibility(item.status.reblogged ? View.VISIBLE : View.GONE);
			separator.setVisibility(item.status.reblogged ? View.GONE : View.VISIBLE);
			reblogHeader.setVisibility(item.status.reblogged ? View.GONE : View.VISIBLE);
			reblogAs.setVisibility(AccountSessionManager.getInstance().getLoggedInAccounts().size() > 1 ? View.VISIBLE : View.GONE);

			itemPublic.setVisibility(item.status.reblogged ? View.GONE : View.VISIBLE);
			itemUnlisted.setVisibility(item.status.reblogged ? View.GONE : View.VISIBLE);
			itemFollowers.setVisibility(item.status.reblogged ? View.GONE : View.VISIBLE);

			Drawable checkMark = ctx.getDrawable(R.drawable.ic_fluent_checkmark_circle_20_regular);
			Drawable publicDrawable = ctx.getDrawable(R.drawable.ic_fluent_earth_24_regular);
			Drawable unlistedDrawable = ctx.getDrawable(R.drawable.ic_fluent_lock_open_24_regular);
			Drawable followersDrawable = ctx.getDrawable(R.drawable.ic_fluent_lock_closed_24_regular);

			StatusPrivacy defaultVisibility = session.preferences != null ? session.preferences.postingDefaultVisibility : null;
			itemPublic.setCompoundDrawablesWithIntrinsicBounds(publicDrawable, null, StatusPrivacy.PUBLIC.equals(defaultVisibility) ? checkMark : null, null);
			itemUnlisted.setCompoundDrawablesWithIntrinsicBounds(unlistedDrawable, null, StatusPrivacy.UNLISTED.equals(defaultVisibility) ? checkMark : null, null);
			itemFollowers.setCompoundDrawablesWithIntrinsicBounds(followersDrawable, null, StatusPrivacy.PRIVATE.equals(defaultVisibility) ? checkMark : null, null);

			undoReblog.setOnClickListener(c->doReblog.accept(null));
			itemPublic.setOnClickListener(c->doReblog.accept(StatusPrivacy.PUBLIC));
			itemUnlisted.setOnClickListener(c->doReblog.accept(StatusPrivacy.UNLISTED));
			itemFollowers.setOnClickListener(c->doReblog.accept(StatusPrivacy.PRIVATE));
			reblogAs.setOnClickListener(c->{
				dialog.dismiss();
				UiUtils.pickInteractAs(v.getContext(),
						item.accountID, item.status,
						s -> s.reblogged,
						(ic, status, consumer) -> ic.setReblogged(status, true, null, consumer),
						R.string.sk_reblog_as,
						R.string.sk_reblogged_as,
						R.string.sk_already_reblogged,
						// TODO: replace once available: https://raw.githubusercontent.com/microsoft/fluentui-system-icons/main/android/library/src/main/res/drawable/ic_fluent_arrow_repeat_all_28_regular.xml
						R.drawable.ic_fluent_arrow_repeat_all_24_regular
				);
			});

			menu.findViewById(R.id.quote).setOnClickListener(c->{
				dialog.dismiss();
				v.startAnimation(opacityIn);
				Bundle args=new Bundle();
				args.putString("account", item.accountID);
				AccountSession accountSession=AccountSessionManager.getInstance().getAccount(item.accountID);
				Instance instance=AccountSessionManager.getInstance().getInstanceInfo(accountSession.domain);
				if(instance.pleroma == null){
					StringBuilder prefilledText = new StringBuilder().append("\n\n");
					String ownID = AccountSessionManager.getInstance().getAccount(item.accountID).self.id;
					if (!item.status.account.id.equals(ownID)) prefilledText.append('@').append(item.status.account.acct).append(' ');
					prefilledText.append(item.status.url);
					args.putString("prefilledText", prefilledText.toString());
					args.putInt("selectionStart", 0);
				}else{
					args.putParcelable("quote", Parcels.wrap(item.status));
				}
				Nav.go(item.parentFragment.getActivity(), ComposeFragment.class, args);
			});

			dialog.show();
			return true;
		}

		private void onFavoriteClick(View v){
			favorite.setSelected(!item.status.favourited);
			AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setFavorited(item.status, !item.status.favourited, r->{
				v.startAnimation(opacityIn);
				bindText(favorites, r.favouritesCount);
			});
		}

		private boolean onFavoriteLongClick(View v) {
			if (AccountSessionManager.getInstance().getLoggedInAccounts().size() < 2) return false;
			UiUtils.pickInteractAs(v.getContext(),
					item.accountID, item.status,
					s -> s.favourited,
					(ic, status, consumer) -> ic.setFavorited(status, true, consumer),
					R.string.sk_favorite_as,
					R.string.sk_favorited_as,
					R.string.sk_already_favorited,
					R.drawable.ic_fluent_star_28_regular
			);
			return true;
		}

		private void onBookmarkClick(View v){
			bookmark.setSelected(!item.status.bookmarked);
			AccountSessionManager.getInstance().getAccount(item.accountID).getStatusInteractionController().setBookmarked(item.status, !item.status.bookmarked, r->{
				v.startAnimation(opacityIn);
			});
		}

		private boolean onBookmarkLongClick(View v) {
			if (AccountSessionManager.getInstance().getLoggedInAccounts().size() < 2) return false;
			UiUtils.pickInteractAs(v.getContext(),
					item.accountID, item.status,
					s -> s.bookmarked,
					(ic, status, consumer) -> ic.setBookmarked(status, true, consumer),
					R.string.sk_bookmark_as,
					R.string.sk_bookmarked_as,
					R.string.sk_already_bookmarked,
					R.drawable.ic_fluent_bookmark_28_regular
			);
			return true;
		}

		private void onShareClick(View v){
			v.startAnimation(opacityIn);
			Intent intent=new Intent(Intent.ACTION_SEND);
			intent.setType("text/plain");
			intent.putExtra(Intent.EXTRA_TEXT, item.status.url);
			v.getContext().startActivity(Intent.createChooser(intent, v.getContext().getString(R.string.share_toot_title)));
		}

		private boolean onShareLongClick(View v){
			UiUtils.copyText(v, item.status.url);
			return true;
		}

		private int descriptionForId(int id){
			if(id==R.id.reply_btn)
				return R.string.button_reply;
			if(id==R.id.boost_btn)
				return R.string.button_reblog;
			if(id==R.id.favorite_btn)
				return R.string.button_favorite;
			if(id==R.id.bookmark_btn)
				return R.string.add_bookmark;
			if(id==R.id.share_btn)
				return R.string.button_share;
			return 0;
		}
	}
}
