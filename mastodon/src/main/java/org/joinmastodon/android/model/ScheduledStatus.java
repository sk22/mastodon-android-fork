package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.List;

@Parcel
public class ScheduledStatus extends BaseModel implements DisplayItemsParent{
    @RequiredField
    public String id;
    @RequiredField
    public Instant scheduledAt;
    @RequiredField
    public Status params;
    @RequiredField
    public List<Attachment> mediaAttachments;

    @Override
    public String getID() {
        return id;
    }

    @Override
    public void postprocess() throws ObjectValidationException {
        super.postprocess();
        params.mediaAttachments = mediaAttachments;
        params.createdAt = scheduledAt;
        params.mentions = List.of();
        params.tags = List.of();
        params.emojis = List.of();
    }
}
