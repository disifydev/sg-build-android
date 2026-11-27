package ru.sguardo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body

/** Шапка формы как в Telegram: круглый крестик слева, название по центру, круглая галочка справа. */
@Composable
fun FormTop(title: String, onClose: () -> Unit, onDone: (() -> Unit)?) {
    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(Sg.c.cHigh).clickable(onClick = onClose), contentAlignment = Alignment.Center) { Ic("x", Sg.c.fg, size = 20.dp) }
        if (title.isNotEmpty()) T(title, body(17f, FontWeight.SemiBold), modifier = Modifier.align(Alignment.Center))
        if (onDone != null) Box(Modifier.align(Alignment.CenterEnd).size(44.dp).clip(CircleShape).background(Sg.c.primary).clickable(onClick = onDone), contentAlignment = Alignment.Center) {
            Ic("check", Sg.c.onPrimary, size = 20.dp)
        }
    }
}

/** Карточка формы: скруглённая группа полей. */
@Composable
fun FormCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Sg.c.cLow), content = content)
}

/** Поле в карточке: без рамки, с тонкой чертой сверху, если это не первое поле. */
@Composable
fun FormField(value: String, onChange: (String) -> Unit, placeholder: String, divider: Boolean = false, keyboard: KeyboardType = KeyboardType.Text,
              cap: KeyboardCapitalization = KeyboardCapitalization.Sentences, singleLine: Boolean = true, startInset: androidx.compose.ui.unit.Dp = 16.dp) {
    val hair = Sg.c.hair
    Box(Modifier.fillMaxWidth().heightIn(min = 54.dp)
        .drawBehind { if (divider) drawRect(hair, Offset(startInset.toPx(), 0f), Size(size.width - startInset.toPx(), .7.dp.toPx())) }
        .padding(start = startInset, end = 16.dp, top = 15.dp, bottom = 15.dp)) {
        if (value.isEmpty()) T(placeholder, body(16.5f), Sg.c.faint)
        BasicTextField(value, onChange, textStyle = body(16.5f).copy(color = Sg.c.fg), singleLine = singleLine, maxLines = if (singleLine) 1 else 6,
            cursorBrush = SolidColor(Sg.c.primary), keyboardOptions = KeyboardOptions(keyboardType = keyboard, capitalization = cap), modifier = Modifier.fillMaxWidth())
    }
}

/** Пояснение под карточкой формы. */
@Composable
fun FormHint(text: String) = T(text, body(13f).copy(lineHeight = androidx.compose.ui.unit.TextUnit(18f, androidx.compose.ui.unit.TextUnitType.Sp)), Sg.c.muted,
    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp))
