package com.google.ar.sceneform.samples.augmentedimage.ui.activity

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.ar.core.*
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene.OnUpdateListener
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ExternalTexture
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.samples.augmentedimage.R
import com.google.ar.sceneform.samples.augmentedimage.app.common.helpers.CameraPermissionHelper
import com.google.ar.sceneform.samples.augmentedimage.app.ext.*
import com.google.ar.sceneform.samples.augmentedimage.app.utils.ToastUtil
import com.google.ar.sceneform.samples.augmentedimage.data.Constants
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.BaseArFragment
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.ref.WeakReference
import java.util.*
import java.util.function.Consumer


class MainActivity : AppCompatActivity() {

    var mArFragment: ArFragment? = null
    private var mTigerRenderable: Renderable? = null
    private var mSimpleImgRenderable: Renderable? = null
    private var mVideoNode: Node? = null //最新添加的视频节点

    private var mIsShowOptions = false

    //key 是识别出来的图像，value 是以该图像为锚点添加的节点
    val mAugmentedImageAddedNodeMap: HashMap<AugmentedImage, Node> = HashMap<AugmentedImage, Node>()

    //key 是识别出来的图像索引(第几个图像)，value 是给该图像添加的锚点节点
    val mAugmentedImageAnchorNodeMap: HashMap<Int, AnchorNode> = HashMap()

    //--- video 相关---
    private var mTigerVideoRenderable: ModelRenderable? = null
    private var mTigerMediaPlayer: MediaPlayer? = null
    private var mTigerVideoTexture: ExternalTexture? = null

    private var mToysVideoRenderable: ModelRenderable? = null
    private var mToysMediaPlayer: MediaPlayer? = null
    private var mToysVideoTexture: ExternalTexture? = null

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
        if (mAugmentedImageAddedNodeMap.isEmpty()) {
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
        mArFragment?.arSceneView?.scene?.addOnUpdateListener(OnUpdateListener { frameTime: FrameTime -> this.onUpdateFrame(frameTime) })
        mArFragment?.setOnTapArPlaneListener(object : BaseArFragment.OnTapArPlaneListener { //暂时识别不了
            override fun onTapPlane(hitResult: HitResult, plane: Plane?, motionEvent: MotionEvent?) {
                ToastUtil.showShortToast("点击了平面")
//                if (getAnchorNode() == null) {
//                    getAnchorNode() = addAnchorOnTap(hitResult)
//                }
            }
        })

        btn_open_argument_img.setOnClickListener {
            startActivity(Intent(this@MainActivity, AugmentedImageActivity::class.java))
        }

        btn_show_hide_options.setOnClickListener {
            mIsShowOptions = !mIsShowOptions
            con_options_parent.visibility = if (mIsShowOptions) View.VISIBLE else View.GONE
            btn_show_hide_options.text = if (mIsShowOptions) "不显示选项" else "显示选项"
        }

        btn_alter_plane.setOnClickListener {
            alterPlane(R.drawable.transparent_texture)
        }

        btn_add_node_gltf.setOnClickListener {
            ToastUtil.showShortToast("暂时不支持添加 gltf 资源")
//            addTigerNode()
        }

        btn_add_node_img.setOnClickListener {
            addSimpleImgNode()
        }

        btn_add_light.setOnClickListener {
            addLight(getAnchorNode())
        }

        btn_add_tiger_node_video.setOnClickListener {//添加 mp4 视频
            mVideoNode = addVideo(getAnchorNode(), mTigerVideoRenderable, mTigerMediaPlayer, mTigerVideoTexture)
        }

        btn_add_node_video_toys.setOnClickListener {
            mVideoNode = addVideo(getAnchorNode(), mToysVideoRenderable, mToysMediaPlayer, mToysVideoTexture)
        }

        btn_see_cur_scene_nodes.setOnClickListener {
            val curSceneChildNodes = mArFragment?.arSceneView?.scene?.children?.size
            ToastUtil.showShortToast("curSceneChildNodes.size=$curSceneChildNodes")
        }

        btn_rotate_anchor.setOnClickListener {
            rotateVideoNode()
        }
    }

    private fun getAnchorNode(): AnchorNode? {
        //默认取第 1 个检测到的图片的锚点
        val index = 0
        Log.d(TAG, "getAnchorNode 取第 $index 个检测到的图片的锚点")
        if (mAugmentedImageAnchorNodeMap.isNotEmpty()) {
            return mAugmentedImageAnchorNodeMap[index]
        }
        return null
    }

    private fun addSimpleImgNode() {
        addOneNode(getAnchorNode(), mSimpleImgRenderable!!)
        ToastUtil.showShortToast("添加了一个简单图片节点")
    }

//    /**
//     * 点击平面时添加一个锚点，之后所有节点用这一个锚点
//     */
//    private fun addAnchorOnTap(hitResult: HitResult): AnchorNode {
//        //父子关系: child -> Parent
//        // tigerTitleNode -> TransformableNode 的 model -> anchorNode -> arFragment.getArSceneView().getScene()
//        // Create the Anchor.
//        val anchor = hitResult.createAnchor()
//        val anchorNode = AnchorNode(anchor)
//        anchorNode.setParent(mArFragment!!.arSceneView.scene)
//
//        ToastUtil.showShortToast("添加锚点成功")
//
//        return anchorNode
//    }

    private fun rotateVideoNode() {
        if (mVideoNode == null) {
            ToastUtil.showShortToast("请添加 mp4 视频结点")
            return
        }
        mVideoNode?.localRotation = com.google.ar.sceneform.math.Quaternion(Vector3(1f, 0f, 0f), -270f) //-270f 旋转 180 度，0f 和 -180f 扁了
    }

    private fun addTigerNode() {
        if (mTigerRenderable == null) {
            ToastUtil.showShortToast("老虎 gltf 资源加载失败，无法添加节点")
            return
        }
        addOneNode(getAnchorNode(), mTigerRenderable!!)
        ToastUtil.showShortToast("添加了一个老虎节点")
    }

    private fun loadResource() {
        loadGlbRes()
        loadSimpleImgRes()
        loadTigerVideoRes()
        loadToysVideoRes()
    }

    private fun loadGlbRes() {
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

    private fun loadSimpleImgRes() {
        //添加简单图片资源
        ViewRenderable.builder()
                .setView(this, R.layout.layout_simple_img) //
                .build()
                .thenAccept(Consumer { renderable: ViewRenderable -> mSimpleImgRenderable = renderable })
    }

    private fun loadTigerVideoRes() {
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
        mTigerMediaPlayer = MediaPlayer.create(this, R.raw.lion_chroma)
        mTigerMediaPlayer?.setSurface(mTigerVideoTexture!!.surface)
        mTigerMediaPlayer?.isLooping = true

        ModelRenderable.builder()
                .setSource(this, R.raw.chroma_key_video)
                .build()
                .thenAccept { renderable: ModelRenderable ->
                    mTigerVideoRenderable = renderable
                    renderable.material.setExternalTexture("videoTexture", mTigerVideoTexture)
                    renderable.material.setFloat4("keyColor", Constants.CHROMA_KEY_COLOR)
                }
                .exceptionally { throwable: Throwable? ->
                    ToastUtil.showShortToast("Unable to load video renderable")
                    null
                }
    }

    private fun loadToysVideoRes() {
        mToysVideoTexture = ExternalTexture()

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
                    renderable.material.setFloat4("keyColor", Constants.CHROMA_KEY_COLOR)
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
        for (augmentedImage: AugmentedImage in updatedAugmentedImages) {
            when (augmentedImage.trackingState) {
                TrackingState.PAUSED -> {
                    Log.d(TAG, "--- TrackingState.PAUSED ---")
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    tv_msg_detect.text = "检测图片成功 !"
                    val text = "检测到图像，索引 augmentedImage.index = " + augmentedImage.index + " augmentedImage.name=" + augmentedImage.name
                    ToastUtil.showShortToast(text)
                }
                TrackingState.TRACKING -> {
                    Log.d(TAG, "--- TrackingState.TRACKING ---")
                    ToastUtil.showShortToast("检测图片轨迹成功 !")
                    // Have to switch to UI Thread to update View.
                    iv_scan.visibility = View.GONE

                    // Create a new anchor for newly found images.
                    if (!mAugmentedImageAddedNodeMap.containsKey(augmentedImage)) {
                        Log.d(TAG, "onUpdateFrame  !mAugmentedImageMap.containsKey(augmentedImage)")
                        //添加 Anchor 到图片中心
                        kotlin.runCatching {
                            addAnchorToDetectedImgCenter(augmentedImage)
                        }.onFailure {
                            ToastUtil.showShortToast("onUpdateFrame err msg =" + it.message)
                        }
                        //添加检测到的图像的四个角
                        addArgumentedImgNode(augmentedImage)
                    }
                }
                TrackingState.STOPPED -> {
                    Log.d(TAG, "--- TrackingState.STOPPED ---")
                    mAugmentedImageAnchorNodeMap.remove(augmentedImage.index)
                    mAugmentedImageAddedNodeMap.remove(augmentedImage)
                }
            }
        }
    }

    companion object {
        val TAG = MainActivity::class.java.simpleName
        private const val MIN_OPENGL_VERSION = 3.0
    }
}
