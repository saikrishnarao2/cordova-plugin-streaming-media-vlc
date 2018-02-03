package com.hutchind.cordova.plugins.streamingmedia;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.os.Build;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

public class StreamingMedia extends CordovaPlugin {
	public static final String ACTION_PLAY_AUDIO = "playAudio";
	public static final String ACTION_PLAY_VIDEO = "playVideo";
	public static final String STOP_VIDEO = "stopVideo";
	private static final int ACTIVITY_CODE_PLAY_MEDIA = 7;

	private CallbackContext callbackContext;

	private static final String TAG = "StreamingMediaPlugin";

	private BroadcastReceiver receiver;
	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
		JSONObject options = null;

		try {
			options = args.getJSONObject(1);
		} catch (JSONException e) {
			// Developer provided no options. Leave options null.
		}
		IntentFilter intentFilter = new IntentFilter();
		//intentFilter.addAction(VitamioMedia.ACTION_INFO);
		if (this.receiver == null) {
			this.receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					Log.v("onRecive tmmate",intent.toString());
					updateMediaInfo(intent);
				}
			};
			cordova.getActivity().registerReceiver(this.receiver, intentFilter);
		}

		if (ACTION_PLAY_AUDIO.equals(action)) {
			return playAudio(args.getString(0), options);
		} else if (ACTION_PLAY_VIDEO.equals(action)) {
			return playVideo(args.getString(0), options);
		}
		else if (STOP_VIDEO.equals(action)) {
			intentFilter.addAction("finish_activity");
			Intent intent = new Intent("finish_activity");
			cordova.getActivity().sendBroadcast(intent);
			return stopVideo(args.getString(0), options);
		} else {
			callbackContext.error("streamingMedia." + action + " is not a supported method.");
			intentFilter.addAction("finish_activity");
			Intent intent = new Intent("finish_activity");
			cordova.getActivity().sendBroadcast(intent);
			return false;
		}
	}

	private boolean stopVideo(String url, JSONObject options) {
		Log.v("Stop Video", "Stop Video called");

		return true;//play(SimpleVideoStream.class, url, options);
	}
	private void updateMediaInfo(Intent mediaIntent) {
		sendUpdate(this.getMediaInfo(mediaIntent), true);
	}
	private JSONObject getMediaInfo(Intent mediaIntent) {
		JSONObject obj = new JSONObject();
		try {
			obj.put("action", mediaIntent.getStringExtra("action"));
			if (mediaIntent.hasExtra("pos")) {
				obj.put("pos", getTimeString(mediaIntent.getIntExtra("pos", -1)));
			}
			obj.put("isDone", false);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return obj;
	}
	private String getTimeString(int millis) {
		if (millis == -1)
			return "00:00:00";
		StringBuffer buf = new StringBuffer();

		int hours = (int) (millis / (1000 * 60 * 60));
		int minutes = (int) ((millis % (1000 * 60 * 60)) / (1000 * 60));
		int seconds = (int) (((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000);

		buf
				.append(String.format("%02d", hours))
				.append(":")
				.append(String.format("%02d", minutes))
				.append(":")
				.append(String.format("%02d", seconds));

		return buf.toString();
	}
	private void sendUpdate(JSONObject info, boolean keepCallback) {
		if (callbackContext != null) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, info);
			result.setKeepCallback(keepCallback);
			callbackContext.sendPluginResult(result);
		}
	}
	private boolean playAudio(String url, JSONObject options) {
		return play(SimpleAudioStream.class, url, options);
	}
	private boolean playVideo(String url, JSONObject options) {
		return play(SimpleVideoStream.class, url, options);
	}

	private boolean play(final Class activityClass, final String url, final JSONObject options) {
		final CordovaInterface cordovaObj = cordova;
		final CordovaPlugin plugin = this;

		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				final Intent streamIntent = new Intent(cordovaObj.getActivity().getApplicationContext(), activityClass);
				Bundle extras = new Bundle();
				extras.putString("mediaUrl", url);

				if (options != null) {
					Iterator<String> optKeys = options.keys();
					while (optKeys.hasNext()) {
						try {
							final String optKey = (String)optKeys.next();
							if (options.get(optKey).getClass().equals(String.class)) {
								extras.putString(optKey, (String)options.get(optKey));
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							} else if (options.get(optKey).getClass().equals(Boolean.class)) {
								extras.putBoolean("shouldAutoClose", true);
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							}

						} catch (JSONException e) {
							Log.e(TAG, "JSONException while trying to read options. Skipping option.");
						}
					}
					streamIntent.putExtras(extras);
				}

				cordovaObj.startActivityForResult(plugin, streamIntent, ACTIVITY_CODE_PLAY_MEDIA);
			}
		});
		return true;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.v(TAG, "onActivityResult: " + requestCode + " " + resultCode);
		super.onActivityResult(requestCode, resultCode, intent);
		if (ACTIVITY_CODE_PLAY_MEDIA == requestCode) {
			if (Activity.RESULT_OK == resultCode) {
				this.callbackContext.success();
			} else if (Activity.RESULT_CANCELED == resultCode) {
				String errMsg = "Error";
				if (intent != null && intent.hasExtra("message")) {
					errMsg = intent.getStringExtra("message");
				}
				this.callbackContext.error(errMsg);
			}
		}
	}
}
