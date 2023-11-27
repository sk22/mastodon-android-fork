package org.joinmastodon.android.api.requests.lists;

import org.joinmastodon.android.api.MastodonAPIRequest;

public class DeleteList extends MastodonAPIRequest<Object> {
	public DeleteList(String id) {
		super(HttpMethod.DELETE, "/lists/" + id, Object.class);
	}
}
