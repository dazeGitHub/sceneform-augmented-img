package com.google.ar.sceneform.samples.augmentedimage.app.test

import android.util.Log
import com.google.ar.core.Pose
import com.google.ar.sceneform.samples.augmentedimage.ui.activity.MainActivity

/**
 * <pre>
 *     author : ZYZ
 *     e-mail : zyz163mail@163.com
 *     time   : 2020/12/28
 *     desc   :
 *     version: 1.0
 * </pre>
 */

/**
 * 测试四元数
 * lastParam ??
 */
//private fun testQuaternion(x: Int, y: Int, z: Int, lastParam: Int) {
//    if (mAnchorNode == null) {
//        ToastUtil.showShortToast("mAnchorNode 是空的")
//        return
//    }
////        var newPose = Pose.makeRotation(x, y, z, lastParam)
//
//    var nodeList = mArFragment!!.arSceneView.scene.children
//    if (nodeList.isNotEmpty()) {
//        //把第 0 个节点移除
////            mArFragment!!.arSceneView.scene.removeChild(nodeList.get(0))
//
//        //包含 X、Y、Z
//        val translationArray = mSingleAugmentedImage?.centerPose?.translation
//        //返回一个四元数
//        val rotationQuaternion = mSingleAugmentedImage?.centerPose?.rotationQuaternion
//
//        Log.d(MainActivity.TAG, "translationArray=" + translationArray + "rotationQuaternion =$rotationQuaternion")
//
//        var transX = 0f
//        var transY = 0f
//        var transZ = 0f
//        var transPos: Pose = Pose.makeTranslation(transX, transY, transZ)
//
////            mSingleAugmentedImage?.centerPose?.rotateVector(floatArrayOf(x.toFloat()), y, floatArrayOf(z.toFloat()), lastParam)
////            mSingleAugmentedImage?.createAnchor()
////            addAnchorToScene()
//    }
//}