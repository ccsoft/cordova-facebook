var CC;
(function (CC) {
    var CordovaFacebook = (function () {
        function CordovaFacebook(appId) {
            this.appId = appId;
        }
        CordovaFacebook.prototype.login = function (successcb, failcb) {
            window.cordova.exec(function (response) {
                console.log("login call successful");
                if (successcb)
                    successcb(response);
            }, function (err) {
                console.log("login call failed with error: " + err);
                if (failcb)
                    failcb(err);
            }, "CordovaFacebook", "login", []);
        };

        CordovaFacebook.prototype.logout = function (successcb) {
            window.cordova.exec(function (response) {
                console.log("logout call successful");
                if (successcb)
                    successcb(response);
            }, function (err) {
                console.log(err);
            }, "CordovaFacebook", "logout", []);
        };
        return CordovaFacebook;
    })();
    CC.CordovaFacebook = CordovaFacebook;
})(CC || (CC = {}));
