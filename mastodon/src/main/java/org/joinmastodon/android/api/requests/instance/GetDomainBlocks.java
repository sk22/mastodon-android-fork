package org.joinmastodon.android.api.requests.instance;

import com.google.gson.reflect.TypeToken;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.DomainBlock;
import org.joinmastodon.android.model.ExtendedDescription;

import java.util.List;

public class GetDomainBlocks extends MastodonAPIRequest<List<DomainBlock>>{
	public GetDomainBlocks(){
		super(HttpMethod.GET, "/instance/domain_blocks", new TypeToken<>(){});
	}

}
