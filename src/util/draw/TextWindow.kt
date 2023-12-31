package util.draw

import java.awt.Font
import javax.swing.JFrame
import javax.swing.JLabel

class TextWindow : JFrame() {
    private val label: JLabel = JLabel("label").apply {
        font = Font("Monospaced", Font.PLAIN, 14)
    }

    init {
        title = "debug window"
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(400, 300)

        // Add label to the frame
        contentPane.add(label)
    }

    fun updateText(text: String) {
        // Update the text in the label
        label.text = text
        // Refresh the window
        repaint()
    }
}

object Draw {
    private val textWindow by lazy {
        TextWindow().apply {
            isVisible = true
        }
    }
    fun updateText(text: String) {
        textWindow.updateText(text)
    }
}
