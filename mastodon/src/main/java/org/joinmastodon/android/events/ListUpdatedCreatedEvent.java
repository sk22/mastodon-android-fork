package org.joinmastodon.android.events;

import org.joinmastodon.android.model.ListTimeline;

public class ListUpdatedCreatedEvent {
	public final String id;
	public final String title;
	public final ListTimeline.RepliesPolicy repliesPolicy;
	public final boolean exclusive;

	public ListUpdatedCreatedEvent(String id, String title, boolean exclusive, ListTimeline.RepliesPolicy repliesPolicy) {
		this.id = id;
		this.title = title;
		this.exclusive = exclusive;
		this.repliesPolicy = repliesPolicy;
	}
}
