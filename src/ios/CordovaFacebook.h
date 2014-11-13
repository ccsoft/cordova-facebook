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

#import <Cordova/CDVPlugin.h>
#import <FacebookSDK/FacebookSDK.h>

@interface CordovaFacebook : CDVPlugin

- (void)init:(CDVInvokedUrlCommand*)command;
- (void)login:(CDVInvokedUrlCommand*)command;
- (void)logout:(CDVInvokedUrlCommand*)command;
- (void)info:(CDVInvokedUrlCommand*)command;
- (void)share:(CDVInvokedUrlCommand*)command;
- (void)feed:(CDVInvokedUrlCommand*)command;
- (void)invite:(CDVInvokedUrlCommand*)command;
- (void)deleteRequest:(CDVInvokedUrlCommand*)command;
- (void)postScore:(CDVInvokedUrlCommand*)command;
- (void)getScores:(CDVInvokedUrlCommand*)command;
- (void)graphCall:(CDVInvokedUrlCommand*)command;

+ (void)sessionStateChanged:(FBSession *)session state:(FBSessionState) state error:(NSError *)error;
+ (BOOL)isReadPermission: (NSString*) permission;
+ (BOOL)activeSessionHasPermissions:(NSArray *)permissions;

+ (NSMutableArray*) readPermissions;
//+ (void)setReadPermissions:(NSMutableArray *)perms;

+ (NSMutableArray*) publishPermissions;
//+ (void)setPublishPermissions:(NSMutableArray *)perms;

+ (NSString*) loginCallbackId;
+ (void)setLoginCallbackId:(NSString *)cb;

+ (id <CDVCommandDelegate>) commandDelegate;
+ (void)setCommandDelegate:(id <CDVCommandDelegate>)del;

@end
