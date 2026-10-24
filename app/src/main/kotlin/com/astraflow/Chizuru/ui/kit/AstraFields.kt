@file:OptIn(ExperimentalCupertinoApi::class)

package com.astraflow.Chizuru.ui.kit

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import zone.ien.hig.CupertinoBorderedTextField
import zone.ien.hig.CupertinoSearchTextField
import zone.ien.hig.CupertinoSearchTextFieldDefaults
import zone.ien.hig.CupertinoText
import zone.ien.hig.ExperimentalCupertinoApi

/*
 * 输入框、搜索框原样用上游 compose-hig 的(样子、光标、清除按钮都是上游的);这里只接上框上方的标题、提示文字、密码遮挡。
 */


/** 输入框:上游的圆角输入框,没有字时显示灰色提示,[label] 写在框上方。[password] 为真时用圆点遮住。 */
@Composable
fun AstraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    password: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: (() -> Unit)? = null,
    textStyle: TextStyle = Astra.type.body,
) {
    FieldWithLabel(label, modifier) {
        CupertinoBorderedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            textStyle = textStyle,
            placeholder = placeholder?.let { { CupertinoText(it, maxLines = if (singleLine) 1 else Int.MAX_VALUE) } },
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else keyboardType,
                imeAction = imeAction,
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() }),
            singleLine = singleLine,
            minLines = minLines,
            contentAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        )
    }
}

/**
 * 输入框的另一种写法:内容放在 [state] 里,别处可以往光标处插字(例如模板编辑器的占位符)。
 * 样子与上面的输入框相同;[label] 写在框上方。
 */
@Composable
fun AstraTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    onImeAction: (() -> Unit)? = null,
    textStyle: TextStyle = Astra.type.body,
) {
    FieldWithLabel(label, modifier) {
        CupertinoBorderedTextField(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            textStyle = textStyle,
            lineLimits = if (singleLine) TextFieldLineLimits.SingleLine else TextFieldLineLimits.MultiLine(),
            placeholder = placeholder?.let { { CupertinoText(it, maxLines = if (singleLine) 1 else Int.MAX_VALUE) } },
            keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
            onKeyboardAction = { onImeAction?.invoke() },
            contentAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        )
    }
}

@Composable
private fun FieldWithLabel(label: String?, modifier: Modifier, field: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (label != null) AstraText(label, style = Astra.type.footnote, color = Astra.colors.labelSecondary)
        field()
    }
}

/** 搜索框:上游的搜索框,左边放大镜,打字时右边出现「取消」。 */
@Composable
fun AstraSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    onSearch: (() -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    CupertinoSearchTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        paddingValues = PaddingValues(0.dp),
        keyboardActions = KeyboardActions(onSearch = { onSearch?.invoke() }),
        interactionSource = source,
        placeholder = { CupertinoText(placeholder, maxLines = 1) },
        cancelButton = {
            CupertinoSearchTextFieldDefaults.cancelButton(
                onValueChange = onValueChange,
                interactionSource = source,
            ) {
                CupertinoText("取消")
            }
        },
    )
}
