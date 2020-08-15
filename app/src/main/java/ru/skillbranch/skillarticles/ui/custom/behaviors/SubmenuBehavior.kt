package ru.skillbranch.skillarticles.ui.custom.behaviors

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginRight
import ru.skillbranch.skillarticles.ui.custom.ArticleSubmenu
import ru.skillbranch.skillarticles.ui.custom.Bottombar
import kotlin.math.max
import kotlin.math.min

class SubmenuBehavior(): CoordinatorLayout.Behavior<ArticleSubmenu>() {

    constructor(context: Context, attributeSet: AttributeSet): this()

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: ArticleSubmenu,
        dependency: View
    ): Boolean {
        return dependency is Bottombar
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: ArticleSubmenu,
        dependency: View
    ): Boolean {
        return if (child.isOpen && dependency.translationY >= 0f) {
            animate(child, dependency)
            true
        } else false
    }

    private fun animate(child: View, dependency: View) {
        val fraction = dependency.translationY / dependency.height
        child.translationX = (child.width + child.marginRight) * fraction
    }

//    override fun onStartNestedScroll(
//        coordinatorLayout: CoordinatorLayout,
//        child: ArticleSubmenu,
//        directTargetChild: View,
//        target: View,
//        axes: Int,
//        type: Int
//    ): Boolean {
//        super.onStartNestedScroll(
//            coordinatorLayout,
//            child,
//            directTargetChild,
//            target,
//            axes,
//            type
//        )
//        return axes == ViewCompat.SCROLL_AXIS_VERTICAL
//    }
//
//    override fun onNestedPreScroll(
//        coordinatorLayout: CoordinatorLayout,
//        child: ArticleSubmenu,
//        target: View,
//        dx: Int,
//        dy: Int,
//        consumed: IntArray,
//        type: Int
//    ) {
//        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)
//        child.translationY = max(0f, min(child.height.toFloat() + child.marginBottom.toFloat(), child.translationY + dy))
//    }
}