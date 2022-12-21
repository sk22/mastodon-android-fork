package org.joinmastodon.android.ui.displayitems;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.TypefaceSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.StatusEditHistoryFragment;
import org.joinmastodon.android.fragments.account_list.StatusFavoritesListFragment;
import org.joinmastodon.android.fragments.account_list.StatusReblogsListFragment;
import org.joinmastodon.android.fragments.account_list.StatusRelatedAccountListFragment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import androidx.annotation.PluralsRes;
import me.grishka.appkit.Nav;

public class ExtendedFooterStatusDisplayItem extends StatusDisplayItem{
	public final Status status;

	private static final DateTimeFormatter TIME_FORMATTER=DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT);

	public ExtendedFooterStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Status status){
		super(parentID, parentFragment);
		this.status=status;
	}

	@Override
	public Type getType(){
		return Type.EXTENDED_FOOTER;
	}

	public static class Holder extends StatusDisplayItem.Holder<ExtendedFooterStatusDisplayItem>{
		private final TextView time;
		private final Button favorites, reblogs, editHistory, applicationName;
		private final ImageView visibility;
		private final Context context;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_extended_footer, parent);
			this.context = context;
			reblogs=findViewById(R.id.reblogs);
			favorites=findViewById(R.id.favorites);
			editHistory=findViewById(R.id.edit_history);
			applicationName=findViewById(R.id.application_name);
			visibility=findViewById(R.id.visibility);
			time=findViewById(R.id.timestamp);

			reblogs.setOnClickListener(v->startAccountListFragment(StatusReblogsListFragment.class));
			favorites.setOnClickListener(v->startAccountListFragment(StatusFavoritesListFragment.class));
			editHistory.setOnClickListener(v->startEditHistoryFragment());
		}

		@SuppressLint("DefaultLocale")
		@Override
		public void onBind(ExtendedFooterStatusDisplayItem item){
			Status s=item.status;
			favorites.setText(context.getResources().getQuantityString(R.plurals.x_favorites, (int)(s.favouritesCount%1000), s.favouritesCount));
			reblogs.setText(context.getResources().getQuantityString(R.plurals.x_reblogs, (int)(s.reblogsCount%1000), s.reblogsCount));
			if(s.editedAt!=null){
				editHistory.setVisibility(View.VISIBLE);
				editHistory.setText(UiUtils.formatRelativeTimestampAsMinutesAgo(itemView.getContext(), s.editedAt));
			}else{
				editHistory.setVisibility(View.GONE);
			}
			String timeStr=TIME_FORMATTER.format(item.status.createdAt.atZone(ZoneId.systemDefault()));
			
			if (item.status.application!=null && !TextUtils.isEmpty(item.status.application.name)) {
				time.setText(item.parentFragment.getString(R.string.timestamp_via_app, timeStr, ""));
				applicationName.setText(item.status.application.name);
				if (item.status.application.website != null && item.status.application.website.toLowerCase().startsWith("https://")) {
					applicationName.setOnClickListener(e -> UiUtils.openURL(context, null, item.status.application.website));
				} else {
					applicationName.setEnabled(false);
				}
			} else {
				time.setText(timeStr);
				applicationName.setVisibility(View.GONE);
			}

			visibility.setImageResource(switch (s.visibility) {
				case PUBLIC -> R.drawable.ic_fluent_earth_20_regular;
				case UNLISTED -> R.drawable.ic_fluent_people_community_20_regular;
				case PRIVATE -> R.drawable.ic_fluent_people_checkmark_20_regular;
				case DIRECT -> R.drawable.ic_fluent_mention_20_regular;
			});
		}

		@Override
		public boolean isEnabled(){
			return false;
		}

		private SpannableStringBuilder getFormattedPlural(@PluralsRes int res, int quantity){
			String str=item.parentFragment.getResources().getQuantityString(res, quantity, quantity);
			String formattedNumber=String.format(Locale.getDefault(), "%,d", quantity);
			int index=str.indexOf(formattedNumber);
			SpannableStringBuilder ssb=new SpannableStringBuilder(str);
			if(index>=0){
				ssb.setSpan(new TypefaceSpan("sans-serif-medium"), index, index+formattedNumber.length(), 0);
				ssb.setSpan(new ForegroundColorSpan(UiUtils.getThemeColor(item.parentFragment.getActivity(), android.R.attr.textColorPrimary)), index, index+formattedNumber.length(), 0);
			}
			return ssb;
		}

		private void startAccountListFragment(Class<? extends StatusRelatedAccountListFragment> cls){
			Bundle args=new Bundle();
			args.putString("account", item.parentFragment.getAccountID());
			args.putParcelable("status", Parcels.wrap(item.status));
			Nav.go(item.parentFragment.getActivity(), cls, args);
		}

		private void startEditHistoryFragment(){
			Bundle args=new Bundle();
			args.putString("account", item.parentFragment.getAccountID());
			args.putString("id", item.status.id);
			Nav.go(item.parentFragment.getActivity(), StatusEditHistoryFragment.class, args);
		}
	}
}
