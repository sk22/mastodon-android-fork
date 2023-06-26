package org.joinmastodon.android.api.requests.accounts;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.LegacyFilter;

import java.util.List;

public class GetWordFilters extends MastodonAPIRequest<List<LegacyFilter>>{
	public GetWordFilters(){
		super(HttpMethod.GET, "/filters", new TypeToken<>(){});
	}
}
