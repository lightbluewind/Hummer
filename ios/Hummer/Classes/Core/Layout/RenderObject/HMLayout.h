//
//  HMLayout.h
//  yogaDemo1
//
//  Created by didi on 2020/9/18.
//  Copyright © 2020 didi. All rights reserved.
//

#import <UIKit/UIKit.h>
#if __has_include(<yoga/Yoga.h>)
#import <yoga/Yoga.h>
#elif __has_include(<YogaKit/Yoga.h>)
#import <YogaKit/Yoga.h>
#endif

NS_ASSUME_NONNULL_BEGIN

@class HMRenderObject;

typedef NS_ENUM(NSInteger, HMDisplayType) {
    HMDisplayTypeNone,
    HMDisplayTypeFlex,
    // 实际上没有实现
    HMDisplayTypeInline,
};

/**
 * @brief 布局指标，用于对比两次布局结果是否一致，如果不一致，说明是 affectedShadowViews
 */
struct HMLayoutMetrics {
    CGRect frame;
    CGRect contentFrame;
    UIEdgeInsets borderWidth;
    HMDisplayType displayType;
    UIUserInterfaceLayoutDirection layoutDirection;
};
typedef struct CG_BOXABLE HMLayoutMetrics HMLayoutMetrics;

/**
 * @brief 会被存放在 YGNodeRef context 中
 */
struct HMLayoutContext {
    CGPoint absolutePosition;
    __unsafe_unretained NSHashTable<HMRenderObject *> *_Nonnull affectedShadowViews;
    __unsafe_unretained NSHashTable<NSString *> *_Nonnull other;
};
typedef struct CG_BOXABLE HMLayoutContext HMLayoutContext;

static inline BOOL HMLayoutMetricsEqualToLayoutMetrics(HMLayoutMetrics a, HMLayoutMetrics b) {
    return CGRectEqualToRect(a.frame, b.frame) && CGRectEqualToRect(a.contentFrame, b.contentFrame) &&
            UIEdgeInsetsEqualToEdgeInsets(a.borderWidth, b.borderWidth) && a.displayType == b.displayType &&
            a.layoutDirection == b.layoutDirection;
}

FOUNDATION_EXTERN HMLayoutMetrics HMLayoutMetricsFromYogaNode(YGNodeRef yogaNode);

/**
 * Converts float values between Yoga and CoreGraphics representations,
 * especially in terms of edge cases.
 */
FOUNDATION_EXTERN float HMYogaFloatFromCoreGraphicsFloat(CGFloat value);

FOUNDATION_EXTERN CGFloat HMCoreGraphicsFloatFromYogaFloat(float value);

/**
 * Converts compound `YGValue` to simple `CGFloat` value.
 */
FOUNDATION_EXTERN CGFloat HMCoreGraphicsFloatFromYogaValue(YGValue value, CGFloat baseFloatValue);

/**
 * @brief Converts `YGDirection` to `UIUserInterfaceLayoutDirection` and vise versa.
 *
 * YGDirectionInherit 会被当做 UIUserInterfaceLayoutDirectionLeftToRight
 */
FOUNDATION_EXTERN YGDirection HMYogaLayoutDirectionFromUIKitLayoutDirection(UIUserInterfaceLayoutDirection direction);

FOUNDATION_EXTERN UIUserInterfaceLayoutDirection HMUIKitLayoutDirectionFromYogaLayoutDirection(YGDirection direction);

/**
 * @brief Converts `YGDisplay` to `RCTDisplayType` and vise versa.
 *
 * 只支持 Flex 和 None，Inline 不支持，会转换为 None
 */
FOUNDATION_EXTERN YGDisplay HMYogaDisplayTypeFromReactDisplayType(HMDisplayType displayType);

FOUNDATION_EXTERN HMDisplayType HMReactDisplayTypeFromYogaDisplayType(YGDisplay displayType);

NS_ASSUME_NONNULL_END

