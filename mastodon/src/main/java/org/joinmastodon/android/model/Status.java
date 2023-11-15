package org.joinmastodon.android.model;

import static org.joinmastodon.android.api.MastodonAPIController.gson;
import static org.joinmastodon.android.api.MastodonAPIController.gsonWithoutDeserializer;

import android.text.TextUtils;
import android.util.Pair;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.joinmastodon.android.events.StatusCountersUpdatedEvent;
import org.joinmastodon.android.ui.text.HtmlParser;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import androidx.annotation.NonNull;

import com.github.bottomSoftwareFoundation.bottom.Bottom;
import com.github.bottomSoftwareFoundation.bottom.TranslationError;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import org.joinmastodon.android.api.session.AccountSession;
import org.joinmastodon.android.api.session.AccountSessionManager;
import org.joinmastodon.android.events.EmojiReactionsUpdatedEvent;
import org.joinmastodon.android.utils.StatusTextEncoder;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.regex.Pattern;

@Parcel
public class Status extends BaseModel implements DisplayItemsParent, Searchable{
	@RequiredField
	public String id;
	@RequiredField
	public String uri;
//	@RequiredField // sometimes null on calckey
	public Instant createdAt;
	@RequiredField
	public Account account;
//	@RequiredField
	public String content;
	@RequiredField
	public StatusPrivacy visibility;
	public boolean sensitive;
	@RequiredField
	public String spoilerText;
	public List<Attachment> mediaAttachments;
	public Application application;
	@RequiredField
	public List<Mention> mentions;
	@RequiredField
	public List<Hashtag> tags;
	@RequiredField
	public List<Emoji> emojis;
	public long reblogsCount;
	public long favouritesCount;
	public long repliesCount;
	public Instant editedAt;
	public List<FilterResult> filtered;

	public String url;
	public String inReplyToId;
	public String inReplyToAccountId;
	public Status reblog;
	public Poll poll;
	public Card card;
	public String language;
	public String text;
	public boolean localOnly;

	public boolean favourited;
	public boolean reblogged;
	public boolean muted;
	public boolean bookmarked;
	public boolean pinned;

	public Status quote; // can be boolean in calckey

	public List<EmojiReaction> reactions;
	protected List<EmojiReaction> emojiReactions; // akkoma

	public transient boolean filterRevealed;
	public transient boolean spoilerRevealed;
	public transient boolean sensitiveRevealed;
	public transient boolean textExpanded, textExpandable;
	public transient String hasGapAfter;
	private transient String strippedText;
	public transient TranslationState translationState=TranslationState.HIDDEN;
	public transient Translation translation;
	public transient boolean fromStatusCreated;

	public Status(){}

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(application!=null)
			application.postprocess();
		for(Mention m:mentions)
			m.postprocess();
		for(Hashtag t:tags)
			t.postprocess();
		for(Emoji e:emojis)
			e.postprocess();
		if (mediaAttachments == null) mediaAttachments=List.of();
		for(Attachment a:mediaAttachments)
			a.postprocess();
		account.postprocess();
		if(poll!=null)
			poll.postprocess();
		if(card!=null)
			card.postprocess();
		if(reblog!=null)
			reblog.postprocess();
		if(filtered!=null)
			for(FilterResult fr:filtered)
				fr.postprocess();
		if(quote!=null)
			quote.postprocess();

		spoilerRevealed=!hasSpoiler();
		if(!spoilerRevealed) sensitive=true;
		sensitiveRevealed=!sensitive;
		if(visibility.equals(StatusPrivacy.LOCAL)) localOnly=true;
		if(emojiReactions!=null) reactions=emojiReactions;
		if(reactions==null) reactions=new ArrayList<>();
	}

	@Override
	public String toString(){
		return "Status{"+
				"id='"+id+'\''+
				", uri='"+uri+'\''+
				", createdAt="+createdAt+
				", account="+account+
				", content='"+content+'\''+
				", visibility="+visibility+
				", sensitive="+sensitive+
				", spoilerText='"+spoilerText+'\''+
				", mediaAttachments="+mediaAttachments+
				", application="+application+
				", mentions="+mentions+
				", tags="+tags+
				", emojis="+emojis+
				", reblogsCount="+reblogsCount+
				", favouritesCount="+favouritesCount+
				", repliesCount="+repliesCount+
				", editedAt="+editedAt+
				", url='"+url+'\''+
				", inReplyToId='"+inReplyToId+'\''+
				", inReplyToAccountId='"+inReplyToAccountId+'\''+
				", reblog="+reblog+
				", poll="+poll+
				", card="+card+
				", language='"+language+'\''+
				", text='"+text+'\''+
				", filtered="+filtered+
				", favourited="+favourited+
				", reblogged="+reblogged+
				", muted="+muted+
				", bookmarked="+bookmarked+
				", pinned="+pinned+
				", spoilerRevealed="+spoilerRevealed+
				", hasGapAfter="+hasGapAfter+
				", strippedText='"+strippedText+'\''+
				'}';
	}

	@Override
	public String getID(){
		return id;
	}

	public void update(StatusCountersUpdatedEvent ev){
		favouritesCount=ev.favorites;
		reblogsCount=ev.reblogs;
		repliesCount=ev.replies;
		favourited=ev.favorited;
		reblogged=ev.reblogged;
		bookmarked=ev.bookmarked;
		pinned=ev.pinned;
	}

	public void update(EmojiReactionsUpdatedEvent ev){
		reactions=ev.reactions;
	}

	public Status getContentStatus(){
		return reblog!=null ? reblog : this;
	}

	public String getContentStatusID(){
		return getContentStatus().id;
	}

	public String getStrippedText(){
		if(strippedText==null)
			strippedText=HtmlParser.strip(content);
		return strippedText;
	}

	public boolean hasSpoiler(){
		return !TextUtils.isEmpty(spoilerText);
	}

	@NonNull
	@Override
	public Status clone(){
		return (Status) super.clone();
	}

	public static final Pattern BOTTOM_TEXT_PATTERN = Pattern.compile("(?:[\uD83E\uDEC2\uD83D\uDC96✨\uD83E\uDD7A,]+|❤️)(?:\uD83D\uDC49\uD83D\uDC48(?:[\uD83E\uDEC2\uD83D\uDC96✨\uD83E\uDD7A,]+|❤️))*\uD83D\uDC49\uD83D\uDC48");
	public boolean isEligibleForTranslation(AccountSession session){
		Instance instanceInfo=AccountSessionManager.getInstance().getInstanceInfo(session.domain);
		boolean translateEnabled=instanceInfo!=null && (
				(instanceInfo.v2!=null && instanceInfo.v2.configuration.translation!=null && instanceInfo.v2.configuration.translation.enabled) ||
				(instanceInfo.isAkkoma() && instanceInfo.hasFeature(Instance.Feature.MACHINE_TRANSLATION))
		);

		try {
			Pair<String, List<String>> decoded=BOTTOM_TEXT_PATTERN.matcher(getStrippedText()).find()
					? new StatusTextEncoder(Bottom::decode).decode(getStrippedText(), BOTTOM_TEXT_PATTERN)
					: null;
			String bottomText=decoded==null || decoded.second.stream().allMatch(s->s.trim().isEmpty()) ? null : decoded.first;
			if(bottomText!=null){
				translation=new Translation();
				translation.content=bottomText;
				translation.detectedSourceLanguage="\uD83E\uDD7A\uD83D\uDC49\uD83D\uDC48";
				translation.provider="bottom-java";
				return true;
			}
		} catch (TranslationError ignored) {}

		return translateEnabled && !TextUtils.isEmpty(content) && !TextUtils.isEmpty(language)
				&& !Objects.equals(Locale.getDefault().getLanguage(), language)
				&& (visibility==StatusPrivacy.PUBLIC || visibility==StatusPrivacy.UNLISTED);
	}

	public enum TranslationState{
		HIDDEN,
		SHOWN,
		LOADING
	}

	public boolean isReblogPermitted(String accountID){
		return visibility.isReblogPermitted(account.id.equals(
				AccountSessionManager.getInstance().getAccount(accountID).self.id
		));
	}

	public static Status ofFake(String id, String text, Instant createdAt) {
		Status s=new Status();
		s.id=id;
		s.mediaAttachments=List.of();
		s.createdAt=createdAt;
		s.content=s.text=text;
		s.spoilerText="";
		s.visibility=StatusPrivacy.PUBLIC;
		s.reactions=List.of();
		s.mentions=List.of();
		s.tags=List.of();
		s.emojis=List.of();
		s.filtered=List.of();
		return s;
	}

	@Override
	public String getQuery() {
		return url;
	}

	public static class StatusDeserializer implements JsonDeserializer<Status> {
		@Override
		public Status deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
			JsonObject obj=json.getAsJsonObject();

			Status quote=null;
			if (obj.has("quote") && obj.get("quote").isJsonObject())
				quote=gson.fromJson(obj.get("quote"), Status.class);
			obj.remove("quote");

			Status reblog=null;
			if (obj.has("reblog"))
				reblog=gson.fromJson(obj.get("reblog"), Status.class);
			obj.remove("reblog");

			Status status=gsonWithoutDeserializer.fromJson(json, Status.class);
			status.quote=quote;
			status.reblog=reblog;

			return status;
		}
	}
}
