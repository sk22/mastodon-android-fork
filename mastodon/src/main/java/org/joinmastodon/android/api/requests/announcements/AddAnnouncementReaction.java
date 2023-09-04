package org.joinmastodon.android.api.requests.announcements;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

public class AddAnnouncementReaction extends MastodonAPIRequest<Object> {
	public AddAnnouncementReaction(String id, String emoji) {
		super(HttpMethod.PUT, "/announcements/" + id + "/reactions/" + emoji, Object.class);
		setRequestBody(new Object());
	}
}
