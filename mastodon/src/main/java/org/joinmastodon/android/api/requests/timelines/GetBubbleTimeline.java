package org.joinmastodon.android.api.requests.timelines;

import android.text.TextUtils;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.GlobalUserPreferences;
import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.Status;

import java.util.List;

public class GetBubbleTimeline extends MastodonAPIRequest<List<Status>> {
    public GetBubbleTimeline(String maxID, int limit) {
        super(HttpMethod.GET, "/timelines/bubble", new TypeToken<>(){});
        if(!TextUtils.isEmpty(maxID))
            addQueryParameter("max_id", maxID);
        if(limit>0)
            addQueryParameter("limit", limit+"");
        if(GlobalUserPreferences.replyVisibility != null)
            addQueryParameter("reply_visibility", GlobalUserPreferences.replyVisibility);
    }
}
