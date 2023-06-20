package org.joinmastodon.android.api.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.ListTimeline;

public class UpdateList extends MastodonAPIRequest<ListTimeline> {
	public UpdateList(String id, String title, boolean exclusive, ListTimeline.RepliesPolicy repliesPolicy) {
		super(HttpMethod.PUT, "/lists/" + id, ListTimeline.class);
		CreateList.Request req = new CreateList.Request();
		req.title = title;
		req.exclusive = exclusive;
		req.repliesPolicy = repliesPolicy;
		setRequestBody(req);
	}
}
