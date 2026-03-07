#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>

// 1. Block standard banner ads
%hook GADBannerView
- (void)loadRequest:(id)request {
    NSLog(@"[MyAdBlocker] 🛡️ Blocked GADBannerView successfully!");
}
%end

// 2. Block full-screen (interstitial) ads
%hook GADInterstitialAd
+ (void)loadWithAdUnitID:(id)arg1 request:(id)arg2 completionHandler:(id)arg3 {
    NSLog(@"[MyAdBlocker] 🛡️ Blocked GADInterstitialAd successfully!");
}
%end

// 3. Block GADBannerAd class
%hook GADBannerAd
- (void)loadRequest:(id)request {
    NSLog(@"[MyAdBlocker] 🛡️ Blocked GADBannerAd successfully!");
}
%end
