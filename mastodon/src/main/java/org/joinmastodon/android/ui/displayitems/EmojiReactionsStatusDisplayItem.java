package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Emoji;
import org.joinmastodon.android.model.EmojiReaction;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.joinmastodon.android.ui.utils.CustomEmojiHelper;
import org.joinmastodon.android.ui.utils.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.grishka.appkit.imageloader.ImageLoaderViewHolder;
import me.grishka.appkit.imageloader.requests.ImageLoaderRequest;

public class EmojiReactionsStatusDisplayItem extends StatusDisplayItem {
    public final List<EmojiReactionDisplay> reactions = new ArrayList<>();

    private final CustomEmojiHelper emojiHelper=new CustomEmojiHelper();

    public EmojiReactionsStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, List<EmojiReaction> emojiReactions) {
        super(parentID, parentFragment);
        for (EmojiReaction emojiReaction : emojiReactions) {
            int atSymbolIndex = emojiReaction.name.indexOf("@");
            String name = emojiReaction.name.substring(0, atSymbolIndex != -1 ? atSymbolIndex : emojiReaction.name.length());
            SpannableStringBuilder ssb = new SpannableStringBuilder(emojiReaction.url != null ? ":" + name + ":" : name);
            ssb.append("\n").append(String.valueOf(emojiReaction.count));
            int countStartIndex = ssb.length() - String.valueOf(emojiReaction.count).length();
            ssb.setSpan(new RelativeSizeSpan(1.6f), 0, countStartIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ssb.setSpan(new RelativeSizeSpan(0.8f), countStartIndex, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (emojiReaction.url != null) {
                Emoji emoji = new Emoji();
                emoji.shortcode = name;
                emoji.url = emojiReaction.url;
                HtmlParser.parseCustomEmoji(ssb, Collections.singletonList(emoji));
                emojiHelper.addText(ssb);
            }
            reactions.add(new EmojiReactionDisplay(ssb, emojiReaction.me));
        }
    }

    @Override
    public Type getType(){
        return Type.EMOJI_REACTIONS;
    }

    @Override
    public int getImageCount(){
        return emojiHelper.getImageCount();
    }

    @Override
    public ImageLoaderRequest getImageRequest(int index){
        return emojiHelper.getImageRequest(index);
    }

    public static class Holder extends StatusDisplayItem.Holder<EmojiReactionsStatusDisplayItem> implements ImageLoaderViewHolder {
        private final LinearLayout layout;
        private final List<Button> buttons = new ArrayList<>();

        public Holder(Activity activity, ViewGroup parent) {
            super(activity, R.layout.display_item_emoji_reactions, parent);
            this.layout = findViewById(R.id.reaction_layout);
        }

        @Override
        public void onBind(EmojiReactionsStatusDisplayItem item) {
            layout.removeAllViews();

            float density = layout.getContext().getResources().getDisplayMetrics().density;
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, (int) (4 * density), 0, (int) (4 * density));
            params.setMarginStart((int) (4 * density));
            params.setMarginEnd((int) (4 * density));
            for (EmojiReactionDisplay reaction : item.reactions) {
                Button btn = new Button(layout.getContext());
                btn.setPaddingRelative((int) (12 * density), 0, (int) (12 * density), 0);
                btn.setBackgroundResource(R.drawable.bg_button_primary_light_on_dark);
                btn.setTextColor(item.parentFragment.getContext().getColor(R.color.gray_800));
                btn.setText(reaction.text);
                if (reaction.me)
                    btn.setPressed(true);

                layout.addView(btn, params);
                buttons.add(btn);
            }
        }

        @Override
        public void setImage(int index, Drawable drawable){
            item.emojiHelper.setImageDrawable(index, drawable);
            buttons.get(index).invalidate();
            if(drawable instanceof Animatable)
                ((Animatable) drawable).start();
        }

        @Override
        public void clearImage(int index){
            setImage(index, null);
        }
    }

    private static class EmojiReactionDisplay {
        public SpannableStringBuilder text;
        public boolean me;

        public EmojiReactionDisplay(SpannableStringBuilder text, boolean me) {
            this.text = text;
            this.me = me;
        }
    }
}
