package com.sweatnation.messages;

import com.sweatnation.ActivityEvent;
import java.util.ArrayList;
import java.util.List;

/** En eller flere hendelser til aktivitetsloggen (flere når en nykommer skal synkroniseres). */
public class ActivityShare extends SnMessage
{
	public List<ActivityEvent> events = new ArrayList<>();

	public ActivityShare()
	{
	}
}
