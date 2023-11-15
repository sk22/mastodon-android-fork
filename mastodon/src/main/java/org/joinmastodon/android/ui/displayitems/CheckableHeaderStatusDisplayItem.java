package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.CheckBox;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.report.ReportAddPostsChoiceFragment;
import org.joinmastodon.android.model.Account;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.views.CheckableRelativeLayout;

import java.time.Instant;
import java.util.function.Predicate;

public class CheckableHeaderStatusDisplayItem extends HeaderStatusDisplayItem{
	public CheckableHeaderStatusDisplayItem(String parentID, Account user, Instant createdAt, BaseStatusListFragment<?> parentFragment, String accountID, Status status, CharSequence extraText){
		super(parentID, user, createdAt, parentFragment, accountID, status, extraText, null, null);
	}

	@Override
	public Type getType(){
		return Type.HEADER_CHECKABLE;
	}

	public static class Holder extends HeaderStatusDisplayItem.Holder{
		private final View checkbox;
		private final CheckableRelativeLayout view;
		private Predicate<Holder> isChecked;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_header_checkable, parent);
			checkbox=findViewById(R.id.checkbox);
			view=findViewById(R.id.checkbox_wrap);
			checkbox.setBackground(new CheckBox(activity).getButtonDrawable());
			view.setOnClickListener(this::onToggle);
			view.setAccessibilityDelegate(new View.AccessibilityDelegate(){
				@Override
				public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info){
					super.onInitializeAccessibilityNodeInfo(host, info);
					info.setClassName(CheckBox.class.getName());
				}
			});
		}

		@Override
		public void onBind(HeaderStatusDisplayItem item){
			super.onBind(item);
			if(isChecked!=null){
				view.setChecked(isChecked.test(this));
			}
		}

		private void onToggle(View v){
			if(item.parentFragment instanceof ReportAddPostsChoiceFragment reportFragment){
				reportFragment.onToggleItem(item.parentID);
			}
		}

		public void setIsChecked(Predicate<Holder> isChecked){
			this.isChecked=isChecked;
		}
	}
}
