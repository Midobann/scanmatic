//
//  ScanMatic.m
//  app
//
//  Created by Zhenya on 2014-11-04.
//
//

#import "ScanMatic.h"

@implementation ScanMatic

NSString* version = @"0.0.1";

- (void)info:(CDVInvokedUrlCommand*)command {
    
    NSArray *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    
    NSMutableDictionary* info = [NSMutableDictionary dictionary];
    NSMutableDictionary* camera = [NSMutableDictionary dictionary];
    NSMutableArray* flashModes = [NSMutableArray array];
    
    [flashModes addObject:@"off"];
    
    [info setObject:version forKey:@"version"];
    
    for (AVCaptureDevice *device in devices) {
        
        if ([device hasMediaType:AVMediaTypeVideo]) {
            
            if ([device position] == AVCaptureDevicePositionBack) {
                
                [camera setObject:[device localizedName] forKey:@"name"];
                
                if (device.hasFlash && device.flashAvailable) {
                    if ([device isFlashModeSupported:AVCaptureFlashModeOn]) {
                        [flashModes addObject:@"on"];
                    }
                    
                    if ([device isFlashModeSupported:AVCaptureFlashModeAuto]) {
                        [flashModes addObject:@"auto"];
                    }
                }
                
                if (device.hasTorch && device.torchAvailable) {
                    [flashModes addObject:@"torch"];
                }
                
                [info setObject:flashModes forKey:@"flashModes"];
            }
        }
    }
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:info];
	
	[self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)camera:(CDVInvokedUrlCommand*)command {
    
}


- (void)capture:(CDVInvokedUrlCommand*)command {
    
}

- (void)focus:(CDVInvokedUrlCommand*)command {
    
}

- (void)flash:(CDVInvokedUrlCommand*)command {
    
}

- (void)finish:(CDVInvokedUrlCommand*)command {
    
}

- (void)onCapture:(CDVInvokedUrlCommand*)command {
    
}

- (void)onPreview:(CDVInvokedUrlCommand*)command {
    
}

- (void)onAutoFocus:(CDVInvokedUrlCommand*)command {
    
}

- (void)onAutoFocusMove:(CDVInvokedUrlCommand*)command {
    
}


@end
