package org.joinmastodon.android.model;

import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.util.List;

@Parcel
public class ExtendedDescription extends BaseModel{
	@RequiredField
	public String content;
	@RequiredField
	public String updatedAt;

	@Override
	public String toString() {
		return "ExtendedDescription{" +
				"content='" + content + '\'' +
				", updatedAt='" + updatedAt + '\'' +
				'}';
	}
}
