package com.etheria.game.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2

/**
 * Slices a grid sprite sheet into row-based LibGDX animations.
 */
class SpriteSheetAnimator(
    texture: Texture,
    frameWidth: Int,
    frameHeight: Int,
    rowNames: List<String>,
    frameDuration: Float = 0.14f
) {
    private val animations = mutableMapOf<String, Animation<TextureRegion>>()

    init {
        val grid = TextureRegion.split(texture, frameWidth, frameHeight)
        rowNames.forEachIndexed { r, name ->
            if (r < grid.size) {
                val frames = com.badlogic.gdx.utils.Array<TextureRegion>()
                grid[r].forEach { frames.add(it) }
                val mode = if (name == "death" || name == "hurt" || name == "attack" || name == "heavy") {
                    Animation.PlayMode.NORMAL
                } else {
                    Animation.PlayMode.LOOP
                }
                animations[name] = Animation(frameDuration, frames, mode)
            }
        }
    }

    fun getFrame(animName: String, stateTime: Float, loop: Boolean = true): TextureRegion {
        val anim = animations[animName] ?: animations["idle"] ?: animations.values.first()
        return anim.getKeyFrame(stateTime, loop)
    }
}

object TextureFactory {
    fun solidColor(width: Int, height: Int, color: Color): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val tex = Texture(pixmap)
        pixmap.dispose()
        return tex
    }
}

class ObjectPool<T>(private val factory: () -> T, private val reset: (T) -> Unit) {
    private val pool = ArrayDeque<T>()

    fun obtain(): T = if (pool.isEmpty()) factory() else pool.removeFirst()

    fun free(obj: T) {
        reset(obj)
        pool.addLast(obj)
    }
}

class CameraShake {
    var intensity = 0f
    var duration = 0f
    val offset = Vector2()

    fun shake(intensity: Float, duration: Float) {
        this.intensity = maxOf(this.intensity, intensity)
        this.duration = maxOf(this.duration, duration)
    }

    fun update(delta: Float): Vector2 {
        return if (duration > 0f) {
            duration -= delta
            offset.x = (Math.random().toFloat() * 2f - 1f) * intensity
            offset.y = (Math.random().toFloat() * 2f - 1f) * intensity
            offset
        } else {
            intensity = 0f
            offset.setZero()
        }
    }
}
