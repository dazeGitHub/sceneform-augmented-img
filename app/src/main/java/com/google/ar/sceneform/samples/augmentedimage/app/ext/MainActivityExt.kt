package com.google.ar.sceneform.samples.augmentedimage.app.ext

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.Log
import android.view.MotionEvent
import com.google.ar.core.AugmentedImage
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.samples.augmentedimage.app.common.augmentedimage.AugmentedImageNode
import com.google.ar.sceneform.samples.augmentedimage.app.utils.ToastUtil
import com.google.ar.sceneform.samples.augmentedimage.data.Constants
import com.google.ar.sceneform.samples.augmentedimage.ui.activity.MainActivity
import com.google.ar.sceneform.ux.TransformableNode

/**
 * <pre>
 *     author : ZYZ
 *     e-mail : zyz163mail@163.com
 *     time   : 2020/12/28
 *     desc   :
 *     version: 1.0
 * </pre>
 */

fun MainActivity.alterPlane(drawableRes: Int) {
    //修改平面
    val sampler: Texture.Sampler = Texture.Sampler.builder()
            .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
            .setWrapMode(Texture.Sampler.WrapMode.REPEAT)
            .build()

    Texture.builder()
            .setSource(this, drawableRes) //R.drawable.transparent_texture
            .setSampler(sampler)
            .build()
            .thenAccept { texture ->
                mArFragment?.let{
                    it.arSceneView.getPlaneRenderer()
                        .material
                        .get()
                        .setTexture(PlaneRenderer.MATERIAL_TEXTURE, texture)
                }
            }.exceptionally { throwable: Throwable? ->
                ToastUtil.showShortToast("set Texture failed" + throwable?.message)
                Log.e(MainActivity.TAG, "set Texture failed" + throwable?.message)
                null
            }
}

fun MainActivity.addVideo(anchorNode : AnchorNode?, videoRenderable: ModelRenderable?, mediaPlayer: MediaPlayer?, externalTexture: ExternalTexture?): Node? {
    if (videoRenderable == null) {
        ToastUtil.showShortToast("videoRenderable 是空的，无法添加资源")
        return null
    }
    if(anchorNode == null){
        ToastUtil.showShortToast("锚点为空，无法 addVideo !")
        return null
    }
    // Create a node to render the video and add it to the anchor.

    // Create a node to render the video and add it to the anchor.
    val videoNode = Node()
    videoNode.setParent(anchorNode)

    ToastUtil.showShortToast("添加了一个视频节点")

    // Set the scale of the node so that the aspect ratio of the video is correct.

    // Set the scale of the node so that the aspect ratio of the video is correct.
    val videoWidth = mediaPlayer!!.videoWidth.toFloat()
    val videoHeight = mediaPlayer!!.videoHeight.toFloat()
    videoNode.localScale = Vector3(Constants.VIDEO_HEIGHT_METERS * (videoWidth / videoHeight), Constants.VIDEO_HEIGHT_METERS, 1.0f)

    videoNode.localRotation = com.google.ar.sceneform.math.Quaternion(Vector3(1f, 0f, 0f), -90f) //0 1 2 / 0 0 0 是竖向扁的，0 1 0 是横向扁的，1 0 0 是正常的正面的
    // Start playing the video when the first node is placed.

    // Start playing the video when the first node is placed.
    if (!mediaPlayer!!.isPlaying) {
        mediaPlayer!!.start()

        // Wait to set the renderable until the first frame of the  video becomes available.
        // This prevents the renderable from briefly appearing as a black quad before the video
        // plays.
        externalTexture?.let {
            it.surfaceTexture
                    .setOnFrameAvailableListener(
                            SurfaceTexture.OnFrameAvailableListener { surfaceTexture: SurfaceTexture? ->
                                videoNode.renderable = videoRenderable
                                it.surfaceTexture.setOnFrameAvailableListener(null)
                            })
        }
    } else {
        videoNode.renderable = videoRenderable
    }
    return videoNode
}


fun MainActivity.addOneNode(anchor: AnchorNode?, renderable: Renderable) {
    if (anchor == null) {
        ToastUtil.showShortToast("锚点不能为空")
        return
    }

    // Create the transformable model and add it to the anchor.
    val model = TransformableNode(mArFragment!!.transformationSystem)
    model.setParent(anchor)
    model.scaleController.maxScale = 0.2f
    model.scaleController.minScale = 0.1f
    model.renderable = renderable
    model.select()

    val simpleImgNode = Node()
    simpleImgNode.setParent(model)
    simpleImgNode.isEnabled = false
    simpleImgNode.localPosition = Vector3(0.0f, 1.0f, 0.0f)
}

fun MainActivity.addAnchorToDetectedImgCenter(augmentedImage : AugmentedImage){
    val anchorNode = AnchorNode(augmentedImage.createAnchor(augmentedImage.centerPose))
    anchorNode.setParent(mArFragment!!.arSceneView.scene)
    mAugmentedImageAnchorNodeMap.put(augmentedImage.index, anchorNode)
}

fun MainActivity.addArgumentedImgNode(augmentedImage : AugmentedImage){
    val node = AugmentedImageNode(this) // AugmentedImageNode 就是带 4 个角的图像节点
    node.setImage(augmentedImage)//为该节点添加 augmentedImage，并自动设置锚点为 augmentedImage 中央
    node.setOnTapListener { hitResult, motionEvent ->
        if(motionEvent?.action == MotionEvent.ACTION_UP){
            ToastUtil.showShortToast("点击了节点 augmentedImage index=" + augmentedImage.index + " name=" + augmentedImage.name)
        }
    }
    Log.d(MainActivity.TAG, "-- mAugmentedImageMap put node --, augmentedImage.name=" + augmentedImage.name)
    mAugmentedImageAddedNodeMap.put(augmentedImage, node)
    mArFragment?.arSceneView?.scene?.addChild(node)
}

fun addLight(anchor: AnchorNode?) {
    if (anchor == null) {
        ToastUtil.showShortToast("锚点为空，请点击平面添加锚点")
        return
    }

    //添加一个聚光灯
    val myLight = Light.builder(Light.Type.DIRECTIONAL)
            .setColor(Color(0xffff00))
            .setShadowCastingEnabled(true)
            .build()

    ToastUtil.showShortToast("为锚点添加了一个灯光")

    anchor.light = myLight
}

