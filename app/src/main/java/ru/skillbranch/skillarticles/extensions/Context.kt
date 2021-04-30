package ru.skillbranch.skillarticles.extensions

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethod
import android.view.inputmethod.InputMethod.SHOW_EXPLICIT
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.forEach
import androidx.core.view.iterator
import androidx.navigation.NavDestination
import androidx.navigation.ui.onNavDestinationSelected
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.ui.delegates.AttrValue
import kotlin.reflect.KProperty

fun Context.dpToPx(dp: Int): Float {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        this.resources.displayMetrics

    )
}

fun Context.dpToIntPx(dp: Int): Int {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp.toFloat(),
        this.resources.displayMetrics
    ).toInt()
}

fun Context.hideKeyboard(view: View) {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.showKeyboard(etComment: EditText?) {
    val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.showSoftInput(etComment, InputMethodManager.SHOW_IMPLICIT)
}

val Context.isNetworkAvailable: Boolean
    get() {
        val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            cm.activeNetwork?.run {
                val nc = cm.getNetworkCapabilities(this)
                nc!!.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) || nc.hasTransport(
                    NetworkCapabilities.TRANSPORT_WIFI
                )
            } ?: false
        } else {
            cm.activeNetworkInfo?.run { isConnectedOrConnecting } ?: false
        }
    }

fun Context.attrValue(res: Int): Int {
    var value: Int? = null

    if (value == null) {
        val tv = TypedValue()
        if (theme.resolveAttribute(res, tv, true)) value = tv.data
        else throw Resources.NotFoundException("Resource with id $res not found")
    }
    return value!!
}

fun BottomNavigationView.selectDestination(destination: NavDestination) {

    menu.forEach {
        if (it.itemId == destination.id) it.isChecked = true
    }
}

fun BottomNavigationView.selectItem(itemId: Int?) {
    itemId ?: return
    for (item in menu.iterator()) {
        if (item.itemId == itemId) {
            item.isChecked = true
            break
        }
    }
}