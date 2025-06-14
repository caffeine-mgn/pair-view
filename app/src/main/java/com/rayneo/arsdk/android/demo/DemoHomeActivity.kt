package com.rayneo.arsdk.android.demo

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.rayneo.arsdk.android.core.make3DEffectForSide
import pw.binom.video.databinding.LayoutDemoHomeBinding
import com.rayneo.arsdk.android.demo.ui.activity.DialogActivity
import com.rayneo.arsdk.android.demo.ui.activity.FixedFocusPosRVActivity
import com.rayneo.arsdk.android.demo.ui.activity.FragmentDemoActivity
import com.rayneo.arsdk.android.demo.ui.activity.MovedFocusPosRVActivity
import com.rayneo.arsdk.android.demo.ui.activity.VideoPlayerActivity
import com.rayneo.arsdk.android.demo.ui.activity.MyTestActivity
import com.rayneo.arsdk.android.ui.toast.FToast
import com.rayneo.arsdk.android.ui.util.FixPosFocusTracker
import com.rayneo.arsdk.android.ui.util.FocusHolder
import com.rayneo.arsdk.android.ui.util.FocusInfo
import com.rayneo.arsdk.android.focus.reqFocus
import com.rayneo.arsdk.android.touch.TempleAction
import com.rayneo.arsdk.android.util.FLogger
import com.rayneo.arsdk.android.ui.activity.BaseMirrorActivity
import kotlinx.coroutines.launch
import pw.binom.glasses.NetworkService
import pw.binom.glasses.requestPermissions
import pw.binom.logger.Logger
import pw.binom.video.R

class DemoHomeActivity : BaseMirrorActivity<LayoutDemoHomeBinding>() {
    private var fixPosFocusTracker: FixPosFocusTracker? = null
    private val logger by Logger.ofThisOrGlobal
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissions(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
        )
        initFocusTarget()
        initEvent()

        NetworkService.start(this)
    }

    private fun initEvent() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                templeActionViewModel.state.collect {
                    FLogger.i("DemoActivity", "action = $it")
                    when (it) {
                        is TempleAction.DoubleClick -> {
//                            finish()
                            moveTaskToBack(false)
                        }

                        else -> fixPosFocusTracker?.handleFocusTargetEvent(it)
                    }
                }
            }
        }
    }

    private fun initFocusTarget() {
        val focusHolder = FocusHolder(true)
        mBindingPair.setLeft {
            val btn1Info = FocusInfo(
                btn1,
                eventHandler = { action ->
                    when (action) {
                        is TempleAction.Click -> {
                            FToast.show("bt1 click")
                        }

                        else -> Unit
                    }
                },
                focusChangeHandler = { hasFocus ->
                    mBindingPair.updateView {
                        triggerFocus(hasFocus, btn1, mBindingPair.checkIsLeft(this))
                    }
                }
            )
            focusHolder.addFocusTarget(
                btn1Info,
                FocusInfo(
                    btn2,
                    eventHandler = { action ->
                        when (action) {
                            is TempleAction.Click -> {
                                startActivity(
                                    Intent(
                                        this@DemoHomeActivity,
                                        DialogActivity::class.java
                                    )
                                )
                            }

                            else -> Unit
                        }
                    },
                    focusChangeHandler = { hasFocus ->
                        mBindingPair.updateView {
                            triggerFocus(hasFocus, btn2, mBindingPair.checkIsLeft(this))
                        }
                    }
                ),
                FocusInfo(
                    btn3,
                    eventHandler = { action ->
                        when (action) {
                            is TempleAction.Click -> {
                                startActivity(
                                    Intent(
                                        this@DemoHomeActivity,
                                        FixedFocusPosRVActivity::class.java
                                    )
                                )
                            }

                            else -> Unit
                        }
                    },
                    focusChangeHandler = { hasFocus ->
                        mBindingPair.updateView {
                            triggerFocus(hasFocus, btn3, mBindingPair.checkIsLeft(this))
                        }
                    }
                ),
                FocusInfo(
                    btn4,
                    eventHandler = { action ->
                        when (action) {
                            is TempleAction.Click -> {
                                startActivity(
                                    Intent(
                                        this@DemoHomeActivity,
                                        MovedFocusPosRVActivity::class.java
                                    )
                                )
                            }

                            else -> Unit
                        }
                    },
                    focusChangeHandler = { hasFocus ->
                        mBindingPair.updateView {
                            triggerFocus(hasFocus, btn4, mBindingPair.checkIsLeft(this))
                        }
                    }
                ),
                FocusInfo(
                    btn5,
                    eventHandler = { action ->
                        when (action) {
                            is TempleAction.Click -> {
                                startActivity(
                                    Intent(
                                        this@DemoHomeActivity,
                                        FragmentDemoActivity::class.java
                                    )
                                )
                            }

                            else -> Unit
                        }
                    },
                    focusChangeHandler = { hasFocus ->
                        mBindingPair.updateView {
                            triggerFocus(hasFocus, btn5, mBindingPair.checkIsLeft(this))
                        }
                    }
                ),
                FocusInfo(
                    startVideoBtn,
                    eventHandler = { action ->
                        when (action) {
                            is TempleAction.Click -> {
                                startActivity(
                                    Intent(
                                        this@DemoHomeActivity,
                                        MyTestActivity::class.java
                                    )
                                )
                            }

                            else -> Unit
                        }
                    },
                    focusChangeHandler = { hasFocus ->
                        mBindingPair.updateView {
                            triggerFocus(hasFocus, startVideoBtn, mBindingPair.checkIsLeft(this))
                        }
                    }
                ),
                FocusInfo(
                    testBtn,
                    eventHandler = { action ->
                        when (action) {
                            is TempleAction.Click -> {
                                startActivity(
                                    Intent(
                                        this@DemoHomeActivity,
                                        VideoPlayerActivity::class.java
                                    )
                                )
                            }

                            else -> Unit
                        }
                    },
                    focusChangeHandler = { hasFocus ->
                        mBindingPair.updateView {
                            triggerFocus(hasFocus, testBtn, mBindingPair.checkIsLeft(this))
                        }
                    }
                ),
            )
            focusHolder.currentFocus(mBindingPair.left.btn1)
        }

        fixPosFocusTracker = FixPosFocusTracker(focusHolder).apply {
            focusObj.reqFocus()
        }
    }


    private fun triggerFocus(hasFocus: Boolean, view: View, isLeft: Boolean) {
        view.setBackgroundColor(getColor(if (hasFocus) R.color.purple_200 else R.color.black))
        // 3D效果
        make3DEffectForSide(view, isLeft, hasFocus)
    }
}