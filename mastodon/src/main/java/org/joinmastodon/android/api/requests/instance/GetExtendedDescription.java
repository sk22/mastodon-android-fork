package org.joinmastodon.android.api.requests.instance;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.ExtendedDescription;
import org.joinmastodon.android.model.Instance;

public class GetExtendedDescription extends MastodonAPIRequest<ExtendedDescription>{
	public GetExtendedDescription(){
		super(HttpMethod.GET, "/instance/extended_description", ExtendedDescription.class);
	}

}
