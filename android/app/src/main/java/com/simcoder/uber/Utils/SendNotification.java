package com.simcoder.uber.Utils;

import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by simco on 3/23/2018.
 */

/*
 *  this class handles the sending of notifications to a specific user with oneSignal
 */
public class SendNotification {

    /**
     * @param message - body of the notification,
     *                heading - header of the notification
     *                sendUid - user to send the notification to
     * @param heading - title of notification
     * @param notificationKey - notification Key of user
     */
    public SendNotification(final String message, final String heading, final String notificationKey) {

        try {
            JSONObject notificationContent = new JSONObject("{'contents': {'en': '" + message + "'}," +
                    "'include_player_ids': ['" + notificationKey + "'], " +
                    "'headings': {'en': '" + heading + "'}}");
            OneSignal.postNotification(notificationContent, null);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

}
