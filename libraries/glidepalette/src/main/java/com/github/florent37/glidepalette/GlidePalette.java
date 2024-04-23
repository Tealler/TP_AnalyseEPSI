package com.github.florent37.glidepalette;

import android.graphics.Bitmap;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

public class GlidePalette<ModelType, T> extends BitmapPalette implements RequestListener<ModelType, T> {

    protected RequestListener<ModelType, T> callback;

    protected GlidePalette() {
    }

    public static GlidePalette with(String url) {
        GlidePalette glidePalette = new GlidePalette();
        glidePalette.url = url;
        return glidePalette;
    }

    public GlidePalette use(@Profile int paletteProfile) {
        super.use(paletteProfile);
        return this;
    }

    public GlidePalette<ModelType, T> intoBackground(View view) {
        return this.intoBackground(view, Swatch.RGB);
    }

    @Override
    public GlidePalette<ModelType, T> intoBackground(View view, @Swatch int paletteSwatch) {
        super.intoBackground(view, paletteSwatch);
        return this;
    }

    public GlidePalette<ModelType, T> intoTextColor(TextView textView) {
        return this.intoTextColor(textView, Swatch.TITLE_TEXT_COLOR);
    }

    @Override
    public GlidePalette<ModelType, T> intoTextColor(TextView textView, @Swatch int paletteSwatch) {
        super.intoTextColor(textView, paletteSwatch);
        return this;
    }

    @Override
    public GlidePalette<ModelType, T> crossfade(boolean crossfade) {
        super.crossfade(crossfade);
        return this;
    }

    @Override
    public GlidePalette<ModelType, T> crossfade(boolean crossfade, int crossfadeSpeed) {
        super.crossfade(crossfade, crossfadeSpeed);
        return this;
    }

    public GlidePalette<ModelType, T> setGlideListener(RequestListener<ModelType, T> listener) {
        this.callback = listener;
        return this;
    }

    @Override
    public GlidePalette<ModelType, T> intoCallBack(GlidePalette.CallBack callBack) {
        super.intoCallBack(callBack);
        return this;
    }

    @Override
    public GlidePalette<ModelType, T> setPaletteBuilderInterceptor(PaletteBuilderInterceptor interceptor) {
        super.setPaletteBuilderInterceptor(interceptor);
        return this;
    }

    @Override
    public GlidePalette<ModelType, T> skipPaletteCache(boolean skipCache) {
        super.skipPaletteCache(skipCache);
        return this;
    }

    @Override
    public boolean onException(Exception e, ModelType model, Target<T> target, boolean isFirstResource) {
        return this.callback != null && this.callback.onException(e, model, target, isFirstResource);
    }

    @Override
    public boolean onResourceReady(T resource, ModelType model, Target<T> target, boolean isFromMemoryCache, boolean isFirstResource) {
        boolean callbackResult = this.callback != null && this.callback.onResourceReady(resource, model, target, isFromMemoryCache, isFirstResource);

        Bitmap b = null;
        if (resource instanceof Bitmap) {
            b = (Bitmap) resource;
        } else if (resource instanceof GlideBitmapDrawable) {
            b = ((GlideBitmapDrawable) resource).getBitmap();
        } else if (target instanceof BitmapHolder) {
            b = ((BitmapHolder) target).getBitmap();
        }

        if (b != null) {
            start(b);
        }

        return callbackResult;
    }

    public interface BitmapHolder {
        @Nullable
        Bitmap getBitmap();
    }

}
