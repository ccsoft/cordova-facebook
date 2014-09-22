/// <reference path='CordovaFacebook.d.ts' />

module CC {
    export class CordovaFacebook implements ICordovaFacebook {
        
        init(appId: string, appNamespace: string, appPermissions: string[], successcb?: (r: any) => void, failcb?: (err: any) => void) {
            if (!(<any>window).cordova) {
                if (failcb) failcb("no cordova");
                return;
            }
            (<any>window).cordova.exec(
                (response) => {
                    if (successcb) successcb(response);
                },
                (err) => {
                    console.log("init call failed with error: " + err);
                    if (failcb) failcb(err);
                }, "CordovaFacebook", "init", [appId, appNamespace, appPermissions]);
        }
        
        login(successcb?: (r: any) => void, failcb?: (err: any) => void) {
            if (!(<any>window).cordova) {
                if (failcb) failcb("no cordova");
                return;
            }            
            (<any>window).cordova.exec(
                (response) => {
                    if (successcb) successcb(response);
                },
                (err) => {
                    console.log("login call failed with error: " + err);
                    if (failcb) failcb(err);
                }, "CordovaFacebook", "login", []);
        }

        logout(successcb?: (r: any) => void) {
            if (!(<any>window).cordova) {
                return;
            }            
            (<any>window).cordova.exec(
                (response) => {
                    if (successcb) successcb(response);
                },
                (err) => {
                    console.log(err)
            }, "CordovaFacebook", "logout", []);
        }

        info(successcb?: (r: any) => void, failcb?: (err: any) => void) {
            if (!(<any>window).cordova) {
                if (failcb) failcb("no cordova");
                return;
            }
            (<any>window).cordova.exec(
                (response) => {
                    if (successcb) successcb(response);
                },
                (err) => {
                    console.log("info call failed with error: " + err);
                    if (failcb) failcb(err);
                }, "CordovaFacebook", "info", []);
        }

        feed(name: string, webUrl: string, logoUrl: string, caption: string, description: string, successcb?: (r: any) => void, failcb?: (err: any) => void) {
            if (!(<any>window).cordova) {
                if (failcb) failcb("no cordova");
                return;
            }            
            (<any>window).cordova.exec(
                (response) => {
                    if (successcb) {
                        if (response && response.post_id) {
                            successcb(response.post_id);
                        } else {
                            successcb(null);
                        }
                    }
                },
                (err) => {
                    console.log("feed call failed with error: " + err);
                    if (failcb) failcb(err);
            }, "CordovaFacebook", "feed", [name, webUrl, logoUrl, caption, description]);
        }

        share(name: string, webUrl: string, logoUrl: string, caption: string, description: string, successcb?: () => void, failcb?: (err: any) => void) {
            if (!(<any>window).cordova) {
                if (failcb) failcb("no cordova");
                return;
            }            
            (<any>window).cordova.exec(
                (response) => {
                    if (successcb) {
                        successcb();                        
                    }
                },
                (err) => {
                    console.log("share call failed with error: " + err);
                    if (failcb) failcb(err);
                }, "CordovaFacebook", "share", [name, webUrl, logoUrl, caption, description]);
        }

        invite(message: string, title: string, successcb: (req: any) => void, failcb?: (err: any) => void) {
            if (!(<any>window).cordova) {
                if (failcb) failcb("no cordova");
                return;
            }
            (<any>window).cordova.exec(
                (response) => {
                    if (successcb) {
                        successcb(response);
                    }
                },
                (err) => {
                    console.log("invite call failed with error: " + err);
                    if (failcb) failcb(err);
                }, "CordovaFacebook", "invite", [message, title]);            
        }

        deleteRequest(request: string, successcb?: () => void, failcb?: (err: any) => void) {
            if (!(<any>window).cordova) {
                if (failcb) failcb("no cordova");
                return;
            }
            (<any>window).cordova.exec(
                () => {
                    if (successcb) successcb();
                },
                (err) => {
                    console.error("deleteRequest call failed with error: " + err);
                    if (failcb) failcb(err);
                }, "CordovaFacebook", "deleteRequest", [request]);
        }
    }
}

declare var module;
module.exports = CC;