/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.ar.sceneform.samples.augmentedimage.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.AugmentedImage
import com.google.ar.core.TrackingState
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.samples.augmentedimage.R
import com.google.ar.sceneform.samples.augmentedimage.app.common.augmentedimage.AugmentedImageNode
import com.google.ar.sceneform.samples.augmentedimage.app.utils.ToastUtil
import com.google.ar.sceneform.ux.ArFragment
import java.util.*

/**
 * This application demonstrates using augmented images to place anchor nodes. app to include image
 * tracking functionality.
 *
 *
 * In this example, we assume all images are static or moving slowly with a large occupation of
 * the screen. If the target is actively moving, we recommend to check
 * ArAugmentedImage_getTrackingMethod() and render only when the tracking method equals to
 * AR_AUGMENTED_IMAGE_TRACKING_METHOD_FULL_TRACKING. See details in [Recognize and Augment
 * Images](https://developers.google.com/ar/develop/c/augmented-images/).
 */
class AugmentedImageActivity : AppCompatActivity() {
    private var arFragment: ArFragment? = null
    private var fitToScanView: ImageView? = null

    // Augmented image and its associated center pose anchor, keyed by the augmented image in
    // the database.
    private val augmentedImageMap: MutableMap<AugmentedImage, AugmentedImageNode?> = HashMap()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_augmented_img)
        arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?
        fitToScanView = findViewById(R.id.image_view_fit_to_scan)
        arFragment!!.arSceneView.scene.addOnUpdateListener { frameTime: FrameTime -> onUpdateFrame(frameTime) }
    }

    override fun onResume() {
        super.onResume()
        if (augmentedImageMap.isEmpty()) {
            fitToScanView!!.visibility = View.VISIBLE
        }
    }

    /**
     * Registered with the Sceneform Scene object, this method is called at the start of each frame.
     *
     * @param frameTime - time since last frame.
     */
    private fun onUpdateFrame(frameTime: FrameTime) {
        val frame = arFragment!!.arSceneView.arFrame ?: return

        // If there is no frame, just return.
        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        for (augmentedImage in updatedAugmentedImages) {
            when (augmentedImage.trackingState) {
                TrackingState.PAUSED -> {
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    val text = "Detected Image augmentedImage.index = " + augmentedImage.index
//                  SnackbarHelper.getInstance().showMessage(this, text)
                    ToastUtil.showShortToast(text)
                }
                TrackingState.TRACKING -> {
                    // Have to switch to UI Thread to update View.
                    fitToScanView!!.visibility = View.GONE

                    // Create a new anchor for newly found images.
                    if (!augmentedImageMap.containsKey(augmentedImage)) {
                        val node = AugmentedImageNode(this)
                        node.image = augmentedImage
                        augmentedImageMap[augmentedImage] = node
                        arFragment!!.arSceneView.scene.addChild(node)
                    }
                }
                TrackingState.STOPPED -> augmentedImageMap.remove(augmentedImage)
            }
        }
    }
}