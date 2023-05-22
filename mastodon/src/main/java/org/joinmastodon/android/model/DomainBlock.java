package org.joinmastodon.android.model;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

@Parcel
public class DomainBlock extends BaseModel {
	@RequiredField
	public String domain;
	@RequiredField
	public String digest;
	@RequiredField
	public Severity severity;
	public String comment;

	@Override
	public String toString() {
		return "DomainBlock{" +
				"domain='" + domain + '\'' +
				", digest='" + digest + '\'' +
				", severity='" + severity + '\'' +
				", comment='" + comment + '\'' +
				'}';
	}


}
