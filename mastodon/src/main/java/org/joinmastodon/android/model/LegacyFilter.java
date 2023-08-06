package org.joinmastodon.android.model;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

@Parcel
public class LegacyFilter extends BaseModel{
	public String id;
	public String phrase;
	public String title;
	public transient EnumSet<FilterContext> context=EnumSet.noneOf(FilterContext.class);
	public Instant expiresAt;
	public boolean irreversible;
	public boolean wholeWord;

	@SerializedName("context")
	protected List<FilterContext> _context;

	public FilterAction filterAction;

	public List<FilterKeyword> keywords=new ArrayList<>();

	public List<FilterStatus> statuses=new ArrayList<>();

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
		for(FilterKeyword keyword:keywords)
			keyword.postprocess();
		for(FilterStatus status:statuses)
			status.postprocess();
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
		if (title == null) title = phrase;
		return pattern.matcher(text).find();
	}

	public boolean matches(Status status){
		return matches(status.getContentStatus().getStrippedText());
	}

	public boolean isActive(){
		return expiresAt==null || expiresAt.isAfter(Instant.now());
	}

	@Override
	public String toString(){
		return "Filter{"+
				"id='"+id+'\''+
				", title='"+title+'\''+
				", phrase='"+phrase+'\''+
				", context="+context+
				", expiresAt="+expiresAt+
				", irreversible="+irreversible+
				", wholeWord="+wholeWord+
				", filterAction="+filterAction+
				", keywords="+keywords+
				", statuses="+statuses+
				'}';
	}
}
