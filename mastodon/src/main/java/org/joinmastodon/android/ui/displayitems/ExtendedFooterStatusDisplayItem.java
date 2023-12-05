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

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.fragments.StatusEditHistoryFragment;
import org.joinmastodon.android.fragments.ThreadFragment;
import org.joinmastodon.android.fragments.account_list.StatusFavoritesListFragment;
import org.joinmastodon.android.fragments.account_list.StatusReblogsListFragment;
import org.joinmastodon.android.fragments.account_list.StatusRelatedAccountListFragment;
import org.joinmastodon.android.model.Status;
import org.joinmastodon.android.model.StatusPrivacy;
import org.joinmastodon.android.ui.Snackbar;
import org.joinmastodon.android.ui.utils.UiUtils;
import org.parceler.Parcels;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

import androidx.annotation.PluralsRes;
import me.grishka.appkit.Nav;

public class ExtendedFooterStatusDisplayItem extends StatusDisplayItem{
	public final String accountID;

	private static final DateTimeFormatter TIME_FORMATTER=DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT);
	private static final DateTimeFormatter TIME_FORMATTER_LONG=DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM);

	public ExtendedFooterStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, String accountID, Status status){
		super(parentID, parentFragment);
		this.status=status;
		this.accountID=accountID;
	}

	@Override
	public Type getType(){
		return Type.EXTENDED_FOOTER;
	}

	public static class Holder extends StatusDisplayItem.Holder<ExtendedFooterStatusDisplayItem>{
		private final TextView dateAppSeparator;
		private final Button favorites, reblogs, editHistory, app, time;
		private final ImageView visibility;
		private final Context context;

		public Holder(Context context, ViewGroup parent){
			super(context, R.layout.display_item_extended_footer, parent);
			this.context = context;
			reblogs=findViewById(R.id.reblogs);
			favorites=findViewById(R.id.favorites);
			editHistory=findViewById(R.id.edit_history);
			app=findViewById(R.id.app_name);
			visibility=findViewById(R.id.visibility);
			time=findViewById(R.id.time);
			dateAppSeparator=findViewById(R.id.date_app_separator);

			reblogs.setOnClickListener(v->startAccountListFragment(StatusReblogsListFragment.class));
			favorites.setOnClickListener(v->startAccountListFragment(StatusFavoritesListFragment.class));
			editHistory.setOnClickListener(v->startEditHistoryFragment());
			time.setOnClickListener(v->showTimeSnackbar());
			app.setOnClickListener(v->UiUtils.launchWebBrowser(context, item.status.application.website));
		}

		@SuppressLint("DefaultLocale")
		@Override
		public void onBind(ExtendedFooterStatusDisplayItem item){
			Status s=item.status;
			favorites.setCompoundDrawablesRelativeWithIntrinsicBounds(GlobalUserPreferences.likeIcon ? R.drawable.ic_fluent_heart_20_regular : R.drawable.ic_fluent_star_20_regular, 0, 0, 0);
			favorites.setText(context.getResources().getQuantityString(R.plurals.x_favorites, (int)(s.favouritesCount%1000), s.favouritesCount));
			reblogs.setText(context.getResources().getQuantityString(R.plurals.x_reblogs, (int) (s.reblogsCount % 1000), s.reblogsCount));
			reblogs.setVisibility(s.visibility != StatusPrivacy.DIRECT ? View.VISIBLE : View.GONE);

			if(s.editedAt!=null){
				editHistory.setVisibility(View.VISIBLE);
				editHistory.setText(UiUtils.formatRelativeTimestampAsMinutesAgo(itemView.getContext(), s.editedAt, false));
			}else{
				editHistory.setVisibility(View.GONE);
			}

			ZonedDateTime dt=item.status.createdAt.atZone(ZoneId.systemDefault());
			time.setText(TIME_FORMATTER.format(dt));
			if(item.status.application!=null && !TextUtils.isEmpty(item.status.application.name)){
				app.setVisibility(View.VISIBLE);
				dateAppSeparator.setVisibility(View.VISIBLE);
				app.setText(item.status.application.name);
				app.setEnabled(!TextUtils.isEmpty(item.status.application.website));
			} else {
				app.setVisibility(View.GONE);
				dateAppSeparator.setVisibility(View.GONE);
			}

			visibility.setImageResource(switch (s.visibility) {
				case PUBLIC -> R.drawable.ic_fluent_earth_20_regular;
				case UNLISTED -> R.drawable.ic_fluent_lock_open_20_regular;
				case PRIVATE -> R.drawable.ic_fluent_lock_closed_20_filled;
				case DIRECT -> R.drawable.ic_fluent_mention_20_regular;
				case LOCAL -> R.drawable.ic_fluent_eye_20_regular;
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
			if(item.status.preview) return;
			Bundle args=new Bundle();
			args.putString("account", item.parentFragment.getAccountID());
			args.putParcelable("status", Parcels.wrap(item.status));
			Nav.go(item.parentFragment.getActivity(), cls, args);
		}

		private void startEditHistoryFragment(){
			if(item.status.preview) return;
			Bundle args=new Bundle();
			args.putString("account", item.parentFragment.getAccountID());
			args.putString("id", item.status.id);
			args.putString("url", item.status.url);
			Nav.go(item.parentFragment.getActivity(), StatusEditHistoryFragment.class, args);
		}

		private void showTimeSnackbar(){
			int bottomOffset=0;
			if(item.parentFragment instanceof ThreadFragment tf){
				bottomOffset=tf.getSnackbarOffset();
			}
			new Snackbar.Builder(itemView.getContext())
					.setText(TIME_FORMATTER_LONG.format(item.status.createdAt.atZone(ZoneId.systemDefault())))
					.setBottomOffset(bottomOffset)
					.show();
		}
	}
}
