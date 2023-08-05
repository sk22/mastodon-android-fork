package org.joinmastodon.android.api.requests.timelines;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class GetHashtagTimeline extends MastodonAPIRequest<List<Status>>{
	public GetHashtagTimeline(String hashtag, String maxID, String minID, int limit, List<String> containsAny, List<String> containsAll, List<String> containsNone, boolean localOnly, String replyVisibility){
		super(HttpMethod.GET, "/timelines/tag/"+hashtag, new TypeToken<>(){});
		if (localOnly)
			addQueryParameter("local", "true");
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(minID!=null)
			addQueryParameter("min_id", minID);
		if(limit>0)
			addQueryParameter("limit", ""+limit);
		if(containsAny!=null)
			for (String tag : containsAny)
				addQueryParameter("any[]", tag);
		if(containsAll!=null)
			for (String tag : containsAll)
				addQueryParameter("all[]", tag);
		if(containsNone!=null)
			for (String tag : containsNone)
				addQueryParameter("none[]", tag);
		if(replyVisibility != null)
			addQueryParameter("reply_visibility", replyVisibility);
	}
}
