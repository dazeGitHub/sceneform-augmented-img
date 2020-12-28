package com.google.ar.sceneform.samples.augmentedimage.ui.activity

import android.content.Intent
import android.graphics.SurfaceTexture
import android.graphics.SurfaceTexture.OnFrameAvailableListener
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.*
import com.google.ar.core.Pose.makeTranslation
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene.OnUpdateListener
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.*
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.samples.augmentedimage.R
import com.google.ar.sceneform.samples.augmentedimage.common.augmentedimage.AugmentedImageNode
import com.google.ar.sceneform.samples.augmentedimage.common.helpers.CameraPermissionHelper
import com.google.ar.sceneform.samples.augmentedimage.common.utils.ToastUtil
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference
import java.util.*
import java.util.function.Consumer


class MainActivity : AppCompatActivity() {

    private var mArFragment: ArFragment? = null
    private var mTigerRenderable: Renderable? = null
    private var mSimpleImgRenderable: Renderable? = null
    private var mAnchorNode: AnchorNode? = null
    private var mVideoNode: Node? = null //最新添加的视频节点

    private var mIsShowOptions = false

    // Augmented image and its associated center pose anchor, keyed by the augmented image in
    // the database.
    private val augmentedImageMap: HashMap<AugmentedImage, AugmentedImageNode> = HashMap<AugmentedImage, AugmentedImageNode>()
    private var mSingleAugmentedImage: AugmentedImage? = null

    //--- video 相关---
    private var mTigerVideoRenderable: ModelRenderable? = null
    private var mTigerMediaPlayer: MediaPlayer? = null
    private var mTigerVideoTexture: ExternalTexture? = null

    private var mToysVideoRenderable: ModelRenderable? = null
    private var mToysMediaPlayer: MediaPlayer? = null
    private var mToysVideoTexture: ExternalTexture? = null

    // The color to filter out of the video.
    private val CHROMA_KEY_COLOR = Color(0.1843f, 1.0f, 0.098f)

    // Controls the height of the video in world space.
    private val VIDEO_HEIGHT_METERS = 0.25f //0.85f

    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
            return
        }

        setContentView(R.layout.activity_main)
        mArFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment1) as ArFragment?
        initData()
    }

    override fun onResume() {
        super.onResume()
        if (augmentedImageMap.isEmpty()) {
            iv_scan.visibility = View.VISIBLE
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            results: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions!!, results!!)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            // Use toast instead of snackbar here since the activity will exit.
            ToastUtil.showShortToast("Camera permission is needed to run this application")
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        } else {
            setContentView(R.layout.activity_main)
            mArFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment1) as ArFragment?
            initData()
        }
    }

    private fun initData() {
//        val result: Boolean = ArCheckUtils.checkDeviceSupportAr(this) //AugmentImageFragment 里已经判断了
//        if (result) {
        loadResource()
        initListener()
//        }
    }

    private fun initListener() {
        mArFragment!!.getArSceneView().getScene().addOnUpdateListener(OnUpdateListener { frameTime: FrameTime -> this.onUpdateFrame(frameTime) })

//        mArFragment!!.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent? ->
//            if (mAnchor == null) {
//                mAnchor = addAnchor(hitResult)
//            }
//        }

        btn_open_argument_img.setOnClickListener {
            startActivity(Intent(this@MainActivity, AugmentedImageActivity::class.java))
        }

        btn_show_hide_options.setOnClickListener {
            mIsShowOptions = !mIsShowOptions
            con_options_parent.visibility = if (mIsShowOptions) View.VISIBLE else View.GONE
            btn_show_hide_options.text = if (mIsShowOptions) "不显示选项" else "显示选项"
        }

        btn_alter_plane.setOnClickListener {
            alterPlane()
        }

        btn_add_node_gltf.setOnClickListener {
            ToastUtil.showShortToast("暂时不支持添加 gltf 资源")
//            addTigerNode()
        }

        btn_add_node_img.setOnClickListener {
            addSimpleImgNode()
        }

        btn_add_light.setOnClickListener {
            addLight()
        }

        btn_add_tiger_node_video.setOnClickListener {//添加 mp4 视频
            addVideo(mTigerVideoRenderable, mTigerMediaPlayer, mTigerVideoTexture)
        }

        btn_add_node_video_toys.setOnClickListener{
            addVideo(mToysVideoRenderable, mToysMediaPlayer, mToysVideoTexture)
        }

        btn_see_cur_scene_nodes.setOnClickListener {
            val curSceneChildNodes = mArFragment?.arSceneView?.scene?.children?.size
            ToastUtil.showShortToast("curSceneChildNodes.size=$curSceneChildNodes")
        }

        btn_rotate_anchor.setOnClickListener {
            rotateVideoNode()
        }
    }

    private fun addVideo(videoRenderable: ModelRenderable?, mediaPlayer: MediaPlayer?, externalTexture: ExternalTexture?) {
        if (videoRenderable == null) {
            ToastUtil.showShortToast("videoRenderable 是空的，无法添加资源")
            return
        }
        // Create a node to render the video and add it to the anchor.

        // Create a node to render the video and add it to the anchor.
        mVideoNode = Node()
        mVideoNode?.setParent(mAnchorNode)

        ToastUtil.showShortToast("添加了一个视频节点")

        // Set the scale of the node so that the aspect ratio of the video is correct.

        // Set the scale of the node so that the aspect ratio of the video is correct.
        val videoWidth = mediaPlayer!!.videoWidth.toFloat()
        val videoHeight = mediaPlayer!!.videoHeight.toFloat()
        mVideoNode?.localScale = Vector3(VIDEO_HEIGHT_METERS * (videoWidth / videoHeight), VIDEO_HEIGHT_METERS, 1.0f)

        mVideoNode?.localRotation = com.google.ar.sceneform.math.Quaternion(Vector3(1f, 0f, 0f), -90f) //0 1 2 / 0 0 0 是竖向扁的，0 1 0 是横向扁的，1 0 0 是正常的正面的
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
                                OnFrameAvailableListener { surfaceTexture: SurfaceTexture? ->
                                    mVideoNode?.renderable = videoRenderable
                                    it.surfaceTexture.setOnFrameAvailableListener(null)
                                })
            }
        } else {
            mVideoNode?.renderable = videoRenderable
        }
    }

    private fun addOneNode(anchor: AnchorNode?, renderable: Renderable) {
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

    private fun addSimpleImgNode() {
        addOneNode(mAnchorNode, mSimpleImgRenderable!!)
        ToastUtil.showShortToast("添加了一个简单图片节点")
    }

    /**
     * 点击平面时添加一个锚点，之后所有节点用这一个锚点
     */
    private fun addAnchor(hitResult: HitResult): AnchorNode {
        //父子关系: child -> Parent
        // tigerTitleNode -> TransformableNode 的 model -> anchorNode -> arFragment.getArSceneView().getScene()
        // Create the Anchor.
        val anchor = hitResult.createAnchor()
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(mArFragment!!.arSceneView.scene)

        ToastUtil.showShortToast("添加锚点成功")

        return anchorNode
    }

    private fun rotateVideoNode() {
        if (mVideoNode == null) {
            ToastUtil.showShortToast("请添加 mp4 视频结点")
            return
        }
        mVideoNode?.localRotation = com.google.ar.sceneform.math.Quaternion(Vector3(1f, 0f, 0f), -270f) //-270f 旋转 180 度，0f 和 -180f 扁了
    }

    /**
     * 测试四元数
     * lastParam ??
     */
    private fun testQuaternion(x: Int, y: Int, z: Int, lastParam: Int) {
        if (mAnchorNode == null) {
            ToastUtil.showShortToast("mAnchorNode 是空的")
            return
        }
//        var newPose = Pose.makeRotation(x, y, z, lastParam)

        var nodeList = mArFragment!!.arSceneView.scene.children
        if (nodeList.isNotEmpty()) {
            //把第 0 个节点移除
//            mArFragment!!.arSceneView.scene.removeChild(nodeList.get(0))

            //包含 X、Y、Z
            val translationArray = mSingleAugmentedImage?.centerPose?.translation
            //返回一个四元数
            val rotationQuaternion = mSingleAugmentedImage?.centerPose?.rotationQuaternion

            Log.d(TAG, "translationArray=" + translationArray + "rotationQuaternion =$rotationQuaternion")

            var transX = 0f
            var transY = 0f
            var transZ = 0f
            var transPos: Pose = makeTranslation(transX, transY, transZ)

//            mSingleAugmentedImage?.centerPose?.rotateVector(floatArrayOf(x.toFloat()), y, floatArrayOf(z.toFloat()), lastParam)
//            mSingleAugmentedImage?.createAnchor()
//            addAnchorToScene()
        }
    }

    /**
     * 向 Scene 中添加一个锚点
     */
    private fun addAnchorToScene(anchor: Anchor) {
        val anchorNode = AnchorNode(anchor)
        anchorNode.setParent(mArFragment!!.arSceneView.scene)
        mAnchorNode = anchorNode
    }

    private fun addTigerNode() {
        if (mTigerRenderable == null) {
            ToastUtil.showShortToast("老虎 gltf 资源加载失败，无法添加节点")
            return
        }
        addOneNode(mAnchorNode, mTigerRenderable!!)
        ToastUtil.showShortToast("添加了一个老虎节点")
    }

    private fun addLight() {
        if (mAnchorNode == null) {
            ToastUtil.showShortToast("锚点为空，请点击平面添加锚点")
            return
        }

        //添加一个聚光灯
        val myLight = Light.builder(Light.Type.DIRECTIONAL)
                .setColor(Color(0xffff00))
                .setShadowCastingEnabled(true)
                .build()

        ToastUtil.showShortToast("为锚点添加了一个灯光")

        mAnchorNode?.light = myLight
    }

    private fun alterPlane() {
        //修改平面
        val sampler: Texture.Sampler = Texture.Sampler.builder()
                .setMagFilter(Texture.Sampler.MagFilter.LINEAR)
                .setWrapMode(Texture.Sampler.WrapMode.REPEAT)
                .build()

        Texture.builder()
                .setSource(this, R.drawable.transparent_texture)
                .setSampler(sampler)
                .build()
                .thenAccept { texture ->
                    mArFragment!!.arSceneView.getPlaneRenderer()
                            .material
                            .get()
                            .setTexture(PlaneRenderer.MATERIAL_TEXTURE, texture)
                }.exceptionally { throwable: Throwable? ->
                    ToastUtil.showShortToast("set Texture failed" + throwable?.message)
                    Log.e(TAG, "set Texture failed" + throwable?.message)
                    null
                }
    }

    private fun loadResource() {
        loadGlbRes()
        loadSimpleImgRes()
        loadTigerVideoRes()
        loadToysVideoRes()
    }

    private fun loadGlbRes(){
        val weakActivity = WeakReference(this)

        //添加老虎 glb 资源   报错 java.util.concurrent.CompletionException: java.lang.AssertionError: No RCB file at uri: https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb
        //glb 文件在线查看: https://techbrood.com/tool?p=gltf-viewer，根据如下的 glb 链接下载 glb 文件后上传到该网站查看
//        ModelRenderable.builder()
//                .setSource(this, Uri.parse("https://storage.googleapis.com/ar-answers-in-search-models/static/Tiger/model.glb"))
////                .setIsFilamentGltf(true)
//                .build()
//                .thenAccept { modelRenderable: ModelRenderable? ->
//                    val activity = weakActivity.get()
//                    if (activity != null) {
//                        activity.mTigerRenderable = modelRenderable
//                    }
//                }
//                .exceptionally { throwable: Throwable? ->
//                    ToastUtil.showShortToast("Unable to load Tiger renderable" + throwable?.message)
//                    null
//                }

    }

    private fun loadSimpleImgRes(){
        //添加简单图片资源
        ViewRenderable.builder()
                .setView(this, R.layout.layout_simple_img) //
                .build()
                .thenAccept(Consumer { renderable: ViewRenderable -> mSimpleImgRenderable = renderable })
    }

    private fun loadTigerVideoRes(){
        //添加视频资源
        //
        // Create a renderable with a material that has a parameter of type 'samplerExternal' so that
        // it can display an ExternalTexture. The material also has an implementation of a chroma key
        // filter.

        // Create a renderable with a material that has a parameter of type 'samplerExternal' so that
        // it can display an ExternalTexture. The material also has an implementation of a chroma key
        // filter.
        // Create an ExternalTexture for displaying the contents of the video.

        // Create an ExternalTexture for displaying the contents of the video.
        mTigerVideoTexture = ExternalTexture()

        // Create an Android MediaPlayer to capture the video on the external texture's surface.

        // Create an Android MediaPlayer to capture the video on the external texture's surface.
        mTigerMediaPlayer = MediaPlayer.create(this, R.raw.lion_chroma)
        mTigerMediaPlayer?.setSurface(mTigerVideoTexture!!.surface)
        mTigerMediaPlayer?.isLooping = true

        ModelRenderable.builder()
                .setSource(this, R.raw.chroma_key_video)
                .build()
                .thenAccept { renderable: ModelRenderable ->
                    mTigerVideoRenderable = renderable
                    renderable.material.setExternalTexture("videoTexture", mTigerVideoTexture)
                    renderable.material.setFloat4("keyColor", CHROMA_KEY_COLOR)
                }
                .exceptionally { throwable: Throwable? ->
                    ToastUtil.showShortToast("Unable to load video renderable")
                    null
                }
    }

    private fun loadToysVideoRes(){
        mToysVideoTexture = ExternalTexture()

        // Create an Android MediaPlayer to capture the video on the external texture's surface.

        // Create an Android MediaPlayer to capture the video on the external texture's surface.
        mToysMediaPlayer = MediaPlayer.create(this, R.raw.toys)
        mToysMediaPlayer?.setSurface(mToysVideoTexture!!.surface)
        mToysMediaPlayer?.isLooping = true

        ModelRenderable.builder()
                .setSource(this, R.raw.chroma_key_video)
                .build()
                .thenAccept { renderable: ModelRenderable ->
                    mToysVideoRenderable = renderable
                    renderable.material.setExternalTexture("videoTexture", mToysVideoTexture)
                    renderable.material.setFloat4("keyColor", CHROMA_KEY_COLOR)
                }
                .exceptionally { throwable: Throwable? ->
                    ToastUtil.showShortToast("Unable to load video renderable")
                    null
                }
    }

    /**
     * Registered with the Sceneform Scene object, this method is called at the start of each frame.
     *
     * @param frameTime - time since last frame.
     */
    private fun onUpdateFrame(frameTime: FrameTime) {
        val frame: Frame = mArFragment?.arSceneView?.arFrame ?: return

        // If there is no frame, just return.
        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)
        for (augmentedImage in updatedAugmentedImages) {
            when (augmentedImage.trackingState) {
                TrackingState.PAUSED -> {
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    tv_msg_detect.text = "检测图片成功 !"
                    val text = "检测到图像，索引 augmentedImage.index = " + augmentedImage.index
                    ToastUtil.showShortToast(text)
                }
                TrackingState.TRACKING -> {
                    // Have to switch to UI Thread to update View.
                    iv_scan.visibility = View.GONE

                    // Create a new anchor for newly found images.
                    if (!augmentedImageMap.containsKey(augmentedImage)) {
                        //添加 Anchor 到图片中心
                        kotlin.runCatching {
                            addAnchorToScene(augmentedImage.createAnchor(augmentedImage.centerPose))
                        }.onFailure {
                            ToastUtil.showShortToast("onUpdateFrame err msg =" + it.message)
                        }
                        if (mSingleAugmentedImage == null) {
                            mSingleAugmentedImage = augmentedImage
                        }
                        //添加检测到的图像的四个角，是 mArFragment?.arSceneView?.scene 的第二个元素
                        val node = AugmentedImageNode(this)
                        node.image = augmentedImage
                        augmentedImageMap.put(augmentedImage, node)
                        mArFragment?.arSceneView?.scene?.addChild(node)
                    }
                }
                TrackingState.STOPPED -> augmentedImageMap.remove(augmentedImage)
            }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val MIN_OPENGL_VERSION = 3.0
    }
}
