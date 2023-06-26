package org.joinmastodon.android.ui.displayitems;

import static org.joinmastodon.android.MastodonApp.context;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.ProfileFragment;
import org.joinmastodon.android.model.Notification;
import org.joinmastodon.android.ui.OutlineProviders;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import me.grishka.appkit.Nav;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.imageloader.requests.UrlImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class NotificationHeaderStatusDisplayItem extends StatusDisplayItem{
	public final Notification notification;
	private ImageLoaderRequest avaRequest;
	private String accountID;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	private CharSequence text;

	public NotificationHeaderStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Notification notification, String accountID){
		super(parentID, parentFragment);
		this.notification=notification;
		this.accountID=accountID;

		if(notification.type==Notification.Type.POLL){
			text=parentFragment.getString(R.string.poll_ended);
		}else{
			avaRequest=new UrlImageLoaderRequest(GlobalUserPreferences.playGifs ? notification.account.avatar : notification.account.avatarStatic, V.dp(50), V.dp(50));
			SpannableStringBuilder parsedName=new SpannableStringBuilder(notification.account.displayName);
			HtmlParser.parseCustomEmoji(parsedName, notification.account.emojis);
			emojiHelper.setText(parsedName);
			String[] parts=parentFragment.getString(switch(notification.type){
				case FOLLOW -> R.string.user_followed_you;
				case FOLLOW_REQUEST -> R.string.user_sent_follow_request;
				case REBLOG -> R.string.notification_boosted;
				case FAVORITE -> R.string.user_favorited;
				case POLL -> R.string.poll_ended;
				case UPDATE -> R.string.sk_post_edited;
				case SIGN_UP -> R.string.sk_signed_up;
				case REPORT -> R.string.sk_reported;
				case REACTION, PLEROMA_EMOJI_REACTION ->
						notification.emoji != null ? R.string.sk_reacted_with : R.string.sk_reacted;
				default -> throw new IllegalStateException("Unexpected value: "+notification.type);
			}).split("%s", 4);
			SpannableStringBuilder text=new SpannableStringBuilder();
			if(parts.length>1 && !TextUtils.isEmpty(parts[0]))
				text.append(parts[0]);
			text.append(parsedName, new TypefaceSpan("sans-serif-medium"), 0);

			if(parts.length==1){
				text.append(parts[0]);
			}else if(!TextUtils.isEmpty(parts[1]) && parts.length < 3){
				text.append(parts[1]);
			} else if (parts.length == 3 && notification.emoji != null) {
				text.append(parts[1]).append(notification.emoji);
			}
			this.text=text;
		}
	}

	@Override
	public Type getType(){
		return Type.NOTIFICATION_HEADER;
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

	public static class Holder extends StatusDisplayItem.Holder<NotificationHeaderStatusDisplayItem> implements ImageLoaderViewHolder{
		private final ImageView icon, avatar;
		private final TextView text;
		private final int selectableItemBackground;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_notification_header, parent);
			icon=findViewById(R.id.icon);
			avatar=findViewById(R.id.avatar);
			text=findViewById(R.id.text);

			avatar.setOutlineProvider(OutlineProviders.roundedRect(8));
			avatar.setClipToOutline(true);

			itemView.setOnClickListener(this::onItemClick);
			TypedValue outValue = new TypedValue();
			context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
			selectableItemBackground = outValue.resourceId;
		}

		@Override
		public void setImage(int index, Drawable image){
			if(index==0){
				avatar.setImageDrawable(image);
			}else{
				item.emojiHelper.setImageDrawable(index-1, image);
				text.invalidate();
			}
		}

		@Override
		public void clearImage(int index){
			if(index==0)
				avatar.setImageResource(R.drawable.image_placeholder);
			else
				ImageLoaderViewHolder.super.clearImage(index);
		}

		@SuppressLint("ResourceType")
		@Override
		public void onBind(NotificationHeaderStatusDisplayItem item){
			text.setText(item.text);
			avatar.setVisibility(item.notification.type==Notification.Type.POLL ? View.GONE : View.VISIBLE);
			icon.setImageResource(switch(item.notification.type){
				case FAVORITE -> R.drawable.ic_fluent_star_24_filled;
				case REBLOG -> R.drawable.ic_fluent_arrow_repeat_all_24_filled;
				case FOLLOW, FOLLOW_REQUEST -> R.drawable.ic_fluent_person_add_24_filled;
				case POLL -> R.drawable.ic_fluent_poll_24_filled;
				case REPORT -> R.drawable.ic_fluent_warning_24_filled;
				case SIGN_UP -> R.drawable.ic_fluent_person_available_24_filled;
				case UPDATE -> R.drawable.ic_fluent_edit_24_filled;
				case REACTION, PLEROMA_EMOJI_REACTION -> R.drawable.ic_fluent_add_24_filled;
				default -> throw new IllegalStateException("Unexpected value: "+item.notification.type);
			});
			icon.setImageTintList(ColorStateList.valueOf(UiUtils.getThemeColor(item.parentFragment.getActivity(), switch(item.notification.type){
				case FAVORITE -> R.attr.colorFavorite;
				case REBLOG -> R.attr.colorBoost;
				case FOLLOW, FOLLOW_REQUEST -> R.attr.colorFollow;
				case POLL -> R.attr.colorPoll;
				default -> android.R.attr.colorAccent;
			})));
			itemView.setBackgroundResource(item.notification.type != Notification.Type.POLL ?
					selectableItemBackground : 0);
			itemView.setClickable(item.notification.type != Notification.Type.POLL);
		}

		public void onItemClick(View v) {
			Bundle args=new Bundle();
			args.putString("account", item.accountID);
			args.putParcelable("profileAccount", Parcels.wrap(item.notification.account));
			Nav.go(item.parentFragment.getActivity(), ProfileFragment.class, args);
		}
	}
}
