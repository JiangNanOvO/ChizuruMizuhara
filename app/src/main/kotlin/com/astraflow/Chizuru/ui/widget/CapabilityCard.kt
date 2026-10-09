package com.astraflow.Chizuru.ui.widget

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.astraflow.Chizuru.R

class CapabilityCard(
    private val ui: UIKit,
    private val context: Context
) {

    class Handle(
        val root: LinearLayout,
        val switch: GlassSwitch,
        val statusText: TextView,
        val content: LinearLayout
    )

    fun build(
        iconRes: Int,
        tintRes: Int,
        title: CharSequence,
        subtitle: CharSequence,
        checked: Boolean,
        onToggle: (Boolean) -> Unit,
        onOpen: () -> Unit
    ): Handle {
        val card = ui.glassCard()
        card.setPadding(ui.dp(14), ui.dp(14), ui.dp(14), ui.dp(14))

        val head = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        head.addView(ui.iconBadge(iconRes, tintRes))

        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(14), 0, ui.dp(10), 0)
        }
        val status = ui.text(subtitle, 12f, R.color.text_sub)
        col.addView(ui.text(title, 16.5f, R.color.text, bold = true))
        col.addView(status.apply { setPadding(0, ui.dp(5), 0, 0) })
        head.addView(col, ui.weight())

        val switch = GlassSwitch(context).apply {
            setChecked(checked, animate = false)
            onCheckedChange = { value ->
                onToggle(value)
                applyDim(card, value)
            }
        }
        head.addView(switch)

        card.addView(head)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
        }
        card.addView(content)

        card.addView(buildOpenHint())

        card.isClickable = true

        
        card.setOnClickListener { onOpen() }
        Motion.pressFeedback(card)

        applyDim(card, checked)

        return Handle(card, switch, status, content)
    }

    private fun buildOpenHint(): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL or Gravity.END
        setPadding(0, ui.dp(10), ui.dp(2), 0)

        addView(
            ui.text(context.getString(R.string.ui_card_tap_hint), 11.5f, R.color.text_hint)
        )
        addView(ui.chevron().apply {
            layoutParams = LinearLayout.LayoutParams(ui.dp(16), ui.dp(16)).apply {
                marginStart = ui.dp(2)
            }
        })
    }

    private fun applyDim(card: View, enabled: Boolean) {
        card.animate()
            .alpha(if (enabled) 1f else 0.55f)
            .setDuration(180)
            .start()
    }
}
