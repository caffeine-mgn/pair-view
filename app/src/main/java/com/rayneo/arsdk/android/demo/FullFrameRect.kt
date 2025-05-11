package com.rayneo.arsdk.android.demo


/**
 * @param program The program to use.  FullFrameRect takes ownership, and will release
 * the program when no longer needed.
 */
class FullFrameRect(program: Texture2dProgram) {
    private val mRectDrawable = Drawable2d(Drawable2d.Prefab.FULL_RECTANGLE)
    private var mProgram: Texture2dProgram? = program

    /**
     * Releases resources.
     *
     *
     * This must be called with the appropriate EGL context current (i.e. the one that was
     * current when the constructor was called).  If we're about to destroy the EGL context,
     * there's no value in having the caller make it current just to do this cleanup, so you
     * can pass a flag that will tell this function to skip any EGL-context-specific cleanup.
     */
    fun release(doEglCleanup: Boolean) {
        if (doEglCleanup) {
            mProgram?.release()
        }
        mProgram = null
    }

    /**
     * Returns the program currently in use.
     */
    fun getProgram(): Texture2dProgram? = mProgram

    /**
     * Changes the program.  The previous program will be released.
     *
     *
     * The appropriate EGL context must be current.
     */
    fun changeProgram(program: Texture2dProgram?) {
        mProgram?.release()
        mProgram = program
    }

    /**
     * Creates a texture object suitable for use with drawFrame().
     */
    fun createTextureObject():Int {
        val mProgram = mProgram
        checkNotNull(mProgram) { "FrameRect already closed" }
        return mProgram.createTextureObject()
    }

    /**
     * Draws a viewport-filling rect, texturing it with the specified texture object.
     */
    fun drawFrame(textureId: Int, texMatrix: FloatArray) {
        val mProgram = mProgram
        checkNotNull(mProgram) { "FrameRect already closed" }
        // Use the identity matrix for MVP so our 2x2 FULL_RECTANGLE covers the viewport.
        mProgram.draw(
            mvpMatrix = GlUtil.IDENTITY_MATRIX,
            vertexBuffer = mRectDrawable.mVertexArray,
            firstVertex = 0,
            vertexCount = mRectDrawable.mVertexCount,
            coordsPerVertex = mRectDrawable.mCoordsPerVertex,
            vertexStride = mRectDrawable.mVertexStride,
            texMatrix = texMatrix,
            texBuffer = mRectDrawable.mTexCoordArray,
            textureId = textureId,
            texStride = mRectDrawable.mTexCoordStride,
        )
    }
}