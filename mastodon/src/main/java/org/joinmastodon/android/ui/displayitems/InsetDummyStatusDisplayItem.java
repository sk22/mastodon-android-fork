package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.Space;

import org.joinmastodon.android.fragments.BaseStatusListFragment;

import me.grishka.appkit.utils.V;

public class InsetDummyStatusDisplayItem extends StatusDisplayItem {
	private final boolean addTopMargin;

	/**
	 * Helps working around issues regarding animations when revealing/closing spoilers.
	 * Two usages:
	 * 1. As the first item of an inset section, to provide the top margin I commented out in
	 *    InsetStatusItemDecoration (which caused inset items to not animate properly).
	 * 2. As the last item of an inset section, preventing the animated content items to clip out
	 *    of the decoration bounds.
	 */
	public InsetDummyStatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment, boolean addTopMargin) {
		super(parentID, parentFragment);
		this.addTopMargin = addTopMargin;
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
			params.setMargins(0, 0, 0, item.addTopMargin ? V.dp(12) : 0);
			itemView.setLayoutParams(params);
		}
	}
}
