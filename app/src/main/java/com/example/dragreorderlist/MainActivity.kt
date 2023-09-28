package com.example.dragreorderlist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.dragreorderlist.ui.theme.DragReorderListTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DragReorderListTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val listStrings = remember {
        mutableStateListOf<String>(
            "0",
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8",
            "9",
            "10",
        )
    }

    ReorderableList(models = listStrings)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    DragReorderListTheme {
        Greeting("Android")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderableList(models: SnapshotStateList<String>) {
    val lazyListState = rememberLazyListState()

    var initiallyDraggedElement by remember { mutableStateOf<LazyListItemInfo?>(null) }

    var currentIndexOfDraggedItem by remember { mutableStateOf<Int?>(null) }

    var calculatedOffset by remember { mutableStateOf(0f) }

    var draggedDistance by remember { mutableStateOf(0f) }

    val moveIndex = { fromIndex: Int, toIndex: Int -> models.moveAt(fromIndex, toIndex) }

    Column() {
        Text(text = "dragged distance = ${draggedDistance}")
        LazyColumn(
            modifier = Modifier.pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDrag = { change, offset ->
                        change.consume()
                        draggedDistance += offset.y
                        calculatedOffset += offset.y
                        initiallyDraggedElement?.let { initialElement ->
                            val startOffset = initialElement.offset + draggedDistance
                            val endOffset =
                                initialElement.offset + initialElement.size + draggedDistance
                            println(draggedDistance)
                            initialElement?.let { hovered ->
                                lazyListState.layoutInfo.visibleItemsInfo
                                    .filterNot { comparedItem ->
                                        comparedItem.offset + comparedItem.size < startOffset
                                                || comparedItem.offset > endOffset
                                    }
                                    .firstOrNull { comparedItem ->
                                        if (comparedItem.index == currentIndexOfDraggedItem) return@firstOrNull false
                                        when {
                                            draggedDistance > 0 ->
                                                endOffset > comparedItem.offset + comparedItem.size

                                            else -> startOffset < comparedItem.offset
                                        }
                                    }?.let { comparedItem ->
                                        //println("${comparedItem.index}")
                                        //println("${currentIndexOfDraggedItem}")
                                        //println("${initiallyDraggedElement?.index}")
                                        currentIndexOfDraggedItem?.let {
                                            moveIndex.invoke(it, comparedItem.index)
                                            currentIndexOfDraggedItem = comparedItem.index
                                            calculatedOffset = 0f
                                        }
                                    }
                            }
                        }
                    },
                    onDragStart = { offset ->
                        draggedDistance = 0f
                        calculatedOffset = 0f
                        lazyListState.layoutInfo.visibleItemsInfo
                            .firstOrNull { item -> offset.y.toInt() in item.offset..item.offset + item.size }
                            ?.let {
                                currentIndexOfDraggedItem = it.index
                                initiallyDraggedElement = it
                            }
                    },
                    onDragEnd = {
                        currentIndexOfDraggedItem = null
                        initiallyDraggedElement = null
                        draggedDistance = 0f
                        calculatedOffset = 0f
                    },
                    onDragCancel = {
                        currentIndexOfDraggedItem = null
                        initiallyDraggedElement = null
                        draggedDistance = 0f
                        calculatedOffset = 0f
                    }
                )
            },
            state = lazyListState,
        ) {
            itemsIndexed(models, key = { index, item -> item }) { index, item ->

                Column(
                    modifier = Modifier.graphicsLayer {
                        translationY =
                            calculatedOffset.takeIf { index == currentIndexOfDraggedItem } ?: 0f
                    }
                        .let {
                            if (index == currentIndexOfDraggedItem) it else it.animateItemPlacement()
                        }) {
                    Text(text = item, modifier = Modifier.padding(6.dp))
                    Divider()
                }
            }

        }
    }
}

/**
 * Moves the given item at the `oldIndex` to the `newIndex`
 */
fun <T> MutableList<T>.moveAt(oldIndex: Int, newIndex: Int) {
    val item = this[oldIndex]
    removeAt(oldIndex)
    add(newIndex, item)
}
