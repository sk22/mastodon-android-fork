package org.joinmastodon.android.ui.displayitems;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.drawables.SawtoothTearDrawable;

// Mind the gap!
public class WarningFilteredStatusDisplayItem extends StatusDisplayItem{
    public boolean loading;
    public final Status status;

    public WarningFilteredStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Status status){
        super(parentID, parentFragment);
        this.status=status;
    }

    @Override
    public Type getType(){
        return Type.WARNING;
    }

    public static class Holder extends StatusDisplayItem.Holder<WarningFilteredStatusDisplayItem>{
        public final View warningWrap;
        public final ProgressBar progress;
        public final TextView text;

        public Holder(Context context, ViewGroup parent){
            super(context, R.layout.display_item_warning, parent);
            warningWrap=findViewById(R.id.warning_wrap);
            progress=findViewById(R.id.progress);
            text=findViewById(R.id.text);
//            itemView.setOnClickListener(v->item.parentFragment.onRevealFilteredClick(this));
        }

        @Override
        public void onBind(WarningFilteredStatusDisplayItem item){
            text.setVisibility(item.loading ? View.GONE : View.VISIBLE);
        }

        @Override
        public void onClick(){
            item.parentFragment.onWarningClick(this);
        }
    }
}
