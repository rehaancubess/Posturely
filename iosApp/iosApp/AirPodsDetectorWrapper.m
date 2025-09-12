//
//  AirPodsDetectorWrapper.m
//  iosApp
//
//  Created by AirPods Detection Wrapper
//

#import "AirPodsDetectorWrapper.h"
#import <AVFoundation/AVFoundation.h>
#import <Foundation/Foundation.h>

@implementation AirPodsDetectorWrapper

+ (BOOL)isAirPodsConnected {
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    
    NSError *error = nil;
    [audioSession setActive:YES error:&error];
    
    if (error) {
        NSLog(@"Error activating audio session: %@", error);
        return NO;
    }
    
    AVAudioSessionRouteDescription *currentRoute = audioSession.currentRoute;
    
    for (AVAudioSessionPortDescription *output in currentRoute.outputs) {
        NSString *portType = output.portType;
        NSString *portName = output.portName;
        
        NSLog(@"Checking port: %@ of type: %@", portName, portType);
        
        // Check for Bluetooth devices (AirPods, Beats)
        if ([portType isEqualToString:AVAudioSessionPortBluetoothA2DP] ||
            [portType isEqualToString:AVAudioSessionPortBluetoothLE] ||
            [portType isEqualToString:AVAudioSessionPortBluetoothHFP]) {
            
            // Check if the device name contains AirPods or Beats identifiers
            if ([portName.lowercaseString containsString:@"airpods"] ||
                [portName.lowercaseString containsString:@"beats"] ||
                [portName.lowercaseString containsString:@"powerbeats"] ||
                [portName.lowercaseString containsString:@"studio"] ||
                [portName.lowercaseString containsString:@"solo"]) {
                NSLog(@"Found compatible device: %@", portName);
                return YES;
            }
        }
        
        // Also check for wired AirPods (if connected via Lightning)
        if ([portType isEqualToString:AVAudioSessionPortHeadphones] ||
            [portType isEqualToString:AVAudioSessionPortHeadsetMic]) {
            if ([portName.lowercaseString containsString:@"airpods"]) {
                NSLog(@"Found wired AirPods: %@", portName);
                return YES;
            }
        }
    }
    
    NSLog(@"No compatible devices found");
    return NO;
}

+ (NSString *)getConnectedDeviceName {
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    
    NSError *error = nil;
    [audioSession setActive:YES error:&error];
    
    if (error) {
        NSLog(@"Error activating audio session: %@", error);
        return nil;
    }
    
    AVAudioSessionRouteDescription *currentRoute = audioSession.currentRoute;
    
    for (AVAudioSessionPortDescription *output in currentRoute.outputs) {
        NSString *portType = output.portType;
        NSString *portName = output.portName;
        
        // Check for Bluetooth devices (AirPods, Beats)
        if ([portType isEqualToString:AVAudioSessionPortBluetoothA2DP] ||
            [portType isEqualToString:AVAudioSessionPortBluetoothLE] ||
            [portType isEqualToString:AVAudioSessionPortBluetoothHFP]) {
            
            // Check if the device name contains AirPods or Beats identifiers
            if ([portName.lowercaseString containsString:@"airpods"] ||
                [portName.lowercaseString containsString:@"beats"] ||
                [portName.lowercaseString containsString:@"powerbeats"] ||
                [portName.lowercaseString containsString:@"studio"] ||
                [portName.lowercaseString containsString:@"solo"]) {
                return portName;
            }
        }
        
        // Also check for wired AirPods (if connected via Lightning)
        if ([portType isEqualToString:AVAudioSessionPortHeadphones] ||
            [portType isEqualToString:AVAudioSessionPortHeadsetMic]) {
            if ([portName.lowercaseString containsString:@"airpods"]) {
                return portName;
            }
        }
    }
    
    return nil;
}

+ (void)startMonitoring {
    // Start monitoring for audio route changes
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRouteChange:)
                                                 name:AVAudioSessionRouteChangeNotification
                                               object:nil];
    NSLog(@"Started monitoring audio route changes");
}

+ (void)stopMonitoring {
    [[NSNotificationCenter defaultCenter] removeObserver:self];
    NSLog(@"Stopped monitoring audio route changes");
}

+ (void)handleRouteChange:(NSNotification *)notification {
    // Handle audio route changes (device connect/disconnect)
    NSLog(@"Audio route changed");
}

@end
