//
//  HMUtility.h
//  Hummer
//
//  Copyright © 2019年 didi. All rights reserved.
//

#import "NSString+Hummer.h"
#import <CommonCrypto/CommonDigest.h>

@implementation NSString(Hummer)

+ (BOOL)hm_isValidString:(NSString *)string {
    return ((!!string) && [string isKindOfClass:[NSString class]] && string.length > 0);
}

- (BOOL)hm_isURLString {
    return [NSURL URLWithString:self] != nil;
}

- (NSString *)hm_stringWithURLEncoded {
    return [self stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLQueryAllowedCharacterSet]];
}

- (NSString *)hm_md5String {
    const char * character = [self UTF8String];
    unsigned char result[CC_MD5_DIGEST_LENGTH];
    CC_MD5(character, (CC_LONG)strlen(character), result);
    NSMutableString *md5String = [NSMutableString stringWithCapacity:CC_MD5_DIGEST_LENGTH];
    for(int i = 0; i < CC_MD5_DIGEST_LENGTH; i++) {
        [md5String appendFormat:@"%02x",result[i]];
    }
    return md5String;
}

- (NSString *)hm_sh1String {
    NSData *data = [self dataUsingEncoding:NSUTF8StringEncoding];
    uint8_t digest[CC_SHA1_DIGEST_LENGTH];
    CC_SHA1(data.bytes, (unsigned int)data.length, digest);
    NSMutableString* output = [NSMutableString stringWithCapacity:CC_SHA1_DIGEST_LENGTH * 2];
    for(int i = 0; i < CC_SHA1_DIGEST_LENGTH; i++) {
        [output appendFormat:@"%02x", digest[i]];
    }
    return output;
}

- (BOOL)hm_isHTTPURLString {
    if (![self hasPrefix:@"http"] && ![self hasPrefix:@"https"]) {
        return NO;
    }
    return [self hm_isURLString];
}

@end
