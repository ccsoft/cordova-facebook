package com.ccsoft.plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.model.GraphObject;
import com.sromku.simple.fb.Permission;
import com.sromku.simple.fb.SimpleFacebook;
import com.sromku.simple.fb.SimpleFacebookConfiguration;
import com.sromku.simple.fb.entities.Feed;
import com.sromku.simple.fb.entities.Profile;
import com.sromku.simple.fb.entities.Score;
import com.sromku.simple.fb.entities.User;
import com.sromku.simple.fb.listeners.OnDeleteListener;
import com.sromku.simple.fb.listeners.OnInviteListener;
import com.sromku.simple.fb.listeners.OnLoginListener;
import com.sromku.simple.fb.listeners.OnLogoutListener;
import com.sromku.simple.fb.listeners.OnProfileListener;
import com.sromku.simple.fb.listeners.OnPublishListener;
import com.sromku.simple.fb.listeners.OnScoresListener;

import android.content.Intent;
import android.os.Bundle;
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
    		ArrayList<Permission> permsArr = new ArrayList<Permission>();
    		for(int i=0; i<ps.length(); i++){
    			Permission p = Permission.fromValue(ps.getString(i));
    			if(p != null){
	    			permsArr.add(p);
    			}
    		}
    		if(permsArr.isEmpty()){
    			permsArr.add(Permission.PUBLIC_PROFILE);
    		}
    		Permission[] perms = permsArr.toArray(new Permission[permsArr.size()]);
    		
    		facebookConfiguration = new SimpleFacebookConfiguration.Builder()
			    .setAppId(args.getString(0))
			    .setNamespace(args.getString(1))
			    .setPermissions(perms)
			    .setAskForAllPermissionsAtOnce(true)
			    .build();

			SimpleFacebook.setConfiguration(facebookConfiguration);
			
			Runnable runnable = new Runnable() {
    			public void run() {
    				SimpleFacebook simpleFB = SimpleFacebook.getInstance(cordova.getActivity());
    				if(simpleFB.isLogin()) {
    					JSONObject resp = prepareAccessTokenInfo(simpleFB.getSession());
    					callbackContext.success(resp);
    				} else {
    		    		Log.i(TAG, "no user logged in yet");
    					callbackContext.success("");
    				}
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
    		
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
        	final OnLoginListener onLoginListener = new OnLoginListener()
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
        	        callbackContext.error(throwable.getMessage());
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
        	        Log.i(TAG, "Logged in fb");
        	        JSONObject resp = prepareAccessTokenInfo(mSimpleFacebook.getSession());
    				callbackContext.success(resp);        	        
        	    }

        	    @Override
        	    public void onNotAcceptingPermissions(Permission.Type type)
        	    {
        	    	// user didn't accept READ or WRITE permission
        	    	String msg = String.format("User didn't accept %s permissions", type.name());
        	        Log.w(TAG, msg);
        	        callbackContext.error(msg);
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
        	final OnLogoutListener onLogoutListener = new OnLogoutListener()
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
        	        callbackContext.error(throwable.getMessage());
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
        	        callbackContext.success("");
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
        	final OnPublishListener onPublishListener = new OnPublishListener()
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
        	        callbackContext.error(throwable.getMessage());
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
    				mSimpleFacebook.publish(feed, true, onPublishListener);
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
        	
        	return true;
        }
		if (action.equals("invite")) {
			final String message = args.getString(0); 
        	final OnInviteListener onInviteListener = new OnInviteListener()
        	{

        	    @Override
        	    public void onFail(String reason)
        	    {
        	        // insure that you are logged in before inviting
        	        Log.w(TAG, reason);
        	        callbackContext.error(reason);
        	    }

        	    @Override
        	    public void onException(Throwable throwable)
        	    {
				    // user may have canceled, we end up here in that case as well!
        	        Log.e(TAG, "Bad thing happened", throwable);
        	        callbackContext.error(throwable.getMessage());
        	    }

        	    @Override
        	    public void onComplete(List<String> invitedFriends, String requestId)
        	    {
        	    	if(invitedFriends.isEmpty())
        	    	{
        	    		callbackContext.error("nobody invited");
        	    		return;
        	    	}
        	        
        	    	//Log.i(TAG, "Invitation was sent to " + invitedFriends.size() + " users with request id " + requestId);
        	    	JSONArray to = new JSONArray();
        	    	for (String item : invitedFriends) {
        	        	to.put(item);        	    		
        	    	}

        	        JSONObject r = new JSONObject();
        	        try {
    					r.put("to", to);
    					r.put("request", requestId);    					
    				} catch (JSONException e) {
    					Log.e(TAG, "Bad thing happened with invite json", e);
    					callbackContext.error("json exception");
    					return;
    				}
        	        callbackContext.success(r);
        	    }

        	    @Override
        	    public void onCancel()
        	    {
        	        Log.i(TAG, "Canceled the dialog");
        	        callbackContext.error("cancelled");
        	    }
        	};
        	
        	Runnable runnable = new Runnable() {
    			public void run() {
    				mSimpleFacebook.invite(message, onInviteListener, null);
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
        	
			return true;
        } 
		else if (action.equals("deleteRequest")) {

        	final String req = args.getString(0);
        	// logout listener
        	final OnDeleteListener onDeleteRequestListener = new OnDeleteListener()
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
        	    public void onComplete(Void response)
        	    {
        	        Log.i(TAG, "Deleted request " + req);
        	        callbackContext.success();
        	    }
        	};

        	Runnable runnable = new Runnable() {
    			public void run() {
    				mSimpleFacebook.deleteRequest(req, onDeleteRequestListener);
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
        	
			return true;
        } else if (action.equals("postScore")) {

        	final OnPublishListener onPostScoreListener = new OnPublishListener()
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
        	        //Log.i(TAG, "Published score successfully.");
        	        callbackContext.success();        	        
        	    }
        	};

        	final Score score = new Score.Builder()
	            .setScore(args.getInt(0))
	            .build();
        	Runnable runnable = new Runnable() {
    			public void run() {
    				mSimpleFacebook.publish(score, onPostScoreListener);
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
        	
        	return true;
        }
        else if (action.equals("getScores")) {
        	final OnScoresListener onScoresRequestListener = new OnScoresListener()
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
        	    public void onComplete(List<Score> scores)
        	    {
        	    	try {
	        	    	JSONArray jsonScores = new JSONArray();
	        	    	for (Score item : scores) {	        	    		
	        	    		JSONObject o = new JSONObject();
	        	    		JSONObject u = new JSONObject();
	        	    		User user = item.getUser();
	        	    		u.put("id", user.getId());
	        	    		u.put("name", user.getName());
							o.put("user", u);
							o.put("score", item.getScore());
							jsonScores.put(o);
	        	    	}
	
	        	        JSONObject r = new JSONObject();
	        	        r.put("data", jsonScores);    					
	    				callbackContext.success(r);
        	    	} catch (JSONException e) {
    					Log.e(TAG, "Bad thing happened with get scores json", e);
    					callbackContext.error("json exception");    					
    				}        	        
        	    }
        	};

        	Runnable runnable = new Runnable() {
    			public void run() {
    				mSimpleFacebook.getScores(onScoresRequestListener);
    			};
    		};
    		cordova.getActivity().runOnUiThread(runnable);
        	
			return true;
        } else if (action.equals("graphCall")) {
        	if(args.length() < 3) {
        		callbackContext.error("not enough params");
        		return true;
        	} 

       		String node = args.getString(0);
       		JSONObject fields = args.getJSONObject(1);
        	String methodString = args.getString(2);
        	
        	// decide on http method
        	HttpMethod method = HttpMethod.GET;
        	if(methodString != null) {
        		if(methodString.equals("POST")) method = HttpMethod.POST;
        		if(methodString.equals("DELETE")) method = HttpMethod.DELETE;
        	}
        	        	
        	// process params
        	final Bundle bundle = new Bundle();
        	if(fields != null) {
        		Iterator<?> keys = fields.keys();
                while( keys.hasNext() ){
                    String key = (String)keys.next();
                    bundle.putString(key, fields.getString(key));                   
                }        		
        	}
        	
        	Request request = new Request(Session.getActiveSession(), node, bundle, method, new Request.Callback() {
        		public void onCompleted(Response response) {
			        FacebookRequestError error = response.getError();
			        if(error != null) {
			        	Log.e("Error", error.getErrorMessage());
			        	callbackContext.error(error.getErrorMessage());
			        	return;
			        }
			        GraphObject graphObject = response.getGraphObject();        	                
			        callbackContext.success(graphObject.getInnerJSONObject());
    			}
			});

        	request.executeAsync();
        	return true;   
        }
        return false;
    }
    
    private JSONObject prepareAccessTokenInfo(Session session) {    	
    	JSONObject r = new JSONObject();
        try {
			r.put("accessToken", session.getAccessToken());
			r.put("expirationDate", session.getExpirationDate().getTime());
			JSONArray permissions  = new JSONArray();
			List<String> parr = session.getPermissions();
	    	for (String item : parr) {
	    		permissions.put(item);        	    		
	    	}
			r.put("permissions", permissions);
		} catch (JSONException e) {
			Log.e(TAG, "Exception when preparing access token json", e);			
			return null;
		}
        return r;
    }
    public void getUserInfo(final CallbackContext callbackContext) {
    	final SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova.getActivity());
    	OnProfileListener onProfileRequestListener = new OnProfileListener()
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
    	        callbackContext.error(throwable.getMessage());
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
    	    	callbackContext.success(profile.getGraphObject().getInnerJSONObject());
    	    }

    	};

        // do the get profile action
        mSimpleFacebook.getProfile(onProfileRequestListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	Log.i(TAG, "onActivityResult");
    	SimpleFacebook mSimpleFacebook = SimpleFacebook.getInstance(cordova.getActivity());
        mSimpleFacebook.onActivityResult(cordova.getActivity(), requestCode, resultCode, data); 
        super.onActivityResult(requestCode, resultCode, data);
    } 
}
