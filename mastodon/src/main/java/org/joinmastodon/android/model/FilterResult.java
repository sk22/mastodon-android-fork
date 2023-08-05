package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.parceler.Parcel;

import java.util.List;

@Parcel
public class FilterResult extends BaseModel {
    public LegacyFilter filter;

    public List<String> keywordMatches;

    @Override
    public void postprocess() throws ObjectValidationException {
        super.postprocess();
        if (filter != null) filter.postprocess();
    }
}
