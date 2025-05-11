package pw.binom.dto

import android.view.View
import android.view.View.MeasureSpec

fun View.redraw() {
    post {
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
        layout(left, top, right, bottom)
    }
}