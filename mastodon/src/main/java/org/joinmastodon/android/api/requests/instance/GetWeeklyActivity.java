package org.joinmastodon.android.api.requests.instance;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.WeeklyActivity;

import java.util.List;

public class GetWeeklyActivity extends MastodonAPIRequest<List<WeeklyActivity>>{
	public GetWeeklyActivity(){
		super(HttpMethod.GET, "/instance/activity", new TypeToken<>(){});
	}

}
