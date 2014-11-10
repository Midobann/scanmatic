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
{
	AVCaptureSession *session;
	AVCaptureDeviceInput *cameraInput;
   
    NSString *callbackAutoFocus;
    NSString *callbackFocusMoved;
    NSString *callbackCapture;
    NSString *callbackPreview;
    
    NSNumber *jpegCompression;
    NSNumber *pixelsTarget;

}

@property(nonatomic, retain) IBOutlet UIView *cameraPreview;

- (void)pluginInitialize;

- (void) onPause;
- (void) onResume;

- (void)info:(CDVInvokedUrlCommand*)command;
- (void)camera:(CDVInvokedUrlCommand*)command;
- (void)capture:(CDVInvokedUrlCommand*)command;
- (void)focus:(CDVInvokedUrlCommand*)command;
- (void)flash:(CDVInvokedUrlCommand*)command;
- (void)finish:(CDVInvokedUrlCommand*)command;
- (void)setImageSpecs:(CDVInvokedUrlCommand*)command;

- (void)onCapture:(CDVInvokedUrlCommand*)command;
- (void)onPreview:(CDVInvokedUrlCommand*)command;
- (void)onAutoFocus:(CDVInvokedUrlCommand*)command;
- (void)onAutoFocusMove:(CDVInvokedUrlCommand*)command;

@end
