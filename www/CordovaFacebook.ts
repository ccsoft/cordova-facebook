module CC {
    export class CordovaFacebook {
        constructor(private appId: string, private appNamespace: string, private appPermissions: string[]) {
        }

        init(successcb?: (r: any) => void, failcb?: (err: any) => void) {
            (<any>window).cordova.exec(
                (response) => {
                    console.log("init call successful");
                    if (successcb) successcb(response);
                },
                (err) => {
                    console.log("init call failed with error: " + err);
                    if (failcb) failcb(err);
                }, "CordovaFacebook", "init", [this.appId, this.appNamespace, this.appPermissions]);
        }
        
        login(successcb?: (r: any) => void, failcb?: (err: any) => void) {
            (<any>window).cordova.exec(
                (response) => {
                    console.log("login call successful");
                    if (successcb) successcb(response);
                },
                (err) => {
                    console.log("login call failed with error: " + err);
                    if (failcb) failcb(err);
                }, "CordovaFacebook", "login", []);
        }

        logout(successcb?: (r: any) => void) {
            (<any>window).cordova.exec(
                (response) => {
                    console.log("logout call successful");
                    if (successcb) successcb(response);
                },
                (err) => {
                    console.log(err)
            }, "CordovaFacebook", "logout", []);
        }
    }
}