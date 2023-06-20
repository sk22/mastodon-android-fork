package org.joinmastodon.android.model;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import org.joinmastodon.android.api.ObjectValidationException;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Parcel
public class Filter extends BaseModel{
	public String id;
	public String phrase;
	public String title;
	public transient EnumSet<FilterContext> context=EnumSet.noneOf(FilterContext.class);
	public Instant expiresAt;
	public boolean irreversible;
	public boolean wholeWord;
	public FilterAction filterAction;

	@SerializedName("context")
	protected List<FilterContext> _context;

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
		THREAD,
		@SerializedName("account")
		ACCOUNT
	}

	public enum FilterAction{
		@SerializedName("hide")
		HIDE,
		@SerializedName("warn")
		WARN
	}
}
