package org.joinmastodon.android.api.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.FollowList;

public class UpdateList extends MastodonAPIRequest<FollowList> {
	public UpdateList(String id, String title, boolean exclusive, FollowList.RepliesPolicy repliesPolicy) {
		super(HttpMethod.PUT, "/lists/" + id, FollowList.class);
		CreateList.Request req = new CreateList.Request();
		req.title = title;
		req.exclusive = exclusive;
		req.repliesPolicy = repliesPolicy;
		setRequestBody(req);
	}
}
