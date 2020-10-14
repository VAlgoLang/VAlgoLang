package com.manimdsl.linearrepresentation

interface Flag

object FadeOut : Flag

object OldShape : Flag

class AnimationFlags(vararg activateFlags: Flag = emptyArray()) {
    private val flags: HashMap<Flag, Boolean> = hashMapOf(
        FadeOut to false,
        OldShape to false
    )

    init {
        activateFlags.forEach { flags[it] = true }
    }

    /** Array like access functions**/
    operator fun get(flag: Flag): Boolean = flags[flag] ?: false
    operator fun set(flag: Flag, set: Boolean) {
        flags[flag] = set
    }
}

