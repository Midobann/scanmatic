//
//  ScanMatic.h
//  app
//
//  Created by Zhenya on 2014-11-04.
//
//

#import <Cordova/CDVPlugin.h>
#import <AVFoundation/AVFoundation.h>


@interface ScanMatic : CDVPlugin

- (void)info:(CDVInvokedUrlCommand*)command;
- (void)camera:(CDVInvokedUrlCommand*)command;
- (void)capture:(CDVInvokedUrlCommand*)command;
- (void)focus:(CDVInvokedUrlCommand*)command;
- (void)flash:(CDVInvokedUrlCommand*)command;
- (void)finish:(CDVInvokedUrlCommand*)command;
- (void)onCapture:(CDVInvokedUrlCommand*)command;
- (void)onPreview:(CDVInvokedUrlCommand*)command;
- (void)onAutoFocus:(CDVInvokedUrlCommand*)command;
- (void)onAutoFocusMove:(CDVInvokedUrlCommand*)command;


@end
