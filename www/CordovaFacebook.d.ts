declare module CC {
    export interface ICordovaFacebook {
        init: (appId: string, appNamespace: string, appPermissions: string[], successcb?: (r: any) => void, failcb?: (err: any) => void) => void;
        login: (successcb?: (r: any) => void, failcb?: (err: any) => void) => void;
        logout: (successcb?: (r: any) => void) => void;
        info: (successcb?: (r: any) => void, failcb?: (err: any) => void) => void;
        feed: (name: string, webUrl: string, logoUrl: string, caption: string, description: string, successcb?: (r: any) => void, failcb?: (err: any) => void) => void;
        share: (name: string, webUrl: string, logoUrl: string, caption: string, description: string, successcb?: () => void, failcb?: (err: any) => void) => void;
        invite: (message: string, title: string, successcb: (req: any) => void, failcb?: (err: any) => void) => void;
    }
}
