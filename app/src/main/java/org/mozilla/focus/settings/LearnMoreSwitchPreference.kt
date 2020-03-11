package org.mozilla.focus.settings

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity

abstract class LearnMoreSwitchPreference(context: Context?, attrs: AttributeSet?) :
        SwitchPreferenceCompat(context, attrs) {

    init {
        layoutResource = R.layout.preference_switch_learn_more
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        getDescription()?.let {
            val summaryView = holder!!.findViewById(android.R.id.summary) as TextView
            summaryView.text = it
            summaryView.visibility = View.VISIBLE
        }

        val learnMoreLink = holder!!.findViewById(R.id.link) as TextView
        learnMoreLink.paintFlags = learnMoreLink.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        learnMoreLink.setTextColor(ContextCompat.getColor(context, R.color.colorAction))
        learnMoreLink.setOnClickListener {
            // This is a hardcoded link: if we ever end up needing more of these links, we should
            // move the link into an xml parameter, but there's no advantage to making it configurable now.
            val intent = InfoActivity.getIntentFor(context!!,
                    getLearnMoreUrl(), getPreferenceTitle())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            context.startActivity(intent)
        }

        val backgroundDrawableArray =
                context.obtainStyledAttributes(intArrayOf(R.attr.selectableItemBackground))
        val backgroundDrawable = backgroundDrawableArray.getDrawable(0)
        backgroundDrawableArray.recycle()
        learnMoreLink.background = backgroundDrawable
    }

    open fun getDescription(): String? = null

    abstract fun getLearnMoreUrl(): String

    abstract fun getPreferenceTitle(): String
}
