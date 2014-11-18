/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

#import "CordovaFacebook.h"
#import <Cordova/CDV.h>
#import <UIKit/UIKit.h>

@implementation CordovaFacebook


static id <CDVCommandDelegate> commandDelegate = nil;
+ (id <CDVCommandDelegate>) commandDelegate {return commandDelegate;}
+ (void)setCommandDelegate:(id <CDVCommandDelegate>)del {commandDelegate = del;}


static NSString* loginCallbackId = nil;
+ (NSString*) loginCallbackId {return loginCallbackId;}
+ (void)setLoginCallbackId:(NSString *)cb {loginCallbackId = cb;}

static NSString* appId = nil;
+ (NSString*) appId {return appId;}
//+ (void)setAppId:(NSString *)aid {appId = aid;}

static NSMutableArray *readPermissions;
+ (NSMutableArray *)readPermissions { return readPermissions; }
//+ (void)setReadPermissions:(NSMutableArray *)param { readPermissions = param; }

static NSMutableArray *publishPermissions;
+ (NSMutableArray *)publishPermissions { return publishPermissions; }
//+ (void)setPublishPermissions:(NSMutableArray *)param { publishPermissions = param; }

+(void)load {
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(notifiedOpenUrl:)
                                                 name:@"CordovaPluginOpenURLNotification"
                                               object:nil];
    
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(notifiedApplicationDidBecomeActive:)
                                                 name:UIApplicationDidBecomeActiveNotification
                                               object:nil];
}

+(void)notifiedOpenUrl:(NSNotification*)notification {
    NSDictionary* params = notification.userInfo;
    if (params == nil) {
        return;
    }
    if (appId == nil) {
        return;
    }
    
    NSURL *url = [params objectForKey:@"url"];
    NSString *scheme = @"fb";
    if ([[url scheme] isEqualToString:[scheme stringByAppendingString:appId]] == FALSE) {
        return;
    }
    if([CordovaFacebook loginCallbackId] == nil || [CordovaFacebook commandDelegate] == nil) { // nowhere to call back
        return;
    }
    
    // Note this handler block should be the exact same as the handler passed to any open calls.
    [FBSession.activeSession setStateChangeHandler:
     ^(FBSession *session, FBSessionState state, NSError *error) {
         // Call sessionStateChanged:state:error method to handle session state changes
         [CordovaFacebook sessionStateChanged:session state:state error:error];
     }];
  
    NSString *sourceApplication = [params objectForKey:@"sourceApplication"];
    BOOL success = [FBAppCall handleOpenURL:url sourceApplication:sourceApplication];
    if(success) {
        [params setValue:@"facebook" forKey:@"success"];
    }
}

+(void)notifiedApplicationDidBecomeActive:(NSNotification*)notification {
    // Handle the user leaving the app while the Facebook login dialog is being shown
    // For example: when the user presses the iOS "home" button while the login dialog is active
    [FBAppCall handleDidBecomeActive];
}

+ (BOOL)activeSessionHasPermissions:(NSArray *)permissions
{
    __block BOOL hasPermissions = YES;
    for (NSString *permission in permissions)
    {
        NSInteger index = [[FBSession activeSession].permissions indexOfObjectPassingTest:^BOOL(id obj, NSUInteger idx, BOOL *stop) {
            if ([obj isEqualToString:permission])
            {
                *stop = YES;
            }
            return *stop;
        }];
        
        if (index == NSNotFound)
        {
            hasPermissions = NO;
        }
    }
    return hasPermissions;
}

+ (void) reportLogin
{
    if([CordovaFacebook loginCallbackId] != nil) {
        NSMutableDictionary* dict = [[NSMutableDictionary alloc] init];
        FBAccessTokenData* token = [FBSession.activeSession accessTokenData];
        dict[@"accessToken"] = [token accessToken];
        long long timestamp = (long long)[[token expirationDate] timeIntervalSince1970]*1000.0;
        dict[@"expirationDate"] = [[NSNumber numberWithLongLong:timestamp] stringValue];
        dict[@"permissions"] = [[FBSession.activeSession accessTokenData] permissions];
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: dict];
        [[CordovaFacebook commandDelegate] sendPluginResult:pluginResult callbackId:[CordovaFacebook loginCallbackId]];
    }
    else {
        NSLog(@"noone to callback");
    }
}

// This method will handle ALL the session state changes in the app
+ (void)sessionStateChanged:(FBSession *)session state:(FBSessionState) state error:(NSError *)error
{
    // If the session was opened successfully
    if (!error && state == FBSessionStateOpen){
        NSLog(@"Session opened");
        
        // if need publish permissions
        if(publishPermissions.count > 0 && [CordovaFacebook activeSessionHasPermissions:publishPermissions] == NO) {
            [FBSession.activeSession requestNewPublishPermissions:publishPermissions
                                                  defaultAudience:FBSessionDefaultAudienceEveryone
                                                completionHandler:^(FBSession *session, NSError *error) {
                                                    if(error != nil) {
                                                        NSLog(@"Request publish err:%@", error);
                                                        [CordovaFacebook reportLogin];
                                                        return;
                                                    }
                                                    else if ([CordovaFacebook activeSessionHasPermissions:publishPermissions] == NO) {
                                                        NSLog(@"Request publish failed");
                                                        [CordovaFacebook reportLogin];
                                                        return;
                                                    }
                                                    NSLog(@"Request publish granted for: %@", publishPermissions);
                                                    [CordovaFacebook reportLogin];
                                                }];
        } else {
            [CordovaFacebook reportLogin];
        }
      
        return;
    }
    if (state == FBSessionStateClosed || state == FBSessionStateClosedLoginFailed){
        // If the session is closed
        NSLog(@"Session closed");
        if([CordovaFacebook loginCallbackId] != nil) {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Login failed or closed"];
            [[CordovaFacebook commandDelegate] sendPluginResult:pluginResult callbackId:[CordovaFacebook loginCallbackId]];
        }
    }

    // Handle errors
    if (error){
        NSLog(@"Error");
        NSString *errorText;
        // If the error requires people using an app to make an action outside of the app in order to recover
        if ([FBErrorUtility shouldNotifyUserForError:error] == YES){
            errorText = [FBErrorUtility userMessageForError:error];
        } else {

            // If the user cancelled login, do nothing
            if ([FBErrorUtility errorCategoryForError:error] == FBErrorCategoryUserCancelled) {
                errorText = @"User cancelled login";
                
                // Handle session closures that happen outside of the app
            } else if ([FBErrorUtility errorCategoryForError:error] == FBErrorCategoryAuthenticationReopenSession){
                errorText = @"Your current session is no longer valid. Please log in again.";
                
                // For simplicity, here we just show a generic message for all other errors
                // You can learn how to handle other errors using our guide: https://developers.facebook.com/docs/ios/errors
            } else {
                //Get more error information from the error
                NSDictionary *errorInformation = [[[error.userInfo objectForKey:@"com.facebook.sdk:ParsedJSONResponseKey"] objectForKey:@"body"] objectForKey:@"error"];
                
                errorText = [NSString stringWithFormat:@"Please retry. \n\n If the problem persists contact us and mention this error code: %@", [errorInformation objectForKey:@"message"]];
            }
        }
        
        NSLog(@"%@", errorText);
        if([CordovaFacebook loginCallbackId] != nil) {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:errorText];
            [[CordovaFacebook commandDelegate] sendPluginResult:pluginResult callbackId:[CordovaFacebook loginCallbackId]];
        }
        
        // Clear this token
        [FBSession.activeSession closeAndClearTokenInformation];
    }
}

- (void)init:(CDVInvokedUrlCommand*)command
{
    NSLog(@"FB SDK: %@", [FBSettings sdkVersion]);
    if([command.arguments count] > 0 && [command.arguments objectAtIndex:0] != (id)[NSNull null]) {
        appId = [command.arguments objectAtIndex:0];
    } else {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no appId sent to init"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    [CordovaFacebook setLoginCallbackId:command.callbackId];
    [CordovaFacebook setCommandDelegate:self.commandDelegate];
    
    [self.commandDelegate runInBackground:^{
        NSArray* appPermissions = [command.arguments objectAtIndex:2];
        readPermissions = [[NSMutableArray alloc] init];
        publishPermissions = [[NSMutableArray alloc] init];
        for (NSString* perm in appPermissions) {
            if([CordovaFacebook isReadPermission:perm]) {
                [readPermissions addObject:perm];
            } else {
                [publishPermissions addObject:perm];
            }
        }
        
        BOOL effectivelyLoggedIn;
        switch (FBSession.activeSession.state) {
            case FBSessionStateOpen:
            case FBSessionStateCreatedTokenLoaded:
            case FBSessionStateOpenTokenExtended:
                effectivelyLoggedIn = YES;
                break;
            default:
                effectivelyLoggedIn = NO;
                break;
        }
        
        if(effectivelyLoggedIn) {
            // Whenever a person inits, check for a cached session
            if (FBSession.activeSession.state == FBSessionStateCreatedTokenLoaded) {
                // If there's one, just open the session silently, without showing the user the login UI
                [FBSession openActiveSessionWithReadPermissions:readPermissions
                                               allowLoginUI:NO
                                          completionHandler:^(FBSession *session, FBSessionState state, NSError *error) {
                                              // Handler for session state changes
                                              // This method will be called EACH time the session state changes,
                                              // also for intermediate states and NOT just when the session open
                                              [CordovaFacebook sessionStateChanged:session state:state error:error];
                                          }];
            } else {
                [CordovaFacebook reportLogin];
            }
        } else {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@""];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }];
}

- (void)login:(CDVInvokedUrlCommand*)command
{
    [CordovaFacebook setLoginCallbackId:nil];
    if([FBSession.activeSession isOpen]){
        //NSLog(@"already logged in");
        [CordovaFacebook setLoginCallbackId:command.callbackId];
        [CordovaFacebook reportLogin];
        return;
    }
    if(readPermissions == nil) {
        NSLog(@"init with some permissions first");
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no read permissions"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    [self.commandDelegate runInBackground:^{
        [CordovaFacebook setLoginCallbackId:command.callbackId];
        // Open a session showing the user the login UI
        // You must ALWAYS ask for public_profile permissions when opening a session
        [FBSession openActiveSessionWithReadPermissions:readPermissions
                                           allowLoginUI:YES
                                      completionHandler:
         ^(FBSession *session, FBSessionState state, NSError *error) {
             // Call the app delegate's sessionStateChanged:state:error method to handle session state changes
             [CordovaFacebook sessionStateChanged:session state:state error:error];
         }];
    }];
}

- (void)logout:(CDVInvokedUrlCommand*)command
{
    [self.commandDelegate runInBackground:^{
        // If the session state is any of the two "open" states when the button is clicked
        if (FBSession.activeSession.state == FBSessionStateOpen
            || FBSession.activeSession.state == FBSessionStateOpenTokenExtended) {
            
            // Close the session and remove the access token from the cache
            // The session state handler (in the app delegate) will be called automatically
            [FBSession.activeSession closeAndClearTokenInformation];
        }
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@""];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }];
}

- (void)info:(CDVInvokedUrlCommand*)command
{
    if([FBSession.activeSession isOpen] == NO) { // not have a session to post
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no active session"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    [[FBRequest requestForMe] startWithCompletionHandler:
     ^(FBRequestConnection *connection, NSDictionary<FBGraphUser> *info, NSError *error) {
         if (!error) {
             CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:info];
             [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
         }
         else {
             CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"failed to get info"];
             [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
         }
     }];
}

- (void)share:(CDVInvokedUrlCommand*)command
{
    FBLinkShareParams *params = [[FBLinkShareParams alloc] init];
    params.name = [command.arguments objectAtIndex:0];
    params.link = [NSURL URLWithString:[command.arguments objectAtIndex:1]];
    params.picture = [NSURL URLWithString:[command.arguments objectAtIndex:2]];
    params.caption = [command.arguments objectAtIndex:3];
    params.linkDescription = [command.arguments objectAtIndex:4];
    BOOL canShare = [FBDialogs canPresentShareDialogWithParams:params];
    if (canShare) {
        // FBDialogs call to open Share dialog
        [FBDialogs presentShareDialogWithParams:params
                                    clientState:nil
                                        handler:^(FBAppCall *call, NSDictionary *results, NSError *error) {
                                            CDVPluginResult* pluginResult = nil;
                                            if(error) {
                                                NSLog(@"Error sharing: %@", error.description);
                                                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error.description];
                                            } else {
                                                // Check if cancel info is returned and log the event
                                                if (results[@"completionGesture"] &&
                                                    [results[@"completionGesture"] isEqualToString:@"cancel"]) {
                                                    NSLog(@"User canceled story publishing.");
                                                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"user cancelled"];
                                                } else {
                                                    NSLog(@"Share Success: %@", results);
                                                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@""];
                                                }
                                            }
                                            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                                        }];
    } else {
        // falback to feed dialog (user does not have FB app installed)
        [self feed:command];
    }
    
/*    // if need publish permissions
    if(publishPermissions.count > 0 && [CordovaFacebook activeSessionHasPermissions:publishPermissions] == NO) {
            [FBSession.activeSession requestNewPublishPermissions:publishPermissions
                              defaultAudience:FBSessionDefaultAudienceEveryone
                            completionHandler:^(FBSession *session, NSError *error) {
                                if(error != nil) {
                                    NSLog(@"Request publish err:%@", error);
                                    return;
                                }
                                else if ([CordovaFacebook activeSessionHasPermissions:publishPermissions] == NO) {
                                    NSLog(@"Request publish failed");
                                    return;
                                }
                                NSLog(@"Request publish granted for: %@", publishPermissions);
                                // do feed post now
                                [self post:command];
                            }];
    }
    else {
        // do feed post now
        [self post:command];
    }
*/
}

/**
 * A function for parsing URL parameters.
 */
- (NSDictionary*)parseURLParams:(NSString *)query {
    NSArray *pairs = [query componentsSeparatedByString:@"&"];
    NSMutableDictionary *params = [[NSMutableDictionary alloc] init];
    for (NSString *pair in pairs) {
        NSArray *kv = [pair componentsSeparatedByString:@"="];
        NSString *val =
        [kv[1] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        NSString *key =
        [kv[0] stringByReplacingPercentEscapesUsingEncoding:NSUTF8StringEncoding];
        params[key] = val;
    }
    return params;
}

- (void)feed:(CDVInvokedUrlCommand*)command
{
    if([FBSession.activeSession isOpen] == NO) { // not have a session to post
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no active session"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    NSMutableDictionary *params =
    [NSMutableDictionary dictionaryWithObjectsAndKeys:
     [command.arguments objectAtIndex:0], @"name",
     [command.arguments objectAtIndex:3], @"caption",
     [command.arguments objectAtIndex:4], @"description",
     [command.arguments objectAtIndex:1], @"link",
     [command.arguments objectAtIndex:2], @"picture",
     nil];
    // Invoke the dialog
    [FBWebDialogs presentFeedDialogModallyWithSession:nil
                                           parameters:params
                                              handler:
     ^(FBWebDialogResult result, NSURL *resultURL, NSError *error) {
         CDVPluginResult* pluginResult = nil;
         if (error) {
             // Error launching the dialog or publishing a story.
             NSLog(@"Error publishing story.");
             pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"feed error"];
         } else {
             if (result == FBWebDialogResultDialogNotCompleted) {
                 // User clicked the "x" icon
                 NSLog(@"User canceled story publishing.");
                 pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"feed cancelled by user"];
             } else {
                 // Handle the publish feed callback
                 NSDictionary *urlParams = [self parseURLParams:[resultURL query]];
                 if (![urlParams valueForKey:@"post_id"]) {
                     // User clicked the Cancel button
                     NSLog(@"User canceled story publishing.");
                     pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"feed cancelled by user"];
                 } else {
                     // User clicked the Share button
                     NSLog(@"Posted feed: %@", urlParams);
                     pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:urlParams];
                 }
             }
         }
         [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
     }];
}

- (void)invite:(CDVInvokedUrlCommand*)command
{
    if([FBSession.activeSession isOpen] == NO) { // not have a session to post
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no active session"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    NSString *message = @"";
    if([command.arguments count] > 0 && [command.arguments objectAtIndex:0] != (id)[NSNull null]) {
        message = [command.arguments objectAtIndex:0];
    }
    NSString *title = @"";
    if([command.arguments count] > 1 && [command.arguments objectAtIndex:1] != (id)[NSNull null]) {
        title = [command.arguments objectAtIndex:1];
    }
    NSMutableDictionary* params =   [NSMutableDictionary dictionaryWithObjectsAndKeys: nil];
    [FBWebDialogs presentRequestsDialogModallyWithSession:nil
                                                  message:message
                                                    title:title
                                               parameters:params
                                                  handler:^(FBWebDialogResult result, NSURL *resultURL, NSError *error) {
                                                      CDVPluginResult* pluginResult = nil;
                                                      if (error) {
                                                          // Case A: Error launching the dialog or sending request.
                                                          pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"error sending request"];
                                                      } else {
                                                          if (result == FBWebDialogResultDialogNotCompleted) {
                                                              // Case B: User clicked the "x" icon
                                                              pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"user canceled request"];
                                                          } else {
                                                              NSDictionary *urlParams = [self parseURLParams:[resultURL query]];
                                                              pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:urlParams];
                                                          }
                                                      }
                                                      [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                                                  }
                                              friendCache:nil];
}

- (void)deleteRequest:(CDVInvokedUrlCommand*)command
{
    if([FBSession.activeSession isOpen] == NO) { // not have a session to post
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no active session"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    if([command.arguments count] <= 0 || [command.arguments objectAtIndex:0] == (id)[NSNull null]) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no request param sent to delete"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    NSString *request = [NSString stringWithFormat:@"/%@", [command.arguments objectAtIndex:0]];
    [FBRequestConnection startWithGraphPath: request
                                 parameters: nil
                                 HTTPMethod: @"DELETE"
                          completionHandler: ^(FBRequestConnection *connection, id result, NSError *error) {
                              if (!error) {
                                  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@""];
                                  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                              }
                              else {
                                  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"failed to delete request"];
                                  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                              }
                          }];
}

- (void)postScore:(CDVInvokedUrlCommand*)command
{
    if([FBSession.activeSession isOpen] == NO) { // not have a session to post
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no active session"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    if([command.arguments count] <= 0 || [command.arguments objectAtIndex:0] == (id)[NSNull null]) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no score param sent"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    NSMutableDictionary *score = [NSMutableDictionary dictionaryWithObjectsAndKeys: [NSString stringWithFormat:@"%@", [command.arguments objectAtIndex:0]], @"score", nil];
    
    
    [FBRequestConnection startWithGraphPath:@"/me/scores"
                                 parameters: score
                                 HTTPMethod: @"POST"
                          completionHandler: ^(FBRequestConnection *connection, id result, NSError *error) {
                              if (!error) {
                                  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:@""];
                                  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                              }
                              else {
                                  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"failed to post score"];
                                  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                              }
                          }];
}

- (void)getScores:(CDVInvokedUrlCommand*)command
{
    if([FBSession.activeSession isOpen] == NO) { // not have a session to post
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no active session"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    [FBRequestConnection startWithGraphPath:[NSString stringWithFormat:@"/%@/scores", [CordovaFacebook appId]]
                                 parameters: nil
                                 HTTPMethod: @"GET"
                          completionHandler: ^(FBRequestConnection *connection, id result, NSError *error) {
                              if (!error) {
                                  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
                                  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                              }
                              else {
                                  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"failed to get scores"];
                                  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                              }
                          }];
}

- (void)graphCall:(CDVInvokedUrlCommand*)command
{
    if([FBSession.activeSession isOpen] == NO) { // not have a session to post
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no active session"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    if([command.arguments count] < 3) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"not enough params"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    
    if([command.arguments objectAtIndex:0] == (id)[NSNull null]){
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no graph path set"];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        return;
    }
    NSString* path = [command.arguments objectAtIndex:0];
    
    NSDictionary* params = nil;
    if([command.arguments objectAtIndex:1] != (id)[NSNull null]) {
        params = [command.arguments objectAtIndex:1];
    }
    
    NSString* method = nil;
    if([command.arguments objectAtIndex:2] != (id)[NSNull null]) {
        method = [command.arguments objectAtIndex:2];
    }
    
    [FBRequestConnection startWithGraphPath:path
                                 parameters:params
                                 HTTPMethod:method
                          completionHandler: ^(FBRequestConnection *connection, id result, NSError *error) {
                              if (!error) {
                                  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:result];
                                  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                              }
                              else {
                                  CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"failed to get graph result"];
                                  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                              }
                          }];
}

/*
 I hope someday Facebook SDK will have a better method for this.
 */
+ (BOOL)isReadPermission: (NSString*) permission
{
    if([permission isEqualToString:@"public_profile"]) return YES;
    if([permission isEqualToString:@"email"]) return YES;
    if([permission isEqualToString:@"user_friends"]) return YES;
    
    if([permission isEqualToString:@"user_about_me"]) return YES;
    if([permission isEqualToString:@"user_actions.books"]) return YES;
    if([permission isEqualToString:@"user_actions.fitness"]) return YES;
    if([permission isEqualToString:@"user_actions.music"]) return YES;
    if([permission isEqualToString:@"user_actions.news"]) return YES;
    if([permission isEqualToString:@"user_actions.video"]) return YES;
    if([permission isEqualToString:@"user_activities"]) return YES;
    if([permission isEqualToString:@"user_birthday"]) return YES;
    if([permission isEqualToString:@"user_education_history"]) return YES;
    if([permission isEqualToString:@"user_events"]) return YES;
    if([permission isEqualToString:@"user_games_activity"]) return YES;
    if([permission isEqualToString:@"user_groups"]) return YES;
    if([permission isEqualToString:@"user_hometown"]) return YES;
    if([permission isEqualToString:@"user_interests"]) return YES;
    if([permission isEqualToString:@"user_photos"]) return YES;
    if([permission isEqualToString:@"user_likes"]) return YES;
    if([permission isEqualToString:@"user_relationships"]) return YES;
    if([permission isEqualToString:@"user_relationship_details"]) return YES;
    if([permission isEqualToString:@"user_religion_politics"]) return YES;
    if([permission isEqualToString:@"user_status"]) return YES;
    if([permission isEqualToString:@"user_tagged_places"]) return YES;
    if([permission isEqualToString:@"user_videos"]) return YES;
    if([permission isEqualToString:@"user_website"]) return YES;
    if([permission isEqualToString:@"user_work_history"]) return YES;
    
    return NO;
}

@end
