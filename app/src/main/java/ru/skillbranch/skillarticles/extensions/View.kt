package ru.skillbranch.skillarticles.extensions

import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.*

fun View.setMarginOptionally(left:Int = marginLeft, top : Int = marginTop, right : Int = marginRight, bottom : Int = marginBottom) {
//    val lp = CoordinatorLayout.LayoutParams(layoutParams)
//    lp.updateMargins(left, top, right, bottom)
//    layoutParams = lp
    (layoutParams as CoordinatorLayout.LayoutParams).setMargins(left, top, right, bottom)
}

fun View.setPaddingOptionally(
    left: Int = paddingLeft,
    top : Int = paddingTop,
    right : Int = paddingRight,
    bottom : Int = paddingBottom
) {
    setPadding(left, top, right, bottom)
}