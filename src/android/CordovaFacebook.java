package com.ccsoft.plugin;

import java.util.ArrayList;

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
    
	private SimpleFacebookConfiguration facebookConfiguration = null;
	
    @Override
    public boolean execute(String action, JSONArray args,
			final CallbackContext callbackContext) throws JSONException {
    	Log.d(TAG, "action:" + action);
    	cordova.setActivityResultCallback(this);
    	
    	if (action.equals("init")) {
    		JSONArray ps = args.getJSONArray(2);
    		ArrayList<Permissions> permsArr = new ArrayList<Permissions>();
    		for(int i=0; i<ps.length(); i++){
    			Permissions p = Permissions.findPermission(ps.getString(i));
    			if(p != null){
	    			permsArr.add(p);
    			}
    		}
    		if(permsArr.isEmpty()){
    			permsArr.add(Permissions.BASIC_INFO);
    		}
    		Permissions[] perms = permsArr.toArray(new Permissions[permsArr.size()]);
    		
    		facebookConfiguration = new SimpleFacebookConfiguration.Builder()
			    .setAppId(args.getString(0))
			    .setNamespace(args.getString(1))
			    .setPermissions(perms)
			    .build();

			SimpleFacebook.setConfiguration(facebookConfiguration);
			return true;
    	}
    	
    	if(facebookConfiguration == null) {
    		Log.e(TAG, "init was not called");
    		callbackContext.error("init plugin first");
    		return true;
    	}
    	
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
        	        callbackContext.success("login ok");
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
    				SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova.getActivity());
    				mSimpleFacebook.login(onLoginListener);
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
        	return true;
        }
        if (action.equals("logout")) {
        	callbackContext.success("logout call echo");
        	return true;
        }
        
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.i(TAG, "onActivityResult");
    	SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova.getActivity());
        mSimpleFacebook.onActivityResult(cordova.getActivity(), requestCode, resultCode, data); 
        super.onActivityResult(requestCode, resultCode, data);
    } 
}