package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.api.requests.statuses.TranslateStatus;
import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Instance;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.drawables.SpoilerStripesDrawable;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.model.TranslatedStatus;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.views.LinkedTextView;

import me.grishka.appkit.api.Callback;
import me.grishka.appkit.api.ErrorResponse;
import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.MovieDrawable;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;
import me.grishka.appkit.utils.CubicBezierInterpolator;
import me.grishka.appkit.utils.V;

public class TextStatusDisplayItem extends StatusDisplayItem{
	private CharSequence text;
	private CustomEmojiHelper emojiHelper=new CustomEmojiHelper(), spoilerEmojiHelper;
	private CharSequence parsedSpoilerText;
	public boolean textSelectable;
	public final Status status;
	public boolean translated = false;
	public TranslatedStatus translation = null;
	private AccountSession session;

	public TextStatusDisplayItem(String parentID, CharSequence text, BaseStatusListFragment parentFragment, Status status){
		super(parentID, parentFragment);
		this.text=text;
		this.status=status;
		emojiHelper.setText(text);
		if(!TextUtils.isEmpty(status.spoilerText)){
			parsedSpoilerText=HtmlParser.parseCustomEmoji(status.spoilerText, status.emojis);
			spoilerEmojiHelper=new CustomEmojiHelper();
			spoilerEmojiHelper.setText(parsedSpoilerText);
		}
		session = AccountSessionManager.getInstance().getAccount(parentFragment.getAccountID());
	}

	@Override
	public Type getType(){
		return Type.TEXT;
	}

	@Override
	public int getImageCount(){
		if(spoilerEmojiHelper!=null && !status.spoilerRevealed)
			return spoilerEmojiHelper.getImageCount();
		return emojiHelper.getImageCount();
	}

	@Override
	public ImageLoaderRequest getImageRequest(int index){
		if(spoilerEmojiHelper!=null && !status.spoilerRevealed)
			return spoilerEmojiHelper.getImageRequest(index);
		return emojiHelper.getImageRequest(index);
	}

	public static class Holder extends StatusDisplayItem.Holder<TextStatusDisplayItem> implements ImageLoaderViewHolder{
		private final LinkedTextView text;
		private final LinearLayout spoilerHeader;
		private final TextView spoilerTitle, spoilerTitleInline, translateInfo;
		private final View spoilerOverlay, borderTop, borderBottom, textWrap, translateWrap, translateProgress;
		private final Drawable backgroundColor, borderColor;
		private final Button translateButton;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_text, parent);
			text=findViewById(R.id.text);
			spoilerTitle=findViewById(R.id.spoiler_title);
			spoilerTitleInline=findViewById(R.id.spoiler_title_inline);
			spoilerHeader=findViewById(R.id.spoiler_header);
			spoilerOverlay=findViewById(R.id.spoiler_overlay);
			borderTop=findViewById(R.id.border_top);
			borderBottom=findViewById(R.id.border_bottom);
			textWrap=findViewById(R.id.text_wrap);
			translateWrap=findViewById(R.id.translate_wrap);
			translateButton=findViewById(R.id.translate_btn);
			translateInfo=findViewById(R.id.translate_info);
			translateProgress=findViewById(R.id.translate_progress);
			itemView.setOnClickListener(v->item.parentFragment.onRevealSpoilerClick(this));

			TypedValue outValue=new TypedValue();
			activity.getTheme().resolveAttribute(R.attr.colorBackgroundLight, outValue, true);
			backgroundColor=activity.getDrawable(outValue.resourceId);
//			activity.getTheme().resolveAttribute(R.attr.colorBackgroundLightest, outValue, true);
//			backgroundColorInset=activity.getDrawable(outValue.resourceId);
			activity.getTheme().resolveAttribute(R.attr.colorPollVoted, outValue, true);
			borderColor=activity.getDrawable(outValue.resourceId);
		}

		@Override
		public void onBind(TextStatusDisplayItem item){
			text.setText(item.translated
							? HtmlParser.parse(item.translation.content, item.status.emojis, item.status.mentions, item.status.tags, item.parentFragment.getAccountID())
							: item.text);
			text.setTextIsSelectable(item.textSelectable);
			spoilerTitleInline.setTextIsSelectable(item.textSelectable);
			text.setInvalidateOnEveryFrame(false);
			spoilerTitleInline.setBackground(item.inset ? null : backgroundColor);
			spoilerTitleInline.setPadding(spoilerTitleInline.getPaddingLeft(), item.inset ? 0 : V.dp(14), spoilerTitleInline.getPaddingRight(), item.inset ? 0 : V.dp(14));
			borderTop.setBackground(item.inset ? null : borderColor);
			borderBottom.setBackground(item.inset ? null : borderColor);
			if(!TextUtils.isEmpty(item.status.spoilerText)){
				spoilerTitle.setText(item.parsedSpoilerText);
				spoilerTitleInline.setText(item.parsedSpoilerText);
				if(item.status.spoilerRevealed){
					spoilerOverlay.setVisibility(View.GONE);
					spoilerHeader.setVisibility(View.VISIBLE);
					textWrap.setVisibility(View.VISIBLE);
					itemView.setClickable(false);
				}else{
					spoilerOverlay.setVisibility(View.VISIBLE);
					spoilerHeader.setVisibility(View.GONE);
					textWrap.setVisibility(View.GONE);
					itemView.setClickable(true);
				}
			}else{
				spoilerOverlay.setVisibility(View.GONE);
				spoilerHeader.setVisibility(View.GONE);
				textWrap.setVisibility(View.VISIBLE);
				itemView.setClickable(false);
			}

			Instance instanceInfo = AccountSessionManager.getInstance().getInstanceInfo(item.session.domain);
			boolean translateEnabled = instanceInfo.v2 != null && instanceInfo.v2.configuration.translation != null && instanceInfo.v2.configuration.translation.enabled;

			translateWrap.setVisibility(
					(!GlobalUserPreferences.translateButtonOpenedOnly || item.textSelectable) &&
					translateEnabled &&
					!item.status.visibility.isLessVisibleThan(StatusPrivacy.UNLISTED) &&
					item.status.language != null &&
					(item.session.preferences == null || !item.status.language.equalsIgnoreCase(item.session.preferences.postingDefaultLanguage))
					? View.VISIBLE : View.GONE);
			translateButton.setText(item.translated ? R.string.sk_translate_show_original : R.string.sk_translate_post);
			translateInfo.setText(item.translated ? itemView.getResources().getString(R.string.sk_translated_using, item.translation.provider) : "");
			translateButton.setOnClickListener(v->{
				if (item.translation == null) {
					translateProgress.setVisibility(View.VISIBLE);
					translateButton.setClickable(false);
					translateButton.animate().alpha(0.5f).setInterpolator(CubicBezierInterpolator.DEFAULT).setDuration(150).start();
					new TranslateStatus(item.status.id).setCallback(new Callback<>() {
						@Override
						public void onSuccess(TranslatedStatus translatedStatus) {
							item.translation = translatedStatus;
							item.translated = true;
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
					item.translated = !item.translated;
					rebind();
				}
			});
		}

		@Override
		public void setImage(int index, Drawable image){
			getEmojiHelper().setImageDrawable(index, image);
			text.invalidate();
			spoilerTitle.invalidate();
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
			return item.spoilerEmojiHelper!=null && !item.status.spoilerRevealed ? item.spoilerEmojiHelper : item.emojiHelper;
		}
	}
}
