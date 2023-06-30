package org.joinmastodon.android.api.requests.timelines;

import com.google.gson.reflect.TypeToken;

import android.text.TextUtils;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class GetHashtagTimeline extends MastodonAPIRequest<List<Status>>{
	public GetHashtagTimeline(String hashtag, String maxID, String minID, int limit, List<String> containsAny, List<String> containsAll, List<String> containsNone, boolean localOnly){
		super(HttpMethod.GET, "/timelines/tag/"+hashtag, new TypeToken<>(){});
		if (localOnly) addQueryParameter("local", "true");
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(minID!=null)
			addQueryParameter("min_id", minID);
		if(limit>0)
			addQueryParameter("limit", ""+limit);
		if(containsAny!=null && !containsAny.isEmpty())
			addQueryParameter("any[]", "[" + TextUtils.join(",", containsAny) + "]");
		if(containsAll!=null && !containsAll.isEmpty())
			addQueryParameter("all[]", "[" + TextUtils.join(",", containsAll) + "]");
		if(containsNone!=null && !containsNone.isEmpty())
			addQueryParameter("none[]", "[" + TextUtils.join(",", containsNone) + "]");
	}

	public GetHashtagTimeline(String hashtag, String maxID, String minID, int limit){
		super(HttpMethod.GET, "/timelines/tag/"+hashtag, new TypeToken<>(){});
		if(maxID!=null)
			addQueryParameter("max_id", maxID);
		if(minID!=null)
			addQueryParameter("min_id", minID);
		if(limit>0)
			addQueryParameter("limit", ""+limit);
		if(GlobalUserPreferences.replyVisibility != null)
			addQueryParameter("reply_visibility", GlobalUserPreferences.replyVisibility);
	}
}
