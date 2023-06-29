package org.joinmastodon.android.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

import me.grishka.appkit.utils.V;

public class EmojiReactionsView extends GridView {

    public EmojiReactionsView(Context context) {
        super(context);
    }

    public EmojiReactionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmojiReactionsView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int rows = (int) Math.ceil((double) getAdapter().getCount() / 7);
        // height * amount of rows + amount of rows above 1 * spacing + 2 * padding + 4 extra
        int height = rows > 0 ? 48 * rows + (rows - 1) * 8 + 2 * 4 + 4 : 0;
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(V.dp(height), MeasureSpec.EXACTLY));
    }
}
