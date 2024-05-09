package floppaclient.ui.clickgui.advanced.elements.menu

import floppaclient.module.Module
import floppaclient.module.settings.impl.KeybindSetting
import floppaclient.ui.clickgui.advanced.AdvancedMenu
import floppaclient.ui.clickgui.advanced.elements.AdvancedElement
import floppaclient.ui.clickgui.advanced.elements.AdvancedElementType
import floppaclient.ui.clickgui.util.FontUtil
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

/**
 * Provides a key bind element.
 *
 * @author Aton
 */
class AdvancedElementKeyBind(parent: AdvancedMenu, module: Module, setting: KeybindSetting) :
    AdvancedElement<KeybindSetting>(parent, module, setting, AdvancedElementType.KEY_BIND) {

    private val keyBlackList = intArrayOf()

    /**
     * Render the element
     */
    override fun renderElement(mouseX: Int, mouseY: Int, partialTicks: Float): Int {
        val displayName = setting.name

        val keyName = if (module.keybinding!!.key > 0)
            Keyboard.getKeyName(module.keybinding.key) ?: "Err"
        else if (module.keybinding.key < 0)
            Mouse.getButtonName(module.keybinding.key + 100)
        else
            ".."
        val displayValue = "[$keyName]"

        // Rendering the text and the keybind.
        FontUtil.drawString(displayName, 1, 2, -0x1)
        FontUtil.drawString(displayValue, this.settingWidth - FontUtil.getStringWidth(displayValue), 2, -0x1)
        return this.settingHeight
    }

    /**
     * Handles mouse clicks for this element and returns true if an action was performed.
     * Used to interact with the element and to register mouse binds.
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int): Boolean {
        if (mouseButton == 0 && isCheckHovered(mouseX, mouseY)) {
            listening = !listening
            return true
        } else if (listening) {
            module.keybinding!!.key = -100 + mouseButton
            listening = false
        }
        return super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Register key strokes. Used to set the key bind.
     */
    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (listening) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK) {
                module.keybinding!!.key = Keyboard.KEY_NONE
                listening = false
            } else if (keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_RETURN) {
                listening = false
            } else if (!keyBlackList.contains(keyCode)) {
                module.keybinding!!.key = keyCode
                listening = false
            }
            return true
        }
        return super.keyTyped(typedChar, keyCode)
    }

    /**
     * Checks whether this element is hovered
     */
    private fun isCheckHovered(mouseX: Int, mouseY: Int): Boolean {
        return mouseX >= parent.x + x && mouseX <= parent.x + x + settingWidth && mouseY >= parent.y + y && mouseY <= parent.y + y + settingHeight
    }
}