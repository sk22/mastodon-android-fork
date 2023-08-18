package org.joinmastodon.android.events;

import androidx.recyclerview.widget.RecyclerView;

import org.joinmastodon.android.api.CacheController;
import org.joinmastodon.android.model.EmojiReaction;
import org.joinmastodon.android.model.Status;

import java.util.ArrayList;
import java.util.List;

public class StatusCountersUpdatedEvent{
	public String id;
	public long favorites, reblogs, replies;
	public boolean favorited, reblogged, bookmarked, pinned;
	public List<EmojiReaction> reactions;
	public Status status;
	public RecyclerView.ViewHolder viewHolder;

	public StatusCountersUpdatedEvent(Status s){
		this(s, null);
	}

	public StatusCountersUpdatedEvent(Status s, RecyclerView.ViewHolder vh){
		id=s.id;
		status=s;
		favorites=s.favouritesCount;
		reblogs=s.reblogsCount;
		replies=s.repliesCount;
		favorited=s.favourited;
		reblogged=s.reblogged;
		bookmarked=s.bookmarked;
		pinned=s.pinned;
		reactions=new ArrayList<>(s.reactions);
		viewHolder=vh;
	}
}
