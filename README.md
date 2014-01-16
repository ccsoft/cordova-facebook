cordova-facebook
================

[Cordova](http://cordova.apache.org/) plugin that handles Facebook integration for mobile (iOS and Android) apps.

Project uses mobile native platform FacebookSDK for iOS and Android to utilize basic operations for a mobile app that uses Cordova. 

We also provide [TypeScript](http://www.typescriptlang.org/) source file together with the JavaScript for the client side with this plugin.

##Why?
- Why do we implement another plugin since there is already an official [phonegap-facebook-plugin](https://github.com/phonegap/phonegap-facebook-plugin)?
	1. As of today (16.01.2014), [official cordova facebook plugin](https://github.com/phonegap/phonegap-facebook-plugin) project on GitHub has 985 stars (including mine), 118 watchers, 218 open issues, 29 pull requests and 5 branches.
	2. Last commit as of today to master branch was 3 months ago, we don't have time to wait for fixes and new updates.
	3. Official plugin tries to retain the same interface for the Facebook JavaScript SDK, which we believe an unnecessary burden. 
	4. We have some live apps that uses the official plugin, and we are scared to update our app to new Cordova version, scared to break things in Facebook side. 
	5. Well, it was not that hard to do it, so we did it.

##Versions
We support only Cordova version > 3.0

We currently tested FacebookSDK for following platforms and versions:

- [FacebookSDK iOS 3.11](https://developers.facebook.com/docs/ios/)

- [FacebookSDK Android 3.6](https://developers.facebook.com/docs/android/)

##Prerequisites

###iOS
Download the latest [FacebookSDK](https://developers.facebook.com/docs/ios/), and follow the [getting started guideline](https://developers.facebook.com/docs/ios/getting-started/). 

The guideline is well documented and people at Facebook may change stuff in the future, so we stick to that instead of fancy cordova plugin hacks (well, cordova people also modify plugin flow too).

###Android

Unlike iOS, Android [getting started guideline](https://developers.facebook.com/docs/android/getting-started/) is pretty long and scary. 
For Android we rely on [Android Simple Facebook](https://github.com/sromku/android-simple-facebook) by [Roman Kushnarenko](https://github.com/sromku), many thanks for that project. 
We are using our own [fork](https://github.com/koraybalci/android-simple-facebook) (but it's in sync for now) for this plugin. We distribute the compiled version of the library with the plugin, so you don't have to worry about anything. 

Here is what to do for Android before installing our plugin.

1. Clone [Facebook SDK 3.6](https://github.com/facebook/facebook-android-sdk) or [download](https://developers.facebook.com/android/) it. Then, import the project to your workspace.

2. Add reference from your project to `FacebookSDK` project.

    ![Screenshot](https://raw.github.com/sromku/android-simple-facebook/master/Refs/reference_to_sdk.png)

##Installing the plugin
To add this plugin just type:
```cordova plugin add https://github.com/ccsoft/cordova-facebook.git```

To remove this plugin type:
```cordova plugin remove com.ccsoft.plugin.CordovaFacebook```

##Usage
		
	// Get a reference to the plugin first
    var plugin = new CC.CordovaFacebook();

The plugin has the following methods:

* [init](#init)
* [login](#login)
* [logout](#logout)
* [info](#info)
* [share](#share)
* [feed](#feed)

*** 

###init
Initializes the plugin. Must be called before calling any other function.

>####parameters

>> *appId*: string: Your FB app id.

>> *appName*: string: Your FB app name.

>> *appPermissions*: array: Your FB app permissions as an array of strings. 

>> *successCallback*: function: Called with an fb access_token if already logged in to FB.

>> *failureCallback*: function: Called with failure reason string.
         
>####example

	plugin.init('YOUR_FB_APP_ID', 'YOUR_FB_APP_NAME', 
		['basic_info', 'email', 'publish_actions'], 
		successCallback, failureCallback);

***

###login

>####parameters
	
>>*successCallback*: function: Called with an fb access_token if login successful.
         
>>*failureCallback*: function: Called with failure reason string.
        
>####example

	plugin.login(function(token) {
		console.log("Access token is: " + token);
	}), failureCallback);

***

###logout

>####parameters
	
>>*successCallback*: function: Called with no params.

>####example

	plugin.logout(successCallback);

***

###info
Retrieves user info.
See [FBGraphUser](https://developers.facebook.com/docs/reference/ios/current/protocol/FBGraphUser/) documentation for successCallback parameter in iOS. 
See the example below for Android. (They must be equiavelent, let us know if there are differences.)

>####parameters
	
>>*successCallback*: function: Called with user info data

>>*failureCallback*: function: Called with failure reason string.

>####example
         
	plugin.info(function(data) {
		console.log("User Id: "		+ data.id);
		console.log("Name: "			+ data.name);
		console.log("Email: "		+ data.email); // if asked for it in permissions
		console.log("First Name: "	+ data.first_name);
		console.log("Last Name: "	+ data.last_name);
		console.log("Link: "			+ data.link);
		console.log("Locale: "		+ data.locale);
	}, 
	function(err) {console.log(err););

***

###share
* iOS: share call tries to open share dialog via official Facebook app. If Facebook app is not installed on device, we fallback to [feed](#feed) call.
* Android: share behaves exactly the same as [feed](#feed).

>####parameters
		
>>*name*: string

>>*url*: string

>>*logoUrl*: string

>>*caption*: string

>>*description*: string

>>*successCallback*: function: post_id (on iOS, if Facebook app is installed and used for share, pass no parameters to callback on success)

>>*failureCallback*: function: Called with failure reason string.

>####example

	plugin.share('Name', 'http://www.example.com', 'http://www.example.com/test.png', 
		'Test caption', 'Test description.', successCallback, failureCallback);

***

###feed
feed call requires an active session. Shows facebook web dialog as a popup on iOS and uses open graph on Android. On Android, we will support dialog whem *Simple Facebook* library supports it.

>####parameters
		
>>*name*: string

>>*url*: string

>>*logoUrl*: string

>>*caption*: string

>>*description*: string

>>*successCallback*: function: post_id (on iOS, if Facebook app is installed and used for share, pass no parameters to callback on success)

>>*failureCallback*: function: Called with failure reason string.

>####example

	plugin.feed('Name', 'http://www.example.com', 'http://www.example.com/test.png', 
		'Test caption', 'Test description.', successCallback, failureCallback);

***

##Sample App
We have a sample cordova app to test the plugin that you can find [here](https://github.com/ccsoft/cordova-sample/tree/facebook). Please note that the link takes you to a dedicated branch named facebook, please use that branch to test this plugin. We use separate branches for each plugin we implement.

Once you download/clone and run the app, you are going to be using a sample Facebook app in sandbox. 
You can change your app settings (in index.html), you can also test the features with the following Facebook tester user credentials:
> User: joe_kxpligh_tester@tfbnw.net

> Pass: 123456



##License
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html)

