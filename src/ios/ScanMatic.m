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


- (void)pluginInitialize {
    
    [super pluginInitialize];
    uriLast = [NSMutableDictionary dictionary];
    [uriLast setObject:@"spendmatic://null?loginToken=null" forKey:@"uri"];

    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onPause) name:UIApplicationDidEnterBackgroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onResume) name:UIApplicationWillEnterForegroundNotification object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(onLaunch:) name:UIApplicationDidFinishLaunchingNotification object:nil];
    
    CGRect viewBounds = [[UIScreen mainScreen] bounds];
    //NSLog(@"VIDEO BOUNDS w: %.2f h: %.2f", viewBounds.size.width, viewBounds.size.height);
    //viewBounds.size.height += 20;

    viewBounds.origin = self.viewController.view.bounds.origin;
    
    cameraPreview = [[UIView alloc] initWithFrame:viewBounds];
    cameraPreview.backgroundColor = [UIColor colorWithWhite:0.0f alpha:1.0f];
    cameraPreview.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    
    [self.viewController.view insertSubview:self.cameraPreview belowSubview:self.webView];
    
    session = [[AVCaptureSession alloc] init];

    session.sessionPreset = AVCaptureSessionPresetMedium;

    AVCaptureVideoPreviewLayer *captureVideoPreviewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession:session];
    
    captureVideoPreviewLayer.videoGravity = AVLayerVideoGravityResize;
    //captureVideoPreviewLayer.

    captureVideoPreviewLayer.frame = self.cameraPreview.bounds;
    [self.cameraPreview.layer addSublayer:captureVideoPreviewLayer];
    
    //get camera handle
    NSArray *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    for (AVCaptureDevice *device in devices) {
        if ([device hasMediaType:AVMediaTypeVideo]) {
            if ([device position] == AVCaptureDevicePositionBack) {
                cameraHandle = device;
            }
        }
    }
    
    //get camera input
    NSError *error = nil;
    cameraInput = [AVCaptureDeviceInput deviceInputWithDevice:cameraHandle error:&error];
    
    //get camera output
    cameraOutput = [[AVCaptureStillImageOutput alloc] init];
    NSDictionary *outputSettings = @{ AVVideoCodecKey : AVVideoCodecJPEG};
    [cameraOutput setOutputSettings:outputSettings];
    
    //default values for compression
    jpegCompression = [NSNumber numberWithInt:60];
    pixelsTarget = [NSNumber numberWithInt:1200000];
    localFlashState = @"off";
    uploadInProgress = 0;
    backgroundTime = 0;
    
}

- (void)info:(CDVInvokedUrlCommand*)command {
    
    @try {
        
        NSMutableDictionary* info = [NSMutableDictionary dictionary];
        
        NSString* version = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleVersion"];
        [info setObject:version forKey:@"version"];

        if (cameraHandle != nil)
        {
            [session beginConfiguration];
            [self setCaptureSize];
            [session addInput:cameraInput];
            [session addOutput:cameraOutput];
            [session commitConfiguration];

            

            
            NSMutableDictionary* camera = [NSMutableDictionary dictionary];
            NSMutableArray* flashModes = [NSMutableArray array];
            
            [camera setObject:[cameraHandle localizedName] forKey:@"name"];
            [flashModes addObject:@"off"];
            if (cameraHandle.hasFlash && cameraHandle.flashAvailable) {
//                if ([cameraHandle isFlashModeSupported:AVCaptureFlashModeOn]) {
//                    [flashModes addObject:@"on"];
//                }
                
                if ([cameraHandle isFlashModeSupported:AVCaptureFlashModeAuto]) {
                    [flashModes addObject:@"auto"];
                }
            }
//            if (cameraHandle.hasTorch && cameraHandle.torchAvailable) {
//                [flashModes addObject:@"torch"];
//            }
            [camera setObject:flashModes forKey:@"flashModes"];
            [info setObject:camera forKey:@"camera"];
        }
        
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:info];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    @catch (NSException * e) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[e reason]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}

- (void)camera:(CDVInvokedUrlCommand*)command {
    
    @try {

        CDVPluginResult* pluginResult;
        
        self.webView.opaque = NO;
        self.webView.backgroundColor = [UIColor clearColor];
        
        NSNumber *enable = [command.arguments objectAtIndex:0];
        if(![enable isKindOfClass:[NSNumber class]]) {
            enable = [NSNumber numberWithBool:NO];
        }
        if ([enable boolValue]) {
            
            if (!cameraInput || !cameraHandle) {
                NSString *error = @"ERROR: camera did not open correctly";
                NSLog(@"%@", error);
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
                
            } else {
            
                [session startRunning];
            
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }
            
        } else {
            
            if (!cameraInput) {
                NSString *error = @"camera was never started";
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:error];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            } else {
                [session stopRunning];
                pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
                [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            }
        }
    }
    @catch (NSException * e) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[e reason]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

}


- (void)focus:(CDVInvokedUrlCommand*)command {
    @try {
        int flags = NSKeyValueObservingOptionNew;
        [cameraHandle addObserver:self forKeyPath:@"adjustingFocus" options:flags context:nil];
        
        [cameraHandle lockForConfiguration:nil];
        if ([cameraHandle isFocusModeSupported:AVCaptureFocusModeAutoFocus]) {
            [cameraHandle setFocusMode:AVCaptureFocusModeAutoFocus];
        }
        [cameraHandle unlockForConfiguration];
    }
    @catch (NSException * e) {
        NSLog(@"Focus Exception: %@", e);
    }

}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {
    if( [keyPath isEqualToString:@"adjustingFocus"] ){
        BOOL adjustingFocus = [ [change objectForKey:NSKeyValueChangeNewKey] isEqualToNumber:[NSNumber numberWithInt:1] ];
        if (adjustingFocus)
        {
            CDVPluginResult* pluginResult;
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            pluginResult.keepCallback = [NSNumber numberWithBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackFocusMoved];
        }
        else
        {
            CDVPluginResult* pluginResult;
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsBool:YES];
            pluginResult.keepCallback = [NSNumber numberWithBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackAutoFocus];
        }
    }
}

- (void)sound:(CDVInvokedUrlCommand*)command {

    @try {
        CDVPluginResult* pluginResult;
        NSString *soundName = [command.arguments objectAtIndex:0];
        //play sound
        NSString *audioPath = [[NSBundle mainBundle] pathForResource:soundName ofType:@"mp3"];
        NSURL *audioURL = [NSURL fileURLWithPath:audioPath];
        audioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:audioURL error:nil];
        [audioPlayer play];

        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    @catch (NSException * e) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[e reason]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

}

- (void)setFlash {
    @try {
        [cameraHandle lockForConfiguration:nil];
        if ([localFlashState  isEqual: @"torch"])
        {
            [cameraHandle setTorchMode:AVCaptureTorchModeOn];
            [cameraHandle setFlashMode:AVCaptureFlashModeOn];
        }
        else if ([localFlashState  isEqual: @"auto"])
        {
            [cameraHandle setTorchMode:AVCaptureTorchModeOff];
            [cameraHandle setFlashMode:AVCaptureFlashModeAuto];
        }
        else if ([localFlashState  isEqual: @"off"])
        {
            [cameraHandle setTorchMode:AVCaptureTorchModeOff];
            [cameraHandle setFlashMode:AVCaptureFlashModeOff];
        }
        [cameraHandle unlockForConfiguration];
    }
    @catch (NSException * e) {
        NSLog(@"Set Flash Exception: %@", e);

    }
}

- (void)flash:(CDVInvokedUrlCommand*)command {
    @try {
        CDVPluginResult* pluginResult;
        NSString *flashState = [command.arguments objectAtIndex:0];
        localFlashState = flashState;
        [self setFlash];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    @catch (NSException * e) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[e reason]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
}


- (void)onCapture:(CDVInvokedUrlCommand*)command {
    callbackCapture = command.callbackId;
}

- (void)onPreview:(CDVInvokedUrlCommand*)command {
    callbackPreview = command.callbackId;
}

- (void)onAutoFocus:(CDVInvokedUrlCommand*)command {
    callbackAutoFocus = command.callbackId;
}

- (void)onAutoFocusMove:(CDVInvokedUrlCommand*)command {
    callbackFocusMoved = command.callbackId;
}

- (void)onResume {
    backgroundTime = 0;
    [self setFlash];
}

- (void)onPause {
    
    UIApplication *app = [UIApplication sharedApplication];
    
    UIBackgroundTaskIdentifier bgTask = 0;
    bgTask = [app beginBackgroundTaskWithName:@"uploadWait" expirationHandler:^{
        [app endBackgroundTask:bgTask];
    }];
    
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
        
        while((uploadInProgress != 0) && (backgroundTime < 300))
        {
            [NSThread sleepForTimeInterval:2.0f];
            backgroundTime += 2;
        }
        backgroundTime = 0;
        [app endBackgroundTask:bgTask];
    });
}

- (void)setImageSpecs:(CDVInvokedUrlCommand*)command {
   
   @try {
        CDVPluginResult* pluginResult;
        
        jpegCompression = [command.arguments objectAtIndex:0];
        pixelsTarget = [command.arguments objectAtIndex:1];
        
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }
    @catch (NSException * e) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[e reason]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

}

- (void)finish:(CDVInvokedUrlCommand*)command {
    CDVPluginResult* pluginResult;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"not supported on iOS"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)capture:(CDVInvokedUrlCommand*)command {

    @try {

        ++uploadInProgress;
        
        for (AVCaptureConnection *connection in cameraOutput.connections) {
            for (AVCaptureInputPort *port in [connection inputPorts]) {
                if ([[port mediaType] isEqual:AVMediaTypeVideo] ) {
                    videoConnection = connection;
                    break;
                }
            }
            if (videoConnection) { break; }
        }

    }
    @catch (NSException * e) {
        NSLog(@"Prepare Capture Exception: %@", e);
    }
    
    [cameraOutput captureStillImageAsynchronouslyFromConnection:videoConnection completionHandler:
        ^(CMSampleBufferRef imageSampleBuffer, NSError *error) {
            
            @try {
                //get image
                NSData *largeJpeg = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageSampleBuffer] ;
                UIImage *image = [UIImage imageWithData:largeJpeg];
                
                //create overlay
                float shrink = (float)image.size.width / 320.0;
                UIImage *resizedImage = [self imageWithImage:image scaledToSize:CGSizeMake(320, image.size.height/shrink)];
                NSData *overlayJpeg = UIImageJPEGRepresentation(resizedImage, 0.25);
                
                //send overlay to javascript
                if (overlayJpeg)
                {
                    CDVPluginResult* pluginResult;
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArrayBuffer:overlayJpeg];
                    pluginResult.keepCallback = [NSNumber numberWithBool:YES];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackPreview];

                }
                else
                {
                    CDVPluginResult* pluginResult;
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no overlay jpeg"];
                    pluginResult.keepCallback = [NSNumber numberWithBool:YES];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackPreview];
                }
                
                //reset flash and output
//                [session beginConfiguration];
//                [session removeOutput:cameraOutput];
//                [session commitConfiguration];
//                [self setFlash];

                //compress image
                float compressionFactor = ((float)[jpegCompression intValue]) / 100.0;
                NSData *jpeg = UIImageJPEGRepresentation(image, compressionFactor);
                
                if (jpeg) {
                
                    //create random image name
                    NSString *alphabet  = @"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXZY0123456789";
                    NSMutableString *s = [NSMutableString stringWithCapacity:10];
                    for (NSUInteger i = 0U; i < 10; i++) {
                        u_int32_t r = arc4random() % [alphabet length];
                        unichar c = [alphabet characterAtIndex:r];
                        [s appendFormat:@"%C", c];
                    }
                   
                    //create image metadata
                    NSString *imageName = [s stringByAppendingString:@".jpeg"];
                    NSString *imageType = @"image/jpeg";
                    NSNumber *imageSize = [NSNumber numberWithLong:jpeg.length];
                    NSNumber *imageTime = [NSNumber numberWithLong:[[NSDate date] timeIntervalSince1970]];
                    
                    //create storage path and store
                    NSArray *paths = NSSearchPathForDirectoriesInDomains(NSLibraryDirectory,NSUserDomainMask,YES);
                    NSString *libDirectory = [paths objectAtIndex:0];
                    NSString *dataPath2 = [libDirectory stringByAppendingPathComponent:@"files"];
                    NSString *dataPath = [dataPath2 stringByAppendingPathComponent:imageName];
                    [jpeg writeToFile:dataPath atomically:YES];
                    
                    //notify javascript
                    BOOL fileExists = [[NSFileManager defaultManager] fileExistsAtPath:dataPath];
                    if (fileExists)
                    {
                        NSMutableDictionary* fileRecord = [NSMutableDictionary dictionary];
                        [fileRecord setObject:imageName forKey:@"name"];
                        [fileRecord setObject:imageSize forKey:@"size"];
                        [fileRecord setObject:imageType forKey:@"type"];
                        [fileRecord setObject:imageTime forKey:@"lastModified"];
                        CDVPluginResult* pluginResult;
                        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:fileRecord];
                        pluginResult.keepCallback = [NSNumber numberWithBool:YES];
                        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackCapture];
                        
                    }
                    else
                    {
                        CDVPluginResult* pluginResult;
                        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"failed to save image to file"];
                        pluginResult.keepCallback = [NSNumber numberWithBool:YES];
                        [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackCapture];
                    }
                }
                else
                {
                    CDVPluginResult* pluginResult;
                    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"no jpeg image"];
                    pluginResult.keepCallback = [NSNumber numberWithBool:YES];
                    [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackCapture];
                }
            }
            @catch (NSException * e) {
                NSLog(@"Capture Exception: %@", e);
            }
     }];
    
}

- (UIImage*)imageWithImage:(UIImage*)image
              scaledToSize:(CGSize)newSize
{
    @try {
        UIGraphicsBeginImageContext( newSize );
        [image drawInRect:CGRectMake(0,0,newSize.width,newSize.height)];
        UIImage* newImage = UIGraphicsGetImageFromCurrentImageContext();
        UIGraphicsEndImageContext();
        
        return newImage;
     }
    @catch (NSException * e) {
        NSLog(@"Scale Image Exception: %@", e);
    }
}


- (void)setCaptureSize {

    @try {
        NSMutableArray* formats = [NSMutableArray array];
        [formats addObject:@"AVCaptureSessionPreset352x288"];
        [formats addObject:@"AVCaptureSessionPreset640x480"];
        [formats addObject:@"AVCaptureSessionPreset1280x720"];
        [formats addObject:@"AVCaptureSessionPreset1920x1080"];
        [formats addObject:@"AVCaptureSessionPresetPhoto"];
        
        NSMutableArray* sizes = [NSMutableArray array];
        [sizes addObject:[NSNumber numberWithInt:101376]];
        [sizes addObject:[NSNumber numberWithInt:307200]];
        [sizes addObject:[NSNumber numberWithInt:921600]];
        [sizes addObject:[NSNumber numberWithInt:2073600]];
        [sizes addObject:[NSNumber numberWithInt:5000000]];
        
        int distance = 10000000;
        int finalIndex = 0;
        for(int i = 0 ; i < sizes.count; i++)
        {
            NSNumber *val = [sizes objectAtIndex:i];
            int valDist = abs(([val intValue])-([pixelsTarget intValue]));
            if (valDist > distance)
            {
                break;
            }
            else
            {
                distance = valDist;
                finalIndex = i;
            }
        }
        
        NSString *captureFormat = [formats objectAtIndex:finalIndex];
        session.sessionPreset = captureFormat;
    }
    @catch (NSException * e) {
        NSLog(@"SetCaptureSize Exception: %@", e);
    }
}

- (void)deleteResource:(CDVInvokedUrlCommand*)command {
    
    @try {
        NSString *resName = [command.arguments objectAtIndex:0];

        //create storage path and store
        NSArray *paths = NSSearchPathForDirectoriesInDomains(NSLibraryDirectory,NSUserDomainMask,YES);
        NSString *libDirectory = [paths objectAtIndex:0];
        NSString *dataPath2 = [libDirectory stringByAppendingPathComponent:@"files"];
        NSString *dataPath = [dataPath2 stringByAppendingPathComponent:resName];
    
        NSError *error;
        [[NSFileManager defaultManager]removeItemAtPath:dataPath error:&error];

        if (error)
        {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
        else
        {
            CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }
    @catch (NSException * e) {
        CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[e reason]];
        [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
    }

    --uploadInProgress;

}

- (void)getLaunchURI:(CDVInvokedUrlCommand*)command {
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:uriLast];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)onLaunch:(NSNotification *)note {
    NSDictionary *noteInfo = [note userInfo];
    NSString *uri =[[noteInfo objectForKey:@"UIApplicationLaunchOptionsURLKey"] absoluteString];
    if (uri != nil)
    {
        [uriLast setObject:uri forKey:@"uri"];
    }
}

@end
