package com.ccsoft.plugin;

import java.util.ArrayList;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sromku.simple.fb.Permissions;
import com.sromku.simple.fb.Properties;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebook.OnLogoutListener;
import com.sromku.simple.fb.SimpleFacebook.OnProfileRequestListener;
import com.sromku.simple.fb.SimpleFacebook.OnPublishListener;
import com.sromku.simple.fb.SimpleFacebookConfiguration;
import com.sromku.simple.fb.SimpleFacebook.OnLoginListener;
import com.sromku.simple.fb.entities.Feed;
import com.sromku.simple.fb.entities.Profile;

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
			
			SimpleFacebook simpleFB = SimpleFacebook.getInstance(cordova.getActivity());
			if(simpleFB.isLogin()) {
				callbackContext.success(simpleFB.getAccessToken());
			} else {
				callbackContext.success("");
			}
			return true;
    	}
    	
    	if(facebookConfiguration == null) {
    		Log.e(TAG, "init was not called");
    		callbackContext.error("init plugin first");
    		return true;
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
        	        callbackContext.success(mSimpleFacebook.getAccessToken());
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
    				mSimpleFacebook.login(onLoginListener);
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
        	return true;
        }
        if (action.equals("logout")) {
        	// logout listener
        	final OnLogoutListener onLogoutListener = new SimpleFacebook.OnLogoutListener()
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
        	    public void onLogout()
        	    {
        	        Log.i(TAG, "You are logged out");
        	        callbackContext.success();
        	    }
        	};

        	Runnable runnable = new Runnable() {
    			public void run() {
    				mSimpleFacebook.logout(onLogoutListener);
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
        	return true;
        }
        if (action.equals("info")) {
        	if(mSimpleFacebook.isLogin() == true) {
        		getUserInfo(callbackContext);
        	}
        	else {
        		callbackContext.error("not logged in"); 
        	}
			return true;
        }
        if (action.equals("feed") || action.equals("share")) {
        	// create publish listener
        	final OnPublishListener onPublishListener = new SimpleFacebook.OnPublishListener()
        	{
        	    @Override
        	    public void onFail(String reason)
        	    {
        	        // insure that you are logged in before publishing
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
        	        // show progress bar or something to the user while publishing
        	        Log.i(TAG, "In progress");
        	    }

        	    @Override
        	    public void onComplete(String postId)
        	    {
        	        Log.i(TAG, "Published successfully. The new post id = " + postId);
        	        JSONObject r = new JSONObject();
        	        try {
    					r.put("post_id", postId);    					
    				} catch (JSONException e) {
    					Log.e(TAG, "Bad thing happened with profile json", e);
    					callbackContext.error("json exception");
    					return;
    				}
        	        callbackContext.success(r);        	        
        	    }
        	};

        	// build feed
        	final Feed feed = new Feed.Builder()
        	    .setName(args.getString(0))
        	    .setLink(args.getString(1))
        	    .setPicture(args.getString(2))
        	    .setCaption(args.getString(3))
        	    .setDescription(args.getString(4))
        	    .build();

        	Runnable runnable = new Runnable() {
    			public void run() {
    				mSimpleFacebook.publish(feed, onPublishListener);
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
        	
        	return true;
        }
        
        return false;
    }
    
    public void getUserInfo(final CallbackContext callbackContext) {
    	final SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova.getActivity());
    	OnProfileRequestListener onProfileRequestListener = new SimpleFacebook.OnProfileRequestListener()
    	{
    	    @Override
    	    public void onFail(String reason)
    	    {
    	        // insure that you are logged in before getting the profile
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
    	        // show progress bar or something to the user while fetching profile
    	        Log.i(TAG, "Thinking...");
    	    }

    	    @Override
    	    public void onComplete(Profile profile)
    	    {
    	        JSONObject r = new JSONObject();
    	        try {
					r.put("id", profile.getId());
					r.put("name", profile.getName());
					r.put("email", profile.getEmail());
					r.put("first_name", profile.getFirstName());
					r.put("last_name", profile.getLastName());
					r.put("link", profile.getLink());
					r.put("locale", profile.getLocale());
					Log.i(TAG, profile.getId() + " " + profile.getName());					
				} catch (JSONException e) {
					Log.e(TAG, "Bad thing happened with profile json", e);
					callbackContext.error("json exception");
					return;
				}
    	        callbackContext.success(r);
    	    }

    	};
    	
    	// prepare the properties that you need
        Properties properties = new Properties.Builder()
            .add(Properties.ID)
            .add(Properties.NAME)
            .build();

        // do the get profile action
        mSimpleFacebook.getProfile(properties, onProfileRequestListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.i(TAG, "onActivityResult");
    	SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova.getActivity());
        mSimpleFacebook.onActivityResult(cordova.getActivity(), requestCode, resultCode, data); 
        super.onActivityResult(requestCode, resultCode, data);
    } 
}