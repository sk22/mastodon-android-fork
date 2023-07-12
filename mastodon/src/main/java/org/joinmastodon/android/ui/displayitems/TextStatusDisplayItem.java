package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.bottomSoftwareFoundation.bottom.Bottom;
import com.github.bottomSoftwareFoundation.bottom.TranslationError;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.TranslateStatus;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.model.TranslatedStatus;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.joinmastodon.android.ui.views.LinkedTextView;
import org.joinmastodon.android.utils.StatusTextEncoder;

import java.util.Locale;
import java.util.regex.Pattern;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.MovieDrawable;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class TextStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper();
	public boolean textSelectable;
	public boolean reduceTopPadding;
	public final Status status;
	public boolean disableTranslate, translationShown;
	private AccountSession session;
	public static final Pattern BOTTOM_TEXT_PATTERN = Pattern.compile("(?:[\uD83E\uDEC2\uD83D\uDC96✨\uD83E\uDD7A,]+|❤️)(?:\uD83D\uDC49\uD83D\uDC48(?:[\uD83E\uDEC2\uD83D\uDC96✨\uD83E\uDD7A,]+|❤️))*\uD83D\uDC49\uD83D\uDC48");

	public TextStatusDisplayItem(String parentID, CharSequence text, BaseStatusListFragment parentFragment, Status status, boolean disableTranslate){
		super(parentID, parentFragment);
		this.text=text;
		this.status=status;
		this.disableTranslate=disableTranslate;
		this.translationShown=status.translationShown;
		emojiHelper.setText(text);
		session = AccountSessionManager.getInstance().getAccount(parentFragment.getAccountID());
	}

	public void setTranslationShown(boolean translationShown) {
		this.translationShown = translationShown;
		status.translationShown = translationShown;
	}

	@Override
	public Type getType(){
		return Type.TEXT;
	}

	@Override
	public int getImageCount(){
		return emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		return emojiHelper.getImageRequest(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<TextStatusDisplayItem> implements ImageLoaderViewHolder{
		private final LinkedTextView text;
		private final TextView translateInfo, readMore;
		private final View textWrap, translateWrap, translateProgress;
		private final Button translateButton;
		private final ScrollView textScrollView;

		private final float textMaxHeight, textCollapsedHeight;
		private final LinearLayout.LayoutParams collapseParams, wrapParams;
		private final ViewGroup parent;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_text, parent);
			this.parent=parent;
			text=findViewById(R.id.text);
			textWrap = (LinearLayout) itemView;
			translateWrap=findViewById(R.id.translate_wrap);
			translateButton=findViewById(R.id.translate_btn);
			translateInfo=findViewById(R.id.translate_info);
			translateProgress=findViewById(R.id.translate_progress);
			textScrollView=findViewById(R.id.text_scroll_view);
			readMore=findViewById(R.id.read_more);
			textMaxHeight=activity.getResources().getDimension(R.dimen.text_max_height);
			textCollapsedHeight=activity.getResources().getDimension(R.dimen.text_collapsed_height);
			collapseParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) textCollapsedHeight);
			wrapParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			readMore.setOnClickListener(v -> item.parentFragment.onToggleExpanded(item.status, getItemID()));
		}

		@Override
		public void onBind(TextStatusDisplayItem item){
			boolean hasSpoiler = !TextUtils.isEmpty(item.status.spoilerText);
			text.setText(item.translationShown
							? HtmlParser.parse(item.status.translation.content, item.status.emojis, item.status.mentions, item.status.tags, item.parentFragment.getAccountID())
							: item.text);
			text.setTextIsSelectable(item.textSelectable);
			if (item.textSelectable) {
				textScrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
			}
			text.setInvalidateOnEveryFrame(false);
			itemView.setClickable(false);
			itemView.setPadding(itemView.getPaddingLeft(), item.reduceTopPadding ? V.dp(6) : V.dp(12), itemView.getPaddingRight(), itemView.getPaddingBottom());
			text.setTextColor(UiUtils.getThemeColor(text.getContext(), item.inset ? R.attr.colorM3OnSurfaceVariant : R.attr.colorM3OnSurface));

			Instance instanceInfo = AccountSessionManager.getInstance().getInstanceInfo(item.session.domain);
			boolean translateEnabled = !item.disableTranslate && instanceInfo != null &&
					instanceInfo.v2 != null && instanceInfo.v2.configuration.translation != null &&
					instanceInfo.v2.configuration.translation.enabled;
			String bottomText = null;
			try {
				bottomText = BOTTOM_TEXT_PATTERN.matcher(item.status.getStrippedText()).find()
						? new StatusTextEncoder(Bottom::decode).decode(item.status.getStrippedText(), BOTTOM_TEXT_PATTERN)
						: null;
			} catch (TranslationError ignored) {}

			boolean translateVisible = (bottomText != null || (
					translateEnabled &&
							!item.status.visibility.isLessVisibleThan(StatusPrivacy.UNLISTED) &&
							item.status.language != null &&
							// todo: compare to mastodon locale instead (how do i query that?!)
							!item.status.language.equalsIgnoreCase(Locale.getDefault().getLanguage())))
					&& (!GlobalUserPreferences.translateButtonOpenedOnly || item.textSelectable);
			translateWrap.setVisibility(translateVisible ? View.VISIBLE : View.GONE);
			translateButton.setText(item.translationShown ? R.string.sk_translate_show_original : R.string.sk_translate_post);
			translateInfo.setText(item.translationShown ? itemView.getResources().getString(R.string.sk_translated_using, bottomText != null ? "bottom-java" : item.status.translation.provider) : "");
			String finalBottomText = bottomText;
			translateButton.setOnClickListener(v->{
				if (item.status.translation == null) {
					if (finalBottomText != null) {
						try {
							item.status.translation = new TranslatedStatus();
							item.status.translation.content = finalBottomText;
							item.setTranslationShown(true);
						} catch (TranslationError err) {
							item.status.translation = null;
							Toast.makeText(itemView.getContext(), err.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
						}
						rebind();
						return;
					}
					translateProgress.setVisibility(View.VISIBLE);
					translateButton.setClickable(false);
					translateButton.animate().alpha(0.5f).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(150).start();
					new TranslateStatus(item.status.id).setCallback(new Callback<>() {
						@Override
						public void onSuccess(TranslatedStatus translatedStatus) {
							item.status.translation = translatedStatus;
							item.setTranslationShown(true);
							if (item.parentFragment.getActivity() == null) return;
							translateProgress.setVisibility(View.GONE);
							translateButton.setClickable(true);
							translateButton.animate().alpha(1).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(50).start();
							rebind();
						}

						@Override
						public void onError(ErrorResponse error) {
							translateProgress.setVisibility(View.GONE);
							translateButton.setClickable(true);
							translateButton.animate().alpha(1).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(50).start();
							error.showToast(itemView.getContext());
						}
					}).exec(item.parentFragment.getAccountID());
				} else {
					item.setTranslationShown(!item.translationShown);
					rebind();
				}
			});

			readMore.setText(item.status.textExpanded ? R.string.sk_collapse : R.string.sk_expand);

			// remove additional padding when (transparently padded) translate button is visible
			int nextPos = getAbsoluteAdapterPosition() + 1;
			boolean nextIsFooter = item.parentFragment.getDisplayItems().size() > nextPos &&
					item.parentFragment.getDisplayItems().get(nextPos) instanceof FooterStatusDisplayItem;
			int bottomPadding = (translateVisible && nextIsFooter) ? 0
					: nextIsFooter ? V.dp(6)
					: V.dp(12);
			itemView.setPadding(itemView.getPaddingLeft(), itemView.getPaddingTop(), itemView.getPaddingRight(), bottomPadding);

			if (!GlobalUserPreferences.collapseLongPosts) {
				textScrollView.setLayoutParams(wrapParams);
				readMore.setVisibility(View.GONE);
			}

			// incredibly ugly workaround for https://github.com/sk22/megalodon/issues/520
			// i am so, so sorry. FIXME
			// attempts to use OnPreDrawListener, OnGlobalLayoutListener and .post have failed -
			// the view didn't want to reliably update after calling .setVisibility etc :(
			int width = parent.getWidth() != 0 ? parent.getWidth()
					: item.parentFragment.getView().getWidth() != 0
					? item.parentFragment.getView().getWidth()
					: item.parentFragment.getParentFragment() != null && item.parentFragment.getParentFragment().getView().getWidth() != 0
					? item.parentFragment.getParentFragment().getView().getWidth() // YIKES
					: UiUtils.MAX_WIDTH;

			text.measure(
					View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
					View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

			if (GlobalUserPreferences.collapseLongPosts && !item.status.textExpandable) {
				boolean tooBig = text.getMeasuredHeight() > textMaxHeight;
				boolean expandable = tooBig && !hasSpoiler;
				item.parentFragment.onEnableExpandable(Holder.this, expandable);
			}

			boolean expandButtonShown=item.status.textExpandable && !item.status.textExpanded;
			translateWrap.setPadding(0, V.dp(expandButtonShown ? 0 : 4), 0, 0);
			readMore.setVisibility(expandButtonShown ? View.VISIBLE : View.GONE);
			textScrollView.setLayoutParams(item.status.textExpandable && !item.status.textExpanded ? collapseParams : wrapParams);

			// compensate for spoiler's bottom margin
			ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();
			params.setMargins(params.leftMargin, (item.inset || GlobalUserPreferences.spectatorMode) && hasSpoiler ? V.dp(-16) : 0,
					params.rightMargin, params.bottomMargin);
		}

		@Override
		public void setImage(int index, Drawable image){
			getEmojiHelper().setImageDrawable(index, image);
			text.invalidate();
			if(image instanceof Animatable){
				((Animatable) image).start();
				if(image instanceof MovieDrawable)
					text.setInvalidateOnEveryFrame(true);
			}
		}

		@Override
		public void clearImage(int index){
			getEmojiHelper().setImageDrawable(index, null);
			text.invalidate();
		}

		private CustomEmojiHelper getEmojiHelper(){
			return item.emojiHelper;
		}
	}
}
