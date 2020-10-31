package ru.skillbranch.skillarticles.extensions

fun List<Pair<Int, Int>>.groupByBounds(bounds: List<Pair<Int, Int>>): List<MutableList<Pair<Int, Int>>> {
    val list = mutableListOf<MutableList<Pair<Int, Int>>>()
    bounds.forEach {
        val boundlist = mutableListOf<Pair<Int, Int>>()
        val (startBound, endBound) = it
        this.forEach {
            val (start, end) = it
            if (start >= startBound && end <= endBound) boundlist.add(it)
        }
        list.add(boundlist)
    }
    return list
}