package org.joinmastodon.android.ui.displayitems;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.joinmastodon.android.R;
import org.joinmastodon.android.fragments.BaseStatusListFragment;
import org.joinmastodon.android.model.Poll;
import org.joinmastodon.android.ui.utils.UiUtils;

public class PollFooterStatusDisplayItem extends StatusDisplayItem{
	public final Poll poll;
	public boolean resultsVisible=false;

	public PollFooterStatusDisplayItem(String parentID, BaseStatusListFragment parentFragment, Poll poll){
		super(parentID, parentFragment);
		this.poll=poll;
	}

	@Override
	public Type getType(){
		return Type.POLL_FOOTER;
	}

	public static class Holder extends StatusDisplayItem.Holder<PollFooterStatusDisplayItem>{
		private TextView text;
		private Button voteButton, resultsButton;
		private ViewGroup wrapper;

		public Holder(Activity activity, ViewGroup parent){
			super(activity, R.layout.display_item_poll_footer, parent);
			text=findViewById(R.id.text);
			voteButton=findViewById(R.id.vote_btn);
			voteButton.setOnClickListener(v->item.parentFragment.onPollVoteButtonClick(this));
			resultsButton=findViewById(R.id.results_btn);
			wrapper=findViewById(R.id.wrapper);
			resultsButton.setOnClickListener(v-> {
				item.resultsVisible = !item.resultsVisible;
				item.parentFragment.onPollViewResultsButtonClick(this, item.resultsVisible);
				rebind();
				UiUtils.beginLayoutTransition(wrapper);
			});
		}

		@Override
		public void onBind(PollFooterStatusDisplayItem item){
			String text=item.parentFragment.getResources().getQuantityString(R.plurals.x_votes, item.poll.votesCount, item.poll.votesCount);
			String sep=" "+item.parentFragment.getString(R.string.sk_separator)+" ";
			if(item.poll.expiresAt!=null && !item.poll.isExpired())
				text+=sep+UiUtils.formatTimeLeft(itemView.getContext(), item.poll.expiresAt).replaceAll(" ", " ");
			else if(item.poll.isExpired())
				text+=sep+item.parentFragment.getString(R.string.poll_closed).replaceAll(" ", " ");
			if(item.poll.multiple)
				text+=sep+item.parentFragment.getString(R.string.sk_poll_multiple_choice).replaceAll(" ", " ");
			this.text.setText(text);
			resultsButton.setVisibility(item.poll.isExpired() || item.poll.voted ? View.GONE : View.VISIBLE);
			resultsButton.setText(item.resultsVisible ? R.string.sk_poll_hide_results : R.string.sk_poll_show_results);
			resultsButton.setSelected(item.resultsVisible);
			voteButton.setVisibility(item.poll.isExpired() || item.poll.voted ? View.GONE : View.VISIBLE);
			voteButton.setEnabled(item.poll.selectedOptions!=null && !item.poll.selectedOptions.isEmpty() && !item.resultsVisible);
		}
	}
}
