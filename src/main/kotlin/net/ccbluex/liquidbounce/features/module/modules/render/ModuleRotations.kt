/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleDerp
import net.ccbluex.liquidbounce.render.drawLineStrip
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.util.Pair

/**
 * Rotations module
 *
 * Allows you to see server-sided rotations.
 */

object ModuleRotations : Module("Rotations", Category.RENDER) {

    enum class BodyPart(
        override val choiceName: String,
        val head: Boolean,
        val body: Boolean
    ) : NamedChoice {
        BOTH("Both", true, true),
        HEAD("Head", true, false),
        BODY("Body", false, true)
    }

    val bodyParts by enumChoice("BodyParts", BodyPart.BOTH)
    private val showRotationVector by boolean("ShowRotationVector", false)
    private val smoothRotations by boolean("SmoothRotation", false)
    val pov by boolean("POV", false)

    var rotationPitch: Pair<Float, Float> = Pair(0f, 0f)

    private var lastRotation: Rotation? = null

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        if (!showRotationVector)
            return@handler

        val rotation = RotationManager.currentRotation ?: return@handler
        val camera = mc.gameRenderer.camera

        val eyeVector = Vec3(0.0, 0.0, 1.0)
            .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
            .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat())

        renderEnvironmentForWorld(matrixStack) {
            withColor(Color4b.WHITE) {
                drawLineStrip(eyeVector, eyeVector + Vec3(rotation.rotationVec * 100.0))
            }
        }
    }

    /**
     * Should server-side rotations be shown?
     */
    fun shouldDisplayRotations() = shouldSendCustomRotation() || ModuleFreeCam.shouldDisableRotations()

    /**
     * Should we even send a rotation if we use freeCam?
     */
    fun shouldSendCustomRotation(): Boolean {
        val special = arrayOf(ModuleDerp).any { it.enabled }

        return enabled && (RotationManager.currentRotation != null || special)
    }

    /**
     * Display case-represented rotations
     */
    fun displayRotations(): Rotation {
        val server = RotationManager.serverRotation
        val current = RotationManager.currentRotation

        // Apply smoothing if enabled
        if (smoothRotations && current != null && lastRotation != null) {
            val smoothedRotation = smoothRotation(lastRotation!!, current)
            lastRotation = smoothedRotation
            return smoothedRotation
        }

        lastRotation = current ?: server
        return current ?: server
    }

    /**
     * Rotation Smoothing
     */
    private fun smoothRotation(from: Rotation, to: Rotation): Rotation {
        val diffYaw = to.yaw - from.yaw
        val diffPitch = to.pitch - from.pitch
        val smoothingFactor = 0.25f

        val smoothedYaw = from.yaw + diffYaw * smoothingFactor
        val smoothedPitch = from.pitch + diffPitch * smoothingFactor

        return Rotation(smoothedYaw, smoothedPitch)
    }
}
