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
                
                cameraHandle = device;
                
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
                
                [camera setObject:flashModes forKey:@"flashModes"];
                [info setObject:camera forKey:@"camera"];
            }
        }
    }
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:info];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void)camera:(CDVInvokedUrlCommand*)command {
    
    self.webView.opaque = NO;
    self.webView.backgroundColor = [UIColor clearColor];
    
    
    NSNumber *enable = [command.arguments objectAtIndex:0];
    
    if(![enable isKindOfClass:[NSNumber class]]) {
        enable = [NSNumber numberWithBool:NO];
    }
    
    CDVPluginResult* pluginResult;
    
    if ([enable boolValue]) {
        
        NSError *error = nil;
        
        if (!cameraHandle)
        {
            NSArray *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
            for (AVCaptureDevice *device in devices) {
                if ([device hasMediaType:AVMediaTypeVideo]) {
                    if ([device position] == AVCaptureDevicePositionBack) {
                        cameraHandle = device;
                    }
                }
            }
        }
        
        if (!cameraInput) {
            // AVCaptureDevice *device = [AVCaptureDevice defaultDeviceWithMediaType:AVMediaTypeVideo];
            cameraInput = [AVCaptureDeviceInput deviceInputWithDevice:cameraHandle error:&error];
        }
        
        if (!cameraInput) {
            // Handle the error appropriately.
            NSLog(@"ERROR: trying to open camera: %@", error);
            
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:[error localizedDescription]];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            
        } else {
            
            [session beginConfiguration];
            [self setCaptureSize];
            [session addInput:cameraInput];
            [session commitConfiguration];
            [session startRunning];
            
            
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
            
        }
            
    } else {
        
        if (!cameraInput) {

            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"camera was never started"];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        
        } else {
            
            [session beginConfiguration];
            [session removeInput:cameraInput];
            [session commitConfiguration];
            [session stopRunning];
            
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
    }
}


- (void)focus:(CDVInvokedUrlCommand*)command {
    
    int flags = NSKeyValueObservingOptionNew;
    [cameraHandle addObserver:self forKeyPath:@"adjustingFocus" options:flags context:nil];
    
    [cameraHandle lockForConfiguration:nil];
    
    if ([cameraHandle isFocusModeSupported:AVCaptureFocusModeAutoFocus]) {
        [cameraHandle setFocusMode:AVCaptureFocusModeAutoFocus];
    }
    
    [cameraHandle unlockForConfiguration];
    
    

}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary *)change context:(void *)context {
    if( [keyPath isEqualToString:@"adjustingFocus"] ){
        BOOL adjustingFocus = [ [change objectForKey:NSKeyValueChangeNewKey] isEqualToNumber:[NSNumber numberWithInt:1] ];
        NSLog(@"Is adjusting focus? %@", adjustingFocus ? @"YES" : @"NO" );
       
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




- (void)flash:(CDVInvokedUrlCommand*)command {

    CDVPluginResult* pluginResult;
    NSString *flashState = [command.arguments objectAtIndex:0];
    [cameraHandle lockForConfiguration:nil];
    
    if ([flashState  isEqual: @"torch"])
    {
        [cameraHandle setTorchMode:AVCaptureTorchModeOn];
        [cameraHandle setFlashMode:AVCaptureFlashModeOn];
    }
    else if ([flashState  isEqual: @"auto"])
    {
        [cameraHandle setTorchMode:AVCaptureTorchModeOff];
        [cameraHandle setFlashMode:AVCaptureFlashModeAuto];
    }
    else if ([flashState  isEqual: @"off"])
    {
        [cameraHandle setTorchMode:AVCaptureTorchModeOff];
        [cameraHandle setFlashMode:AVCaptureFlashModeOff];
    }
    [cameraHandle unlockForConfiguration];
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
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
    NSLog(@"Resuming application");
}

- (void)onPause {
    if (cameraInput) {
        [session removeInput:cameraInput];
        [session stopRunning];
    }
}

- (void)setImageSpecs:(CDVInvokedUrlCommand*)command {
   
    CDVPluginResult* pluginResult;
    
    jpegCompression = [command.arguments objectAtIndex:0];
    pixelsTarget = [command.arguments objectAtIndex:1];
    
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)finish:(CDVInvokedUrlCommand*)command {
    CDVPluginResult* pluginResult;
    pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"not supported on iOS"];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];

}

- (void)capture:(CDVInvokedUrlCommand*)command {
    
    cameraOutput = [[AVCaptureStillImageOutput alloc] init];
    [session beginConfiguration];
    [session addOutput:cameraOutput];
    [session commitConfiguration];
    
    NSDictionary *outputSettings = @{ AVVideoCodecKey : AVVideoCodecJPEG};
    [cameraOutput setOutputSettings:outputSettings];
    
    AVCaptureConnection *videoConnection = nil;
    for (AVCaptureConnection *connection in cameraOutput.connections) {
        for (AVCaptureInputPort *port in [connection inputPorts]) {
            if ([[port mediaType] isEqual:AVMediaTypeVideo] ) {
                videoConnection = connection;
                break;
            }
        }
        if (videoConnection) { break; }
    }
    
    [cameraOutput captureStillImageAsynchronouslyFromConnection:videoConnection completionHandler:
        ^(CMSampleBufferRef imageSampleBuffer, NSError *error) {
            
            [session beginConfiguration];
            [session removeOutput:cameraOutput];
            [session commitConfiguration];

            //get image
            NSData *largeJpeg = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageSampleBuffer] ;
            UIImage *image = [UIImage imageWithData:largeJpeg];
            
            //create overlay
            UIImage *resizedImage = [self imageWithImage:image scaledToSize:CGSizeMake(image.size.width/2, image.size.height/2)];
            NSData *overlayJpeg = UIImageJPEGRepresentation(resizedImage, 0.25);
            NSLog(@"OVERLAYSIZE: %i", overlayJpeg.length);
            
            //send overlay to javascript
            CDVPluginResult* pluginResult;
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsArrayBuffer:overlayJpeg];
            pluginResult.keepCallback = [NSNumber numberWithBool:YES];
            [self.commandDelegate sendPluginResult:pluginResult callbackId:callbackPreview];
            
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
                NSArray *paths = NSSearchPathForDirectoriesInDomains(NSCachesDirectory,NSUserDomainMask,YES);
                NSString *documentsDirectory = [paths objectAtIndex:0];
                NSString *dataPath = [documentsDirectory stringByAppendingPathComponent:imageName];
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
                    NSLog(@"DATA: %@", fileRecord);
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
     }];
    
}

- (UIImage*)imageWithImage:(UIImage*)image
              scaledToSize:(CGSize)newSize
{
    UIGraphicsBeginImageContext( newSize );
    [image drawInRect:CGRectMake(0,0,newSize.width,newSize.height)];
    UIImage* newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return newImage;
}


- (void)setCaptureSize {
    
    NSLog(@"PIXELS_TARGET: %@", pixelsTarget);
    NSLog(@"JPEG_COMPRESSION: %@", jpegCompression);
    
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



@end
