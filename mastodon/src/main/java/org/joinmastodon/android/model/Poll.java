package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Parcel
public class Poll extends BaseModel{
	@RequiredField
	public String id;
	public Instant expiresAt;
	protected boolean expired;
	public boolean multiple;
	public int votersCount;
	public int votesCount;
	public boolean voted;
//	@RequiredField
	public List<Integer> ownVotes;
	@RequiredField
	public List<Option> options;
//	@RequiredField
	public List<Emoji> emojis;

	public transient ArrayList<Option> selectedOptions;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if (emojis == null) emojis = List.of();
		if (ownVotes == null) ownVotes = List.of();
		for(Emoji e:emojis)
			e.postprocess();
	}

	@Override
	public String toString(){
		return "Poll{"+
				"id='"+id+'\''+
				", expiresAt="+expiresAt+
				", expired="+expired+
				", multiple="+multiple+
				", votersCount="+votersCount+
				", votesCount="+votesCount+
				", voted="+voted+
				", ownVotes="+ownVotes+
				", options="+options+
				", emojis="+emojis+
				", selectedOptions="+selectedOptions+
				'}';
	}

	public boolean isExpired(){
		return expired || (expiresAt!=null && expiresAt.isBefore(Instant.now()));
	}

	@Parcel
	public static class Option{
		public String title;
		public Integer votesCount;

		public Option() {}
		public Option(String title) {
			this.title = title;
		}

		@Override
		public String toString(){
			return "Option{"+
					"title='"+title+'\''+
					", votesCount="+votesCount+
					'}';
		}
	}
}
