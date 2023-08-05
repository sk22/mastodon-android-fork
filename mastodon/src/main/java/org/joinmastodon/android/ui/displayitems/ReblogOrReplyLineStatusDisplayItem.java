package org.joinmastodon.android.ui.displayitems;

import static org.joinmastodon.android.MastodonApp.context;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.List;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.V;

public class ReblogOrReplyLineStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	@DrawableRes
	private int icon;
	private StatusPrivacy visibility;
	@DrawableRes
	private int iconEnd;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper(), fullTextEmojiHelper;
	private View.OnClickListener handleClick;
	public boolean needBottomPadding;
	ReblogOrReplyLineStatusDisplayItem extra;
	CharSequence fullText;

	public ReblogOrReplyLineStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, CharSequence text, List<Emoji> emojis, @DrawableRes int icon, StatusPrivacy visibility, @Nullable View.OnClickListener handleClick) {
		this(parentID, parentFragment, text, emojis, icon, visibility, handleClick, text);
	}

	public ReblogOrReplyLineStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, CharSequence text, List<Emoji> emojis, @DrawableRes int icon, StatusPrivacy visibility, @Nullable View.OnClickListener handleClick, CharSequence fullText) {
		super(parentID, parentFragment);
		SpannableStringBuilder ssb=new SpannableStringBuilder(text);
		HtmlParser.parseCustomEmoji(ssb, emojis);
		this.text=ssb;
		emojiHelper.setText(ssb);
		this.icon=icon;
		this.handleClick=handleClick;
		TypedValue outValue = new TypedValue();
		context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
		updateVisibility(visibility);

		if (fullText != null) {
			fullTextEmojiHelper = new CustomEmojiHelper();
			SpannableStringBuilder fullTextSsb = new SpannableStringBuilder(fullText);
			HtmlParser.parseCustomEmoji(fullTextSsb, emojis);
			this.fullText=fullTextSsb;
			fullTextEmojiHelper.setText(fullTextSsb);
		}

	}

	public void updateVisibility(StatusPrivacy visibility) {
		this.visibility = visibility;
		this.iconEnd = visibility != null ? switch (visibility) {
			case PUBLIC -> R.drawable.ic_fluent_earth_20sp_regular;
			case UNLISTED -> R.drawable.ic_fluent_lock_open_20sp_regular;
			case PRIVATE -> R.drawable.ic_fluent_lock_closed_20sp_filled;
			default -> 0;
		} : 0;
	}

	@Override
	public Type getType(){
		return Type.REBLOG_OR_REPLY_LINE;
	}

	@Override
	public int getImageCount(){
		return emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return emojiHelper.getImageRequest(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<ReblogOrReplyLineStatusDisplayItem> implements ImageLoaderViewHolder{
		private final TextView text, extraText;
		private final View separator;
		private final ViewGroup parent;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_reblog_or_reply_line, parent);
			this.parent = parent;
			text=findViewById(R.id.text);
			extraText=findViewById(R.id.extra_text);
			separator=findViewById(R.id.separator);
			if (GlobalUserPreferences.compactReblogReplyLine) {
				parent.addOnLayoutChangeListener((v, l, t, right, b, ol, ot, oldRight, ob) -> {
					if (right != oldRight) layoutLine();
				});
			}
		}

		private void bindLine(ReblogOrReplyLineStatusDisplayItem item, TextView text) {
			if (item.fullText != null) text.setContentDescription(item.fullText);
			text.setText(item.text);
			text.setCompoundDrawablesRelativeWithIntrinsicBounds(item.icon, 0, item.iconEnd, 0);
			text.setOnClickListener(item.handleClick);
			text.setEnabled(!item.inset && item.handleClick != null);
			text.setClickable(!item.inset && item.handleClick != null);
			Context ctx = itemView.getContext();
			int visibilityText = item.visibility != null ? switch (item.visibility) {
				case PUBLIC -> R.string.visibility_public;
				case UNLISTED -> R.string.sk_visibility_unlisted;
				case PRIVATE -> R.string.visibility_followers_only;
				case LOCAL -> R.string.sk_local_only;
				default -> 0;
			} : 0;
			if (visibilityText != 0) text.setContentDescription(item.text + " (" + ctx.getString(visibilityText) + ")");
			if(Build.VERSION.SDK_INT<Build.VERSION_CODES.N)
				UiUtils.fixCompoundDrawableTintOnAndroid6(text);
			text.setCompoundDrawableTintList(text.getTextColors());
		}

		@Override
		public void onBind(ReblogOrReplyLineStatusDisplayItem item){
			bindLine(item, text);
			if (item.extra != null) bindLine(item.extra, extraText);
			extraText.setVisibility(item.extra == null ? View.GONE : View.VISIBLE);
			separator.setVisibility(item.extra == null ? View.GONE : View.VISIBLE);
			itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), item.needBottomPadding ? V.dp(16) : 0);
			layoutLine();
		}

		private void layoutLine() {
			// layout line only if above header, compact and has extra
			if (!GlobalUserPreferences.compactReblogReplyLine || item.extra == null) return;
			itemView.measure(
					View.MeasureSpec.makeMeasureSpec(parent.getWidth(), View.MeasureSpec.EXACTLY),
					View.MeasureSpec.UNSPECIFIED);
			boolean isVertical = ((LinearLayout) itemView).getOrientation() == LinearLayout.VERTICAL;
			extraText.setPaddingRelative(extraText.getPaddingStart(), item.extra != null && isVertical ? 0 : V.dp(16), extraText.getPaddingEnd(), extraText.getPaddingBottom());
			separator.setVisibility(item.extra != null && !isVertical ? View.VISIBLE : View.GONE);
			((LinearLayout) itemView).removeView(extraText);
			if (isVertical) ((LinearLayout) itemView).addView(extraText);
			else ((LinearLayout) itemView).addView(extraText, 0);
			text.setText(isVertical ? item.fullText : item.text);
			if (item.extra != null) {
				extraText.setText(isVertical ? item.extra.fullText : item.extra.text);
			}
		}

		@Override
		public void setImage(int index, Drawable image){
			item.emojiHelper.setImageDrawable(index, image);
			text.invalidate();
		}

		@Override
		public void clearImage(int index){
			setImage(index, null);
		}
	}
}
