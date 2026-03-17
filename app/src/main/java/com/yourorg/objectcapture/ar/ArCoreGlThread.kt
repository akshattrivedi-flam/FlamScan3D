package com.yourorg.objectcapture.ar

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.google.ar.core.Session
import com.google.ar.core.SharedCamera
import java.util.concurrent.Callable
import java.util.concurrent.FutureTask

class ArCoreGlThread {
    private val thread = HandlerThread("ArCoreGlThread")
    private var handler: Handler? = null

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE
    private var eglConfig: EGLConfig? = null
    private var textureId: Int = 0
    private var initialized = false

    fun reset() {
        initialized = false
        textureId = 0
    }

    fun <T> runWithGlContext(session: Session, sharedCamera: SharedCamera?, block: () -> T): T {
        ensureThread()
        val handler = handler ?: throw IllegalStateException("GL handler missing")
        if (handler.looper == Looper.myLooper()) {
            ensureEgl()
            ensureTexture(session, sharedCamera)
            return block()
        }
        val task = FutureTask(Callable {
            ensureEgl()
            ensureTexture(session, sharedCamera)
            block()
        })
        handler.post(task)
        return task.get()
    }

    fun setSharedCameraBufferSize(width: Int, height: Int, sharedCamera: SharedCamera?) {
        if (sharedCamera == null) return
        ensureThread()
        handler?.post {
            ensureEgl()
            try {
                sharedCamera.surfaceTexture.setDefaultBufferSize(width, height)
            } catch (_: Throwable) {
                // Ignore invalid buffer sizes or SurfaceTexture state errors.
            }
        }
    }

    private fun ensureThread() {
        if (handler != null) return
        thread.start()
        handler = Handler(thread.looper)
    }

    private fun ensureEgl() {
        if (initialized) {
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
            return
        }

        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("Unable to get EGL display")
        }

        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("Unable to initialize EGL")
        }

        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0
            )
        ) {
            throw RuntimeException("Unable to choose EGL config")
        }
        eglConfig = configs[0]

        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        eglContext = EGL14.eglCreateContext(
            eglDisplay, eglConfig, EGL14.EGL_NO_CONTEXT, contextAttribs, 0
        )
        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw RuntimeException("Unable to create EGL context")
        }

        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, 1,
            EGL14.EGL_HEIGHT, 1,
            EGL14.EGL_NONE
        )
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, eglConfig, surfaceAttribs, 0)
        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("Unable to create EGL surface")
        }

        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("Unable to make EGL context current")
        }

        initialized = true
    }

    private fun ensureTexture(session: Session, sharedCamera: SharedCamera?) {
        if (textureId != 0) return
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        textureId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )

        if (sharedCamera != null) {
            try {
                try {
                    sharedCamera.surfaceTexture.detachFromGLContext()
                } catch (_: Throwable) {
                    // Ignore detach failures; texture might not be attached yet.
                }
                sharedCamera.surfaceTexture.attachToGLContext(textureId)
            } catch (_: Throwable) {
                // Ignore attach failures; ARCore will throw if it cannot use the texture.
            }
        }
        session.setCameraTextureName(textureId)
    }
}
