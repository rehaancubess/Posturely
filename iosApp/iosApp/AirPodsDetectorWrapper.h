//
//  AirPodsDetectorWrapper.h
//  iosApp
//
//  Created by AirPods Detection Wrapper
//

#import <Foundation/Foundation.h>

@interface AirPodsDetectorWrapper : NSObject

+ (BOOL)isAirPodsConnected;
+ (NSString *)getConnectedDeviceName;
+ (void)startMonitoring;
+ (void)stopMonitoring;

@end
