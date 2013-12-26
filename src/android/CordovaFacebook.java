package com.ccsoft.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import android.util.Log;

public class CordovaFacebook extends CordovaPlugin {
	
	private final String TAG = "CordovaFacebook";
    
    @Override
    public boolean execute(String action, JSONArray args,
			final CallbackContext callbackContext) throws JSONException {
    	Log.d(TAG, "action:" + action);
    	cordova.setActivityResultCallback(this);
    
    	if (action.equals("login")) {
    		callbackContext.success("login call ok");
        	return true;
        }
        else if (action.equals("logout")) {
        	callbackContext.success("logout call ok");
        	return true;
        }
        
        return false;
    }

}