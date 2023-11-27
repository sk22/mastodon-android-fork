package org.joinmastodon.android.api.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.FollowList;

public class CreateList extends MastodonAPIRequest<FollowList> {
	public CreateList(String title, boolean exclusive, FollowList.RepliesPolicy repliesPolicy) {
		super(HttpMethod.POST, "/lists", FollowList.class);
		Request req = new Request();
		req.title = title;
		req.exclusive = exclusive;
		req.repliesPolicy = repliesPolicy;
		setRequestBody(req);
	}

	public static class Request {
		public String title;
		public boolean exclusive;
		public FollowList.RepliesPolicy repliesPolicy;
	}
}
