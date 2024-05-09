package floppaclient.module.settings.impl

import floppaclient.module.settings.Setting
import floppaclient.module.settings.Visibility
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class KeybindSetting(
    name: String,
    override val default: Keybinding,
    description: String,
    hidden: Visibility = Visibility.VISIBLE
) : Setting<Keybinding>(name, hidden, description) {

    override var value: Keybinding = default

    /**
     * Action to do, when keybinding is pressed
     *
     * Note: Action is always invoked, even if module isn't enabled.
     */
    fun onPress(block: () -> Unit): KeybindSetting {
        value.onPress = block
        return this
    }

    override fun reset() {
        value.key = default.key
    }
}

class Keybinding(var key: Int) {

    /**
     * Intended to active when keybind is pressed.
     */
    var onPress: (() -> Unit)? = null

    /**
     * @return `true` if [key] is held down.
     */
    fun isDown(): Boolean {
        return if (key == 0) false else (if (key < 0) Mouse.isButtonDown(key + 100) else Keyboard.isKeyDown(key))
    }
}