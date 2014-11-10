//
//  ScanMatic.m
//  app
//
//  Created by Zhenya on 2014-11-04.
//
//

#import "ScanMatic.h"

@implementation ScanMatic

@synthesize cameraPreview;

NSString* version = @"0.0.1";


- (void)pluginInitialize {
    
    [super pluginInitialize];

    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onPause) name:UIApplicationDidEnterBackgroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onResume) name:UIApplicationWillEnterForegroundNotification object:nil];

    
    CGRect viewBounds = self.viewController.view.bounds;

    viewBounds.origin = self.viewController.view.bounds.origin;
    
    cameraPreview = [[UIView alloc] initWithFrame:viewBounds];
    cameraPreview.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    
    [self.viewController.view insertSubview:self.cameraPreview belowSubview:self.webView];
    
    session = [[AVCaptureSession alloc] init];

    session.sessionPreset = AVCaptureSessionPresetMedium;

    AVCaptureVideoPreviewLayer *captureVideoPreviewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession:session];

    captureVideoPreviewLayer.frame = self.cameraPreview.bounds;
    [self.cameraPreview.layer addSublayer:captureVideoPreviewLayer];
}


- (void)onPause {

}

- (void)onResume {

}


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
    
    self.webView.opaque = NO;
    self.webView.backgroundColor = [UIColor clearColor];
    
    bool enable = (bool)[command.arguments objectAtIndex:0];
    
    CDVPluginResult* pluginResult;
    
    if (enable) {
        
                
        NSError *error = nil;
        
        if (!cameraInput) {
            AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
            cameraInput = [AVCaptureDeviceInput deviceInputWithDevice:device error:&error];
        }
        
        if (!cameraInput) {
            // Handle the error appropriately.
            NSLog(@"ERROR: trying to open camera: %@", error);
            
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            
        } else {
            
            [session addInput:cameraInput];
            [session startRunning];
            
            
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            
        }
            
    } else {
        if (!cameraInput) {
        
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"camera was never started"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
        } else {
            
            [session removeInput:cameraInput];
            [session stopRunning];
            
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
        
        
    }
    
    
    
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
