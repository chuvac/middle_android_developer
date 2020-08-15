package ru.skillbranch.skillarticles.viewmodels.base

import android.os.Bundle

interface IViewModelstate {
    fun save(outState: Bundle)
    fun restore(savedState: Bundle): IViewModelstate
}