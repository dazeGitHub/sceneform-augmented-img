package com.google.ar.sceneform.samples.augmentedimage.app.utils

import com.hjq.toast.ToastUtils

object ToastUtil {

    @JvmStatic
    fun showShortToast(text: CharSequence?) {
        ToastUtils.show(text)
    }
}