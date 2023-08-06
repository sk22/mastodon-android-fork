package org.joinmastodon.android.api.requests.accounts;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Relationship;

public class SetAccountMuted extends MastodonAPIRequest<Relationship>{
	public SetAccountMuted(String id, boolean muted, long duration){
		super(HttpMethod.POST, "/accounts/"+id+"/"+(muted ? "mute" : "unmute"), Relationship.class);
		setRequestBody(new Request(duration));
	}

	private static class Request{
		public long duration;
		public Request(long duration){
			this.duration=duration;
		}
	}
}
