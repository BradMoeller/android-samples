/*
Copyright 2009-2014 Urban Airship Inc. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation
and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE URBAN AIRSHIP INC ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
EVENT SHALL URBAN AIRSHIP INC OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.urbanairship.richpush.sample;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.text.Html;

import com.urbanairship.UAirship;
import com.urbanairship.push.PushManager;
import com.urbanairship.push.PushNotificationBuilder;
import com.urbanairship.push.PushPreferences;
import com.urbanairship.richpush.RichPushInbox;
import com.urbanairship.richpush.RichPushManager;
import com.urbanairship.richpush.RichPushMessage;
import com.urbanairship.util.NotificationIDGenerator;

import java.util.List;
import java.util.Map;

/**
 * A custom push notification builder to create inbox style notifications
 * for rich push messages.  In the case of standard push notifications, it will
 * fall back to the default behavior.
 *
 */
public class RichNotificationBuilder implements PushNotificationBuilder {

    private static final int EXTRA_MESSAGES_TO_SHOW = 2;
    private static final int INBOX_NOTIFICATION_ID = 9000000;

    @Override
    public Notification buildNotification(String alert, Map<String, String> extras) {
        if (extras != null && RichPushManager.isRichPushMessage(extras)) {
            return createInboxNotification(alert);
        } else {
            return createNotification(alert);
        }
    }

    @Override
    public int getNextId(String alert, Map<String, String> extras) {
        if (extras != null && extras.containsKey(PushReceiver.EXTRA_MESSAGE_ID_KEY)) {
            return INBOX_NOTIFICATION_ID;
        } else {
            return NotificationIDGenerator.nextID();
        }
    }

    /**
     * Creates an inbox style notification summarizing the unread messages
     * in the inbox
     *
     * @param incomingAlert The alert message from an Urban Airship push
     * @return An inbox style notification
     */
    private Notification createInboxNotification(String incomingAlert) {
        Context context = UAirship.shared().getApplicationContext();

        List<RichPushMessage> unreadMessages = RichPushInbox.shared().getUnreadMessages();
        int totalUnreadCount = unreadMessages.size();

        // If we do not have any unread messages (message already read or they failed to fetch)
        // show a normal notification.
        if (totalUnreadCount == 0) {
            return createNotification(incomingAlert);
        }

        Resources res = UAirship.shared().getApplicationContext().getResources();
        String title = res.getQuantityString(R.plurals.inbox_notification_title, totalUnreadCount, totalUnreadCount);

        Bitmap largeIcon = BitmapFactory.decodeResource(res, R.drawable.ua_launcher);

        NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle(
                new NotificationCompat.Builder(context)
                    .setDefaults(getNotificationDefaults())
                    .setContentTitle(title)
                    .setContentText(incomingAlert)
                    .setLargeIcon(largeIcon)
                    .setSmallIcon(R.drawable.ua_notification_icon)
                    .setNumber(totalUnreadCount)
                    .setAutoCancel(true)
        );

        // Add the incoming alert as the first line in bold
        style.addLine(Html.fromHtml("<b>"+incomingAlert+"</b>"));

        // Add any extra messages to the notification style
        int extraMessages =  Math.min(EXTRA_MESSAGES_TO_SHOW, totalUnreadCount);
        for (int i = 0; i < extraMessages; i++) {
            style.addLine(unreadMessages.get(i).getTitle());
        }

        // If we have more messages to show then the EXTRA_MESSAGES_TO_SHOW, add a summary
        if (totalUnreadCount > EXTRA_MESSAGES_TO_SHOW) {
            style.setSummaryText(context.getString(R.string.inbox_summary, totalUnreadCount - EXTRA_MESSAGES_TO_SHOW));
        }

        return style.build();
    }

    private Notification createNotification(String alert) {
        Resources res = UAirship.shared().getApplicationContext().getResources();
        Bitmap largeIcon = BitmapFactory.decodeResource(res, R.drawable.ua_launcher);

        return new NotificationCompat.Builder(UAirship.shared().getApplicationContext())
                .setContentTitle(UAirship.getAppName())
                .setContentText(alert)
                .setDefaults(getNotificationDefaults())
                .setSmallIcon(R.drawable.ua_notification_icon)
                .setLargeIcon(largeIcon)
                .setAutoCancel(true)
                .build();
    }

    /**
     * Dismisses the inbox style notification if it exists
     */
    public static void dismissInboxNotification() {
        NotificationManager manager = (NotificationManager) UAirship.shared().
                getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        manager.cancel(INBOX_NOTIFICATION_ID);
    }

    /**
     * Gets the notification defaults based on
     * the PushPreferences for quiet time, vibration enabled,
     * and sound enabled.
     *
     * @return Notification defaults
     */
    private int getNotificationDefaults() {
        PushPreferences prefs = PushManager.shared().getPreferences();
        int defaults = Notification.DEFAULT_LIGHTS;

        if (!prefs.isInQuietTime()) {
            if (prefs.isVibrateEnabled()) {
                defaults |= Notification.DEFAULT_VIBRATE;
            }

            if (prefs.isSoundEnabled()) {
                defaults |= Notification.DEFAULT_SOUND;
            }
        }

        return defaults;
    }
}
