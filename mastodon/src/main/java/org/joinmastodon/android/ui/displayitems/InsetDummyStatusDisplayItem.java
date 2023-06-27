package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Space;
import android.widget.TextView;

import org.joinmastodon.android.fragments.BaseStatusListFragment;

import me.grishka.appkit.utils.V;

public class InsetDummyStatusDisplayItem extends StatusDisplayItem {
	private final boolean addMediaGridMargin;

	public InsetDummyStatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment, boolean addMediaGridMargin) {
		super(parentID, parentFragment);
		this.addMediaGridMargin = addMediaGridMargin;
	}

	@Override
	public Type getType() {
		return Type.DUMMY;
	}

	public static class Holder extends StatusDisplayItem.Holder<InsetDummyStatusDisplayItem> {
		public Holder(Context context) {
			super(new Space(context));
		}

		@Override
		public void onBind(InsetDummyStatusDisplayItem item) {
			// BetterItemAnimator appears not to handle InsetStatusItemDecoration's getItemOffsets
			// correctly, causing removed inset views to jump while animating. i don't quite
			// understand it, but this workaround appears to work.
			// see InsetStatusItemDecoration#getItemOffsets
			ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
			params.setMargins(0, item.addMediaGridMargin ? V.dp(4) : 0, 0, V.dp(16));
			itemView.setLayoutParams(params);
		}
	}
}
