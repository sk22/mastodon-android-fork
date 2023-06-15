package org.joinmastodon.android.model;

import android.text.TextUtils;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;

@Parcel
public class Filter extends BaseModel{
	public String id;
	public String phrase;
	public String title;

	@RequiredField
	public EnumSet<FilterContext> context;

	public Instant expiresAt;
	public boolean irreversible;
	public boolean wholeWord;
	public FilterAction filterAction;

	public List<FilterKeyword> keywords=new ArrayList<>();

	public List<FilterStatus> statuses=new ArrayList<>();

	private transient Pattern pattern;

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		for(FilterKeyword keyword:keywords)
			keyword.postprocess();
		for(FilterStatus status:statuses)
			status.postprocess();
	}

	public boolean isActive(){
		return expiresAt==null || expiresAt.isAfter(Instant.now());
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
