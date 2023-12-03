package org.joinmastodon.android.api.requests.tags;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.requests.HeaderPaginationRequest;
import org.joinmastodon.android.model.Hashtag;

public class GetFollowedTags extends HeaderPaginationRequest<Hashtag> {
    public GetFollowedTags() {
        this(null, null, -1, null);
    }

	public GetFollowedTags(String maxID, int limit){
		this(maxID, null, limit, null);
	}

    public GetFollowedTags(String maxID, String minID, int limit, String sinceID){
        super(HttpMethod.GET, "/followed_tags", new TypeToken<>(){});
        if(maxID!=null)
            addQueryParameter("max_id", maxID);
        if(minID!=null)
            addQueryParameter("min_id", minID);
        if(sinceID!=null)
            addQueryParameter("since_id", sinceID);
        if(limit>0)
            addQueryParameter("limit", limit+"");
    }
}

