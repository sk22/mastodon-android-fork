package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.drawables.SawtoothTearDrawable;

import java.util.ArrayList;

public class WarningFilteredStatusDisplayItem extends StatusDisplayItem{
	public boolean loading;
	public final Status status;
	public ArrayList<StatusDisplayItem> filteredItems;

	public WarningFilteredStatusDisplayItem(String parentID, BaseStatusListFragment<?> parentFragment, Status status, ArrayList<StatusDisplayItem> items){
		super(parentID, parentFragment);
		this.status=status;
		this.filteredItems = items;
	}

	@Override
	public Type getType(){
		return Type.WARNING;
	}

	public static class Holder extends StatusDisplayItem.Holder<WarningFilteredStatusDisplayItem>{
		public final View warningWrap;
		public final Button showBtn;
		public final TextView text;
		public ArrayList<StatusDisplayItem> filteredItems;

		public Holder(Context context, ViewGroup parent) {
			super(context, R.layout.display_item_warning, parent);
			warningWrap=findViewById(R.id.warning_wrap);
			showBtn=findViewById(R.id.reveal_btn);
			showBtn.setOnClickListener(i -> item.parentFragment.onWarningClick(this));
			text=findViewById(R.id.text);
		}

		@Override
		public void onBind(WarningFilteredStatusDisplayItem item) {
			filteredItems = item.filteredItems;
			text.setText(item.parentFragment.getString(R.string.sk_filtered, item.status.filtered.get(item.status.filtered.size() -1).filter.title));
		}

		@Override
		public void onClick() {}
	}
}
