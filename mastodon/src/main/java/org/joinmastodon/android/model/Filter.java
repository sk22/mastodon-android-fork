package org.joinmastodon.android.model;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

@Parcel
public class Filter extends BaseModel{
	@RequiredField
	public String id;
	@RequiredField
	public String phrase;
	public transient EnumSet<FilterContext> context=EnumSet.noneOf(FilterContext.class);
	public Instant expiresAt;
	public boolean irreversible;
	public boolean wholeWord;
	public String filterAction;

	@SerializedName("context")
	private List<FilterContext> _context;

	private transient Pattern pattern;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(_context==null)
			throw new ObjectValidationException();
		for(FilterContext c:_context){
			if(c!=null)
				context.add(c);
		}
	}

	public boolean matches(CharSequence text){
		if(TextUtils.isEmpty(text))
			return false;
		if(pattern==null){
			if(wholeWord)
				pattern=Pattern.compile("\\b"+Pattern.quote(phrase)+"\\b", Pattern.CASE_INSENSITIVE);
			else
				pattern=Pattern.compile(Pattern.quote(phrase), Pattern.CASE_INSENSITIVE);
		}
		return pattern.matcher(text).find();
	}

	public boolean matches(Status status){
		return matches(status.getContentStatus().getStrippedText());
	}

	@Override
	public String toString(){
		return "Filter{"+
				"id='"+id+'\''+
				", phrase='"+phrase+'\''+
				", context="+context+
				", expiresAt="+expiresAt+
				", irreversible="+irreversible+
				", wholeWord="+wholeWord+
				'}';
	}

	public enum FilterContext{
		@SerializedName("home")
		HOME,
		@SerializedName("notifications")
		NOTIFICATIONS,
		@SerializedName("public")
		PUBLIC,
		@SerializedName("thread")
		THREAD
	}
}
