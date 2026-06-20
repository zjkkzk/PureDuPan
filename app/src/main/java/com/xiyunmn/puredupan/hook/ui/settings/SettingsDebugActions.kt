package com.xiyunmn.puredupan.hook.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.xiyunmn.puredupan.hook.core.XposedCompat
import com.xiyunmn.puredupan.hook.settings.registry.SettingsDexKitState
import com.xiyunmn.puredupan.hook.settings.registry.SettingsHostState
import com.xiyunmn.puredupan.hook.settings.registry.SettingsUserState
import com.xiyunmn.puredupan.hook.ui.UiStyle
import com.xiyunmn.puredupan.hook.ui.UiText

internal object SettingsDebugActions {
    fun showDexKitStatusDialog(context: Context) {
        try {
            if (!SettingsHostState.showDexKitStatusInSettings(context)) {
                XposedCompat.logW("[SettingsDebugActions] DexKit status skipped: unsupported host")
                return
            }
            val density = context.resources.displayMetrics.density
            val padding = (16 * density).toInt()
            val tokens = UiStyle.tokens(context)
            val statuses = SettingsDexKitState.statusViews(context)
            val summary = SettingsDexKitState.summaryText(context)

            val root = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(padding, padding, padding, padding / 2)
            }
            root.addView(TextView(context).apply {
                text = "${UiText.Settings.DEXKIT_STATUS_SUMMARY_PREFIX}: $summary"
                textSize = 14f
                setTextColor(tokens.accent)
                typeface = Typeface.DEFAULT_BOLD
                includeFontPadding = false
                setPadding(0, 0, 0, (10 * density).toInt())
            })

            statuses.forEachIndexed { index, item ->
                if (index > 0) {
                    root.addView(View(context).apply {
                        setBackgroundColor(tokens.divider)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            1,
                        ).apply {
                            setMargins(0, (10 * density).toInt(), 0, (10 * density).toInt())
                        }
                    })
                }
                root.addView(createDexKitStatusRow(context, item))
            }

            val scrollView = ScrollView(context).apply {
                addView(
                    root,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    ),
                )
            }

            AlertDialog.Builder(context, SettingsDialogWindows.themeFor(context))
                .setTitle(UiText.Settings.DEXKIT_STATUS_TITLE)
                .setView(scrollView)
                .setPositiveButton(android.R.string.ok, null)
                .show()
                .window
                ?.let { window ->
                    SettingsDialogWindows.applyCardStyle(
                        window = window,
                        density = window.context.resources.displayMetrics.density,
                        maxWidthDp = 360f,
                        horizontalMarginDp = 28f,
                    )
                }
        } catch (t: Throwable) {
            XposedCompat.logW("[SettingsDebugActions] showDexKitStatusDialog failed: ${t.message}")
        }
    }

    fun showClearLogsConfirmDialog(context: Context) {
        try {
            val path = XposedCompat.logDirectoryPath(context)
            val message = buildString {
                append(UiText.Settings.CLEAR_LOGS_CONFIRM_MESSAGE)
                if (path.isNotBlank()) {
                    append("\n\n")
                    append(path)
                }
            }
            AlertDialog.Builder(context, SettingsDialogWindows.themeFor(context))
                .setTitle(UiText.Settings.CLEAR_LOGS_CONFIRM_TITLE)
                .setMessage(message)
                .setNegativeButton(UiText.Settings.BUTTON_CANCEL, null)
                .setPositiveButton(UiText.Settings.ACTION_ICON_CLEAR) { _, _ ->
                    val result = XposedCompat.clearLogFiles(context)
                    val text = if (result.success) {
                        UiText.Settings.CLEAR_LOGS_SUCCESS
                    } else {
                        "${UiText.Settings.CLEAR_LOGS_FAILED}：${result.failedCount}"
                    }
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                }
                .show()
                .window
                ?.let { window ->
                    SettingsDialogWindows.applyCardStyle(
                        window = window,
                        density = window.context.resources.displayMetrics.density,
                        maxWidthDp = 360f,
                        horizontalMarginDp = 28f,
                    )
                }
        } catch (t: Throwable) {
            Toast.makeText(context, UiText.Settings.CLEAR_LOGS_FAILED, Toast.LENGTH_SHORT).show()
            XposedCompat.logW("[SettingsDebugActions] showClearLogsConfirmDialog failed: ${t.message}")
        }
    }

    fun showResetModuleSettingsConfirmDialog(context: Context, onRestart: () -> Unit) {
        try {
            AlertDialog.Builder(context, SettingsDialogWindows.themeFor(context))
                .setTitle(UiText.Settings.RESET_MODULE_SETTINGS_CONFIRM_TITLE)
                .setMessage(UiText.Settings.RESET_MODULE_SETTINGS_CONFIRM_MESSAGE)
                .setNegativeButton(UiText.Settings.BUTTON_CANCEL, null)
                .setPositiveButton(UiText.Settings.ACTION_ICON_RESET) { _, _ ->
                    val success = SettingsUserState.resetUserSettings(context)
                    val text = if (success) {
                        UiText.Settings.RESET_MODULE_SETTINGS_SUCCESS
                    } else {
                        UiText.Settings.RESET_MODULE_SETTINGS_FAILED
                    }
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    if (success) {
                        onRestart()
                    }
                }
                .show()
                .window
                ?.let { window ->
                    SettingsDialogWindows.applyCardStyle(
                        window = window,
                        density = window.context.resources.displayMetrics.density,
                        maxWidthDp = 360f,
                        horizontalMarginDp = 28f,
                    )
                }
        } catch (t: Throwable) {
            Toast.makeText(context, UiText.Settings.RESET_MODULE_SETTINGS_FAILED, Toast.LENGTH_SHORT).show()
            XposedCompat.logW("[SettingsDebugActions] showResetModuleSettingsConfirmDialog failed: ${t.message}")
        }
    }

    private fun createDexKitStatusRow(
        context: Context,
        item: SettingsDexKitState.TargetStatusView,
    ): View {
        val density = context.resources.displayMetrics.density
        val tokens = UiStyle.tokens(context)
        val stateText = when (item.state) {
            "success" -> UiText.Settings.DEXKIT_STATUS_SUCCESS
            "not_found" -> UiText.Settings.DEXKIT_STATUS_NOT_FOUND
            "error" -> UiText.Settings.DEXKIT_STATUS_ERROR
            "scanning" -> UiText.Settings.DEXKIT_STATUS_SCANNING
            else -> UiText.Settings.DEXKIT_STATUS_PENDING
        }
        val stateColor = when (item.state) {
            "success" -> tokens.accent
            "error", "not_found" -> 0xFFE66A5E.toInt()
            "scanning" -> 0xFFF0A03A.toInt()
            else -> tokens.textMuted
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(TextView(context).apply {
                    text = item.id
                    textSize = 14f
                    setTextColor(tokens.textPrimary)
                    typeface = Typeface.DEFAULT_BOLD
                    includeFontPadding = false
                }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(TextView(context).apply {
                    text = stateText
                    textSize = 13f
                    setTextColor(stateColor)
                    typeface = Typeface.DEFAULT_BOLD
                    gravity = Gravity.END
                    includeFontPadding = false
                })
            })
            addView(TextView(context).apply {
                text = "${UiText.Settings.DEXKIT_STATUS_TARGET}: ${item.target}"
                textSize = 12.5f
                setTextColor(tokens.textSecondary)
                includeFontPadding = false
                setPadding(0, (6 * density).toInt(), 0, 0)
            })
            addView(TextView(context).apply {
                text = "${UiText.Settings.DEXKIT_STATUS_FEATURE}: ${item.feature}"
                textSize = 12.5f
                setTextColor(tokens.textSecondary)
                includeFontPadding = false
                setPadding(0, (3 * density).toInt(), 0, 0)
            })
            if (!item.detail.isNullOrBlank()) {
                addView(TextView(context).apply {
                    text = item.detail
                    textSize = 12f
                    setTextColor(tokens.textMuted)
                    includeFontPadding = false
                    setPadding(0, (3 * density).toInt(), 0, 0)
                })
            }
        }
    }
}
