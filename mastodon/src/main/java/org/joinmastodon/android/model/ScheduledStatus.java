package org.joinmastodon.android.model;

import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.model.Poll.Option;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Parcel
public class ScheduledStatus extends BaseModel implements DisplayItemsParent{
    @RequiredField
    public String id;
    @RequiredField
    public Instant scheduledAt;
    @RequiredField
    public Params params;
    @RequiredField
    public List<Attachment> mediaAttachments;

    @Override
    public String getID() {
        return id;
    }

    @Parcel
    public static class Params {
        @RequiredField
        public String text;
        public String spoilerText;
        @RequiredField
        public StatusPrivacy visibility;
        public long inReplyToId;
        public ScheduledPoll poll;
        public boolean sensitive;
        public boolean withRateLimit;
        public String language;
        public String idempotency;
        public String applicationId;
        public List<String> mediaIds;
    }

    @Parcel
    public static class ScheduledPoll {
        @RequiredField
        public String expiresIn;
        @RequiredField
        public List<String> options;
        public boolean multiple;
        public boolean hideTotals;

        public Poll toPoll() {
            Poll p = new Poll();
            p.voted = true;
            p.emojis = List.of();
            p.ownVotes = List.of();
            p.multiple = multiple;
            p.options = options.stream().map(Option::new).collect(Collectors.toList());
            return p;
        }
    }

    public Status toStatus() {
        Status s = new Status();
        s.id = id;
        s.mediaAttachments = mediaAttachments;
        s.createdAt = scheduledAt;
        s.content = s.text = params.text;
        s.spoilerText = params.spoilerText;
        s.visibility = params.visibility;
        s.language = params.language;
        s.mentions = List.of();
        s.tags = List.of();
        s.emojis = List.of();
        if (params.poll != null) s.poll = params.poll.toPoll();
        return s;
    }
}
