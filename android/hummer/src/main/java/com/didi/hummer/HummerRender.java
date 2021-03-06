package com.didi.hummer;

import android.content.Intent;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.didi.hummer.adapter.http.HttpCallback;
import com.didi.hummer.adapter.navigator.NavPage;
import com.didi.hummer.context.HummerContext;
import com.didi.hummer.core.engine.JSValue;
import com.didi.hummer.core.engine.base.ICallback;
import com.didi.hummer.core.util.DebugUtil;
import com.didi.hummer.core.util.HMGsonUtil;
import com.didi.hummer.devtools.DevToolsConfig;
import com.didi.hummer.devtools.HummerDevTools;
import com.didi.hummer.hotload.HotLoader;
import com.didi.hummer.render.style.HummerLayout;
import com.didi.hummer.utils.AssetsUtil;
import com.didi.hummer.utils.FileUtil;
import com.didi.hummer.utils.JsSourceUtil;
import com.didi.hummer.utils.NetworkUtil;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 负责JS的加载，可以加载JS字符串或JS对应的URL
 *
 * Created by XiaoFeng on 2019-08-27.
 */
public class HummerRender {

    private HummerContext hmContext;
    private AtomicBoolean isDestroyed = new AtomicBoolean(false);
    private HotLoader hotLoader;

    public interface HummerRenderCallback {
        void onSucceed(HummerContext hmContext, JSValue jsPage);
        void onFailed(Exception e);
    }

    public HummerRender(@NonNull HummerLayout container) {
        this(container, null);
    }

    public HummerRender(@NonNull HummerLayout container, String namespace) {
        this(container, namespace, null);
    }

    public HummerRender(@NonNull HummerLayout container, String namespace, DevToolsConfig config) {
        hmContext = Hummer.createContext(container, namespace);

        if (DebugUtil.isDebuggable()) {
            HummerDevTools.init(hmContext, config != null ? config.getInjector() : null);
        }
    }

    public HummerContext getHummerContext() {
        return hmContext;
    }

    public void onStart() {
        hmContext.onStart();
    }

    public void onResume() {
        hmContext.onResume();
    }

    public void onPause() {
        hmContext.onPause();
    }

    public void onStop() {
        hmContext.onStop();
    }

    public void onDestroy() {
        isDestroyed.set(true);

        if (DebugUtil.isDebuggable()) {
            if (hotLoader != null) {
                hotLoader.destroy();
            }
        }

        hmContext.onDestroy();
    }

    public boolean onBack() {
        return hmContext.onBack();
    }

    public void render(String js) {
        render(js, hmContext.getJsSourcePath());
    }

    public void render(String js, String sourcePath) {
        if (TextUtils.isEmpty(js) || isDestroyed.get()) {
            return;
        }

        hmContext.setJsSourcePath(sourcePath);
        hmContext.evaluateJavaScript(js, sourcePath);
    }

    public void renderWithUrl(String url) {
        this.renderWithUrl(url, null);
    }

    public void renderWithUrl(String url, HummerRenderCallback callback) {
        if (TextUtils.isEmpty(url) || isDestroyed.get()) {
            return;
        }

        NetworkUtil.httpGet(url, (HttpCallback<String>) response -> {
            if (isDestroyed.get()) {
                if (callback != null) {
                    callback.onFailed(new RuntimeException("Page is destroyed!"));
                }
                return;
            }

            if (response == null) {
                if (callback != null) {
                    callback.onFailed(new RuntimeException("Http response is empty!"));
                }
                return;
            }

            if (response.error.code != 0) {
                if (callback != null) {
                    callback.onFailed(new RuntimeException(String.format("Http response error: %d, %s", response.error.code, response.error.msg)));
                }
                return;
            }

            render(response.data, url);

            if (callback != null) {
                if (getHummerContext().getJsPage() != null) {
                    callback.onSucceed(getHummerContext(), getHummerContext().getJsPage());
                } else {
                    callback.onFailed(new RuntimeException("Page is empty!"));
                }
            }
        });

        if (DebugUtil.isDebuggable()) {
            // 热更新
            if (hotLoader == null) {
                hotLoader = new HotLoader();
                hotLoader.connect(hmContext, url);
            }
        }
    }

    public void renderWithAssets(String assetsPath) {
        if (TextUtils.isEmpty(assetsPath) || isDestroyed.get()) {
            return;
        }

        if (assetsPath.startsWith("/")) {
            assetsPath = assetsPath.substring(1);
        }

        render(AssetsUtil.readFile(assetsPath), JsSourceUtil.JS_SOURCE_PREFIX_ASSETS + assetsPath);
    }

    public void renderWithFile(String jsFilePath) {
        if (TextUtils.isEmpty(jsFilePath) || isDestroyed.get()) {
            return;
        }

        if (jsFilePath.startsWith("/")) {
            jsFilePath = jsFilePath.substring(1);
        }

        render(FileUtil.readFile(jsFilePath), JsSourceUtil.JS_SOURCE_PREFIX_FILE + jsFilePath);
    }

    public void renderWithFile(File jsFile) {
        if (jsFile == null || !jsFile.exists() || isDestroyed.get()) {
            return;
        }
        render(FileUtil.readFile(jsFile), JsSourceUtil.JS_SOURCE_PREFIX_FILE + jsFile.getAbsolutePath());
    }

    /**
     * 向JS的Hummer域中注入Native参数
     *
     * @param key
     * @param data
     */
    public void setNativeDataToHummer(String key, Map<String, Object> data) {
        if (isDestroyed.get()) {
            return;
        }
        hmContext.getJsContext().getJSValue("Hummer").set(key, data);
    }

    /**
     * 设置JS页面打开时，前一个JS页面传递过来的参数
     *
     * @param page
     */
    public void setJsPageInfo(NavPage page) {
        if (isDestroyed.get()) {
            return;
        }
        hmContext.getJsContext().getJSValue("Hummer").set("pageInfo", page);
        hmContext.setJsSourcePath(page.url);
    }

    /**
     * 获取JS页面关闭时，当前JS页面需要返回到前一个页面的参数
     *
     * @return
     */
    public Map<String, Object> getJsPageResult() {
        if (isDestroyed.get()) {
            return null;
        }
        // 使用JSContext.getJSValue的方式目前拿不到Object类型的数据，所以只能先用下面的方法代替
//        return hmContext.getJsContext().getJSValue("Hummer").getJSValue("pageResult").jsonValueOf(new TypeToken<Map<String, Object>>(){}.getType());
        Object result = hmContext.evaluateJavaScript("JSON.stringify(Hummer.pageResult)");
        if (result instanceof String) {
            return HMGsonUtil.fromJson((String) result, new TypeToken<Map<String, Object>>(){}.getType());
        }
        return null;
    }

    /**
     * 获取JS页面关闭时，当前JS页面需要返回到前一个页面的参数，并转成Intent格式
     *
     * @return
     */
    public Intent getJsPageResultIntent() {
        Map<String, Object> result = getJsPageResult();
        if (result == null) {
            return null;
        }
        Intent intent = new Intent();
        for (String key : result.keySet()) {
            Object value = result.get(key);
            if (value instanceof Serializable) {
                intent.putExtra(key, (Serializable) value);
            } else if (value instanceof Parcelable) {
                intent.putExtra(key, (Parcelable) value);
            }
        }
        return intent;
    }

    /**
     * 给当前JS页面的根View注册Native方法（需要在render结束之后才能调用）
     *
     * @param funcName
     * @param callback
     */
    public void registerJsPageFunction(String funcName, ICallback callback) {
        if (isDestroyed.get()) {
            return;
        }
        hmContext.registerJSFunction(hmContext.getJsPage(), funcName, callback);
    }
}
