package com.ccsoft.plugin;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import com.sromku.simple.fb.Permissions;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;
import com.sromku.simple.fb.SimpleFacebook.OnLoginListener;

import android.content.Intent;
import android.util.Log;

public class CordovaFacebook extends CordovaPlugin {
	
	private final String TAG = "CordovaFacebook";
    
	Permissions[] appPermissions = new Permissions[]
			{
				Permissions.BASIC_INFO
			};
	private final String appId = "YOUR_APP_ID";
	private final String appNamespace = "YOUR_APP_NAMESPACE";
	private SimpleFacebookConfiguration facebookConfiguration = null;
    @Override
    public boolean execute(String action, JSONArray args,
			final CallbackContext callbackContext) throws JSONException {
    	Log.d(TAG, "action:" + action);
    	cordova.setActivityResultCallback(this);
    
    	if(facebookConfiguration == null) {
    		facebookConfiguration = new SimpleFacebookConfiguration.Builder()
			    .setAppId(appId)
			    .setNamespace(appNamespace)
			    .setPermissions(appPermissions)
			    .build();

			SimpleFacebook.setConfiguration(facebookConfiguration);			
    	}
    	
    	final SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova.getActivity());
    	
    	if (action.equals("login")) {
    		// login listener
        	final OnLoginListener onLoginListener = new SimpleFacebook.OnLoginListener()
        	{
        	    @Override
        	    public void onFail(String reason)
        	    {
        	        Log.w(TAG, reason);
        	        callbackContext.error(reason);
        	    } 

        	    @Override
        	    public void onException(Throwable throwable)
        	    {
        	        Log.e(TAG, "Bad thing happened", throwable);
        	        callbackContext.error("exception");
        	    }

        	    @Override
        	    public void onThinking()
        	    {
        	        // show progress bar or something to the user while login is happening
        	        Log.i(TAG, "In progress");
        	    }

        	    @Override
        	    public void onLogin()
        	    {
        	        // change the state of the button or do whatever you want
        	        Log.i(TAG, "Logged in");
        	        //getUserInfo(callbackContext);
        	    }

        	    @Override
        	    public void onNotAcceptingPermissions()
        	    {
        	        Log.w(TAG, "User didn't accept read permissions");
        	        callbackContext.error("permission not accepted");
        	    }

        	};

        	Runnable runnable = new Runnable() {
    			public void run() {
    				Log.d(TAG, "mSimpleFacebook.login call");
    				mSimpleFacebook.login(onLoginListener);
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
        	return true;
        }
        else if (action.equals("logout")) {
        	callbackContext.success("logout call ok");
        	return true;
        }
        
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.w(TAG, "onActivityResult");
    	SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova.getActivity());
        mSimpleFacebook.onActivityResult(cordova.getActivity(), requestCode, resultCode, data); 
        super.onActivityResult(requestCode, resultCode, data);
    } 
}