package com.idestudio.app.ui.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

class CollapsingToolbarState(
    val toolbarHeightPx: Float
) {
    var toolbarOffsetPx = Animatable(0f)
        private set

    val isCollapsed: Boolean get() = toolbarOffsetPx.value <= -toolbarHeightPx + 1f
    val isExpanded: Boolean get() = toolbarOffsetPx.value >= -1f

    suspend fun expand() {
        toolbarOffsetPx.animateTo(0f, animationSpec = tween(200))
    }

    suspend fun collapse() {
        toolbarOffsetPx.animateTo(-toolbarHeightPx, animationSpec = tween(200))
    }
}

@Composable
fun rememberCollapsingToolbarConnection(
    toolbarHeightPx: Float,
    state: CollapsingToolbarState
): NestedScrollConnection {
    val scope = rememberCoroutineScope()

    return remember(toolbarHeightPx, state) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = (state.toolbarOffsetPx.value + delta).coerceIn(-toolbarHeightPx, 0f)
                val consumed = newOffset - state.toolbarOffsetPx.value

                scope.launch {
                    state.toolbarOffsetPx.snapTo(newOffset)
                }

                return Offset(0f, consumed)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // Snap to nearest state when scroll finishes
                if (state.toolbarOffsetPx.value < -toolbarHeightPx / 2f) {
                    state.collapse()
                } else {
                    state.expand()
                }
                return super.onPostFling(consumed, available)
            }
        }
    }
}
