package com.tamakitech.yearprogress.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import com.tamakitech.yearprogress.MainActivity
import com.tamakitech.yearprogress.R
import com.tamakitech.yearprogress.YearProgress
import com.tamakitech.yearprogress.calcYearProgress
import kotlin.math.roundToInt

/**
 * ホーム画面ウィジェット。
 *
 * 更新の合図（updatePeriodMillis・時刻変更・リサイズ）を受けたその場で RemoteViews を組み立てて反映する。
 * WorkManager などの遅延実行を経由しないため、アプリをほとんど開かなくても描画が後回しにされない。
 */
open class YearWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { render(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        render(context, manager, appWidgetId)
    }

    /** レイアウトの種類と、それを使い始める最小サイズ（dp）。 */
    private enum class WidgetLayout(val minWidthDp: Float, val minHeightDp: Float) {
        /** 1×2（縦長） */
        SMALL(57f, 110f),

        /** 4×1（横長） */
        WIDE(180f, 40f),

        /** 4×4 */
        LARGE(250f, 250f),
    }

    companion object {
        /** 同じ描画を共有するプロバイダ（通常版・横長版）。 */
        private val PROVIDERS = listOf(YearWidgetProvider::class.java, YearWideWidgetProvider::class.java)

        /** 配置済みのすべてのウィジェットを即時に描き直す。 */
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            PROVIDERS.forEach { provider ->
                manager.getAppWidgetIds(ComponentName(context, provider)).forEach { render(context, manager, it) }
            }
        }

        private fun render(context: Context, manager: AppWidgetManager, appWidgetId: Int) {
            val progress = calcYearProgress()
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+：ランチャーが実寸に収まるもののうち、実寸に最も近いレイアウトを選ぶ
                RemoteViews(
                    WidgetLayout.entries.associate {
                        SizeF(it.minWidthDp, it.minHeightDp) to buildViews(context, progress, it)
                    },
                )
            } else {
                val options = manager.getAppWidgetOptions(appWidgetId)
                val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
                val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                buildViews(context, progress, layoutForLegacy(width, height))
            }
            manager.updateAppWidget(appWidgetId, views)
        }

        /** Android 11 以下：ウィジェットの寸法（dp）からレイアウトを決める。 */
        private fun layoutForLegacy(width: Int, height: Int): WidgetLayout = when {
            width >= WidgetLayout.LARGE.minWidthDp && height >= WidgetLayout.LARGE.minHeightDp -> WidgetLayout.LARGE
            height < WidgetLayout.SMALL.minHeightDp && width >= WidgetLayout.WIDE.minWidthDp -> WidgetLayout.WIDE
            else -> WidgetLayout.SMALL
        }

        private fun buildViews(context: Context, progress: YearProgress, size: WidgetLayout): RemoteViews {
            val layout = when (size) {
                WidgetLayout.SMALL -> R.layout.widget_small
                WidgetLayout.WIDE -> R.layout.widget_wide
                WidgetLayout.LARGE -> R.layout.widget_large
            }
            val vertical = size == WidgetLayout.SMALL
            val fill = when {
                vertical && progress.isLow -> R.drawable.widget_bar_fill_red_v
                vertical -> R.drawable.widget_bar_fill_green_v
                progress.isLow -> R.drawable.widget_bar_fill_red_h
                else -> R.drawable.widget_bar_fill_green_h
            }
            // ClipDrawable のレベル 0〜10000 = 残り割合
            val level = (progress.remainingRatio * 10_000).roundToInt().coerceIn(0, 10_000)

            return RemoteViews(context.packageName, layout).apply {
                setTextViewText(R.id.text_year, progress.year.toString())
                setTextViewText(R.id.text_percent, progress.percentText)
                setImageViewResource(R.id.bar_fill, fill)
                setInt(R.id.bar_fill, "setImageLevel", level)
                if (size != WidgetLayout.SMALL) {
                    setTextViewText(
                        R.id.text_days,
                        context.getString(R.string.remaining_days, progress.remainingDays.toInt()),
                    )
                }
                setOnClickPendingIntent(android.R.id.background, openAppIntent(context))
            }
        }

        private fun openAppIntent(context: Context): PendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
