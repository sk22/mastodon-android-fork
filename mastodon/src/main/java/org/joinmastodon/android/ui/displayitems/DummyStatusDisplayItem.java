package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Space;

import org.joinmastodon.android.fragments.BaseStatusListFragment;

import me.grishka.appkit.utils.V;

public class DummyStatusDisplayItem extends StatusDisplayItem {
	private final boolean addMediaGridMargin;

	public DummyStatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment, boolean addMediaGridMargin) {
		super(parentID, parentFragment);
		this.addMediaGridMargin = addMediaGridMargin;
	}

	@Override
	public Type getType() {
		return Type.DUMMY;
	}

	public static class Holder extends StatusDisplayItem.Holder<DummyStatusDisplayItem> {
		public Holder(Context context) {
			super(new Space(context));
		}

		@Override
		public void onBind(DummyStatusDisplayItem item) {
			// BetterItemAnimator appears not to handle InsetStatusItemDecoration's getItemOffsets
			// correctly, causing removed inset views to jump while animating. i don't quite
			// understand it, but this workaround appears to work.
			// see InsetStatusItemDecoration#getItemOffsets
			ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
			params.setMargins(0, item.addMediaGridMargin ? V.dp(0) : 0, 0, V.dp(16));
			itemView.setLayoutParams(params);
		}
	}
}
