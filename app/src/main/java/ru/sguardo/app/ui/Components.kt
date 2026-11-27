package ru.sguardo.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.sguardo.app.R
import ru.sguardo.app.ui.theme.Onest
import ru.sguardo.app.ui.theme.Sg
import ru.sguardo.app.ui.theme.body
import ru.sguardo.app.ui.theme.disp

/** Кликабельность с рябью Material и сменой фигуры для ряби. */
fun Modifier.tap(shape: Shape? = null, enabled: Boolean = true, onClick: () -> Unit): Modifier =
    (if (shape != null) this.clip(shape) else this).clickable(enabled = enabled, onClick = onClick)

@Composable
fun T(text: String, style: TextStyle = body(15f), color: Color = Sg.c.fg, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE, align: TextAlign? = null) {
    Text(text, style = style, color = color, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis, textAlign = align)
}

/** Шапка вкладки (.atop): крупный заголовок Unbounded, справа иконки. */
@Composable
fun TopBar(title: String, scrolled: Boolean = false, actions: @Composable RowScope.() -> Unit = {}) {
    val bg = if (scrolled) Sg.c.c else Sg.c.surface
    Row(
        Modifier.fillMaxWidth().background(bg).windowInsetsPadding(WindowInsets.statusBars).height(64.dp).padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        T(title, disp(22), modifier = Modifier.weight(1f), maxLines = 1)
        actions()
    }
}

@Composable
fun SubTopBar(title: String, onBack: () -> Unit, scrolled: Boolean = false, backIcon: String = "arrowL", actions: @Composable RowScope.() -> Unit = {}) {
    val bg = if (scrolled) Sg.c.c else Sg.c.surface
    Row(
        Modifier.fillMaxWidth().background(bg).windowInsetsPadding(WindowInsets.statusBars).height(64.dp).padding(start = 4.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBtn(backIcon, onBack)
        FitText(title, disp(22), modifier = Modifier.weight(1f).padding(start = 4.dp))
        actions()
    }
}

@Composable
fun IconBtn(icon: String, onClick: () -> Unit, tint: Color = Sg.c.fg, size: Dp = 48.dp, iconSize: Dp = 22.dp, badge: Int = 0, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Ic(icon, tint, size = iconSize)
        if (badge > 0) Badge(badge, Modifier.align(Alignment.TopEnd).offset((-8).dp, 8.dp))
    }
}

@Composable
fun Badge(n: Int, modifier: Modifier = Modifier, border: Color? = null) {
    Box(
        modifier.heightIn(min = 16.dp).widthIn(min = 16.dp).let { if (border != null) it.border(1.5.dp, border, RoundedCornerShape(9.dp)) else it }
            .background(Sg.c.wine, RoundedCornerShape(9.dp)).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) { Text(n.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Onest) }
}

/** Кнопка кабинета в шапке: аватар «В». */
@Composable
fun MeButton(name: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Avatar(name.ifBlank { "В" }, 32.dp, color = Color(0xFFC7E2D3))
    }
}

/** Переключатель «Все / Непрочитанные / Ждут ответа» (.seg на Android): контур, бегунок, галочка у выбранного. Ширина кнопок — по тексту. */
@Composable
fun Seg(options: List<Triple<String, String, Int>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier, hpad: Dp = 16.dp) {
    val idx = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val weights = options.map { (_, l, n) -> l.length + 6f + (if (n > 0) 3.5f else 0f) }
    val total = weights.sum()
    BoxWithConstraints(
        modifier.fillMaxWidth().padding(horizontal = hpad).border(1.dp, Sg.c.outline, RoundedCornerShape(22.dp)).padding(3.dp)
    ) {
        val W = maxWidth
        val x by animateDpAsState(W * (weights.take(idx).sum() / total), spring(dampingRatio = 0.78f, stiffness = 900f), label = "segx")
        val w by animateDpAsState(W * (weights[idx] / total), spring(dampingRatio = 0.78f, stiffness = 900f), label = "segw")
        Box(Modifier.offset(x = x).width(w).height(36.dp).background(Sg.c.sc, RoundedCornerShape(18.dp)))
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, (k, label, n) ->
                val on = k == selected
                Row(
                    Modifier.weight(weights[i]).height(36.dp).clip(RoundedCornerShape(18.dp)).clickable { onSelect(k) }.padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedVisibility(on, enter = fadeIn(tween(120)) + expandHorizontally(tween(160)), exit = fadeOut(tween(80)) + shrinkHorizontally(tween(120))) {
                        Ic("check", if (on) Sg.c.onSc else Sg.c.fg, size = 15.dp, modifier = Modifier.padding(end = 5.dp))
                    }
                    T(label, body(12.5f, FontWeight.SemiBold), if (on) Sg.c.onSc else Sg.c.fg, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                    if (n > 0) {
                        Spacer(Modifier.width(5.dp))
                        Box(Modifier.heightIn(min = 18.dp).widthIn(min = 18.dp).background(if (on) Sg.c.onSc else Sg.c.primary, RoundedCornerShape(9.dp)).padding(horizontal = 5.dp), contentAlignment = Alignment.Center) {
                            Text(n.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = Onest, color = if (on) Sg.c.sc else Sg.c.onPrimary)
                        }
                    }
                }
            }
        }
    }
}

/** Текст, который уменьшается, если не помещается в строку (крупные суммы в карточках). */
@Composable
fun FitText(text: String, style: TextStyle, color: Color = Sg.c.fg, modifier: Modifier = Modifier) {
    var scale by remember(text) { mutableFloatStateOf(1f) }
    Text(text, style = style.copy(fontSize = style.fontSize * scale), color = color, maxLines = 1, softWrap = false, modifier = modifier,
        onTextLayout = { if (it.hasVisualOverflow && scale > .55f) scale *= .92f })
}

/** Чип фильтра (.chip): контур, у выбранного тональная заливка. */
@Composable
fun Chip(text: String, on: Boolean = false, leading: (@Composable () -> Unit)? = null, trailingIcon: String? = null, check: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.height(34.dp).clip(shape).background(if (on) Sg.c.sc else Color.Transparent)
            .border(1.dp, if (on) Color.Transparent else Sg.c.outline, shape).clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (check && on) Ic("check", Sg.c.onSc, size = 16.dp)
        leading?.invoke()
        T(text, body(13.5f, FontWeight.Medium), if (on) Sg.c.onSc else Sg.c.fg, maxLines = 1)
        if (trailingIcon != null) Ic(trailingIcon, if (on) Sg.c.onSc else Sg.c.fg, size = 13.dp)
    }
}

@Composable
fun HScroll(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

@Composable
fun Dot(color: Color, size: Dp = 9.dp, modifier: Modifier = Modifier) { Box(modifier.size(size).background(color, CircleShape)) }

/** Заголовок раздела (.sect). */
@Composable
fun Sect(title: String, small: String? = null, top: Dp = 22.dp) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = top, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        T(title, disp(17), modifier = Modifier.weight(1f))
        if (small != null) T(small, body(12.5f, FontWeight.Medium), Sg.c.muted)
    }
}

/** Пустое состояние (.empty) с кругом и вращающейся пунктирной обводкой. */
@Composable
fun EmptyState(icon: String, title: String, text: String, error: Boolean = false, button: String? = null, buttonIcon: String = "plus", onButton: (() -> Unit)? = null) {
    val inf = rememberInfiniteTransition(label = "spin")
    val rot by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "rot")
    val col = if (error) Sg.c.wine else Sg.c.teal
    Column(Modifier.fillMaxWidth().padding(start = 32.dp, end = 32.dp, top = 48.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(100.dp)) {
                rotate(rot) {
                    drawCircle(col.copy(alpha = .3f), radius = size.minDimension / 2 - 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                }
            }
            Box(Modifier.size(84.dp).background(col.copy(alpha = if (error) .14f else .12f), CircleShape), contentAlignment = Alignment.Center) { Ic(icon, col, size = 28.dp) }
        }
        Spacer(Modifier.height(14.dp))
        T(title, disp(18), align = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        T(text, body(14f).copy(lineHeight = 20.sp), Sg.c.muted, align = TextAlign.Center, modifier = Modifier.widthIn(max = 260.dp))
        if (button != null && onButton != null) {
            Spacer(Modifier.height(20.dp))
            Btn(button, icon = buttonIcon, onClick = onButton)
        }
    }
}

@Composable
fun ErrorState(onRetry: () -> Unit, text: String = "Сервер не ответил. Проверьте связь и попробуйте ещё раз") =
    EmptyState("alert", "Не получилось загрузить", text, error = true, button = "Повторить", buttonIcon = "refresh", onButton = onRetry)

/** Кнопки (.btn): основная, тональная, текстовая, контурная. */
enum class BtnKind { Primary, Tonal, Text, Outline }

@Composable
fun Btn(text: String, modifier: Modifier = Modifier, icon: String? = null, kind: BtnKind = BtnKind.Primary, small: Boolean = false, wide: Boolean = false, danger: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val h = if (wide) 56.dp else if (small) 36.dp else 44.dp
    val shape = RoundedCornerShape(h / 2)
    val (bg, fg) = when (kind) {
        BtnKind.Primary -> Sg.c.primary to Sg.c.onPrimary
        BtnKind.Tonal -> Sg.c.sc to Sg.c.onSc
        BtnKind.Text -> Color.Transparent to (if (danger) Sg.c.danger else Sg.c.primary)
        BtnKind.Outline -> Color.Transparent to (if (danger) Sg.c.danger else Sg.c.primary)
    }
    Row(
        modifier.height(h).let { if (wide) it.fillMaxWidth() else it }.clip(shape).background(if (enabled) bg else bg.copy(alpha = .5f))
            .let { if (kind == BtnKind.Outline) it.border(1.dp, Sg.c.outline, shape) else it }
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = if (small) 14.dp else 22.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Ic(icon, fg, size = 16.dp); Spacer(Modifier.width(8.dp)) }
        T(text, body(if (wide) 16f else if (small) 13.5f else 15f, FontWeight.SemiBold), fg, maxLines = 1)
    }
}

/** Плашка «Нет интернета» (.offl). */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp).background(Sg.c.cHigh, RoundedCornerShape(14.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Ic("wifiOff", Sg.c.fg)
        Column {
            T("Нет интернета", body(13.5f, FontWeight.Medium))
            T("Показываем сохранённое. Новое придёт, когда связь вернётся", body(12.5f), Sg.c.muted)
        }
    }
}

/** Мерцающая заглушка загрузки (.skel). */
@Composable
fun Skel(modifier: Modifier, shape: Shape = RoundedCornerShape(10.dp)) {
    val inf = rememberInfiniteTransition(label = "shim")
    val x by inf.animateFloat(-1f, 2f, infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Restart), label = "x")
    val base = Sg.c.hair
    val hi = Sg.c.fg.copy(alpha = .14f)
    BoxWithConstraints(modifier.clip(shape)) {
        val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(base, hi, base), start = Offset(w * x - w, 0f), end = Offset(w * x, 0f))))
    }
}

@Composable
fun SkelRows(n: Int, avatar: Dp = 46.dp) {
    Column {
        repeat(n) { i ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Skel(Modifier.size(avatar), CircleShape)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Skel(Modifier.fillMaxWidth(((55 + (i * 17) % 35) / 100f)).height(12.dp))
                    Skel(Modifier.fillMaxWidth(((70 + (i * 11) % 25) / 100f)).height(10.dp))
                }
            }
        }
    }
}

/** Группа настроек (.grp). */
@Composable
fun Grp(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(20.dp)).background(Sg.c.cLow)) { content() }
}

/** Строка группы (.gi). */
@Composable
fun Gi(title: String, sub: String? = null, icon: String? = null, leading: (@Composable () -> Unit)? = null, titleColor: Color = Sg.c.fg, center: Boolean = false,
       onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).let { if (onClick != null) it.clickable(onClick = onClick) else it }.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (center) Arrangement.Center else Arrangement.Start,
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(14.dp)) }
        else if (icon != null) { Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) { Ic(icon, Sg.c.fg, size = 16.dp) }; Spacer(Modifier.width(14.dp)) }
        Column(if (center) Modifier.padding(vertical = 10.dp) else Modifier.weight(1f).padding(vertical = 10.dp)) {
            T(title, body(15f), titleColor)
            if (sub != null) T(sub, body(12.5f).copy(lineHeight = 17.sp), Sg.c.muted, modifier = Modifier.padding(top = 2.dp))
        }
        trailing?.invoke()
    }
}

@Composable
fun Chev() = Ic("chevR", Sg.c.faint, size = 16.dp)

@Composable
fun SgSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    Switch(on, onChange, colors = SwitchDefaults.colors(
        checkedThumbColor = Sg.c.onPrimary, checkedTrackColor = Sg.c.primary, checkedBorderColor = Sg.c.primary,
        uncheckedThumbColor = Sg.c.outline, uncheckedTrackColor = Sg.c.cHigh, uncheckedBorderColor = Sg.c.outline,
    ))
}

@Composable
fun Radio(on: Boolean) {
    Box(Modifier.size(22.dp).border(2.dp, if (on) Sg.c.primary else Sg.c.outline, CircleShape), contentAlignment = Alignment.Center) {
        if (on) Box(Modifier.size(11.dp).background(Sg.c.primary, CircleShape))
    }
}

/** Расширенная кнопка действия (.fab): при прокрутке сворачивается до иконки. */
@Composable
fun Fab(icon: String, text: String, expanded: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier.height(56.dp).androidx_shadow().clip(RoundedCornerShape(18.dp)).background(Sg.c.pc).clickable(onClick = onClick)
            .padding(start = 16.dp, end = if (expanded) 20.dp else 16.dp).animateContentSize(spring(stiffness = Spring.StiffnessMediumLow)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Ic(icon, Sg.c.onPc)
        AnimatedVisibility(expanded, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
            T(text, body(15f, FontWeight.SemiBold), Sg.c.onPc, modifier = Modifier.padding(start = 10.dp), maxLines = 1)
        }
    }
}

private fun Modifier.androidx_shadow(): Modifier = this.shadowCompat()

fun Modifier.shadowCompat(elev: Dp = 6.dp, shape: Shape = RoundedCornerShape(18.dp)): Modifier =
    this.shadow(elev, shape, clip = false, ambientColor = Color.Black.copy(alpha = .3f), spotColor = Color.Black.copy(alpha = .3f))

/** Поле ввода в листе (.field.sf): иконка слева, скругление 24. */
@Composable
fun SField(value: String, onChange: (String) -> Unit, placeholder: String, icon: String? = null, modifier: Modifier = Modifier, height: Dp = 48.dp,
           keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true, bg: Color = Sg.c.cHigh, password: Boolean = false) {
    Row(modifier.fillMaxWidth().heightIn(min = height).background(bg, RoundedCornerShape(height / 2)).padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Ic(icon, Sg.c.muted, size = 16.dp); Spacer(Modifier.width(8.dp)) }
        Box(Modifier.weight(1f).padding(vertical = 10.dp)) {
            if (value.isEmpty()) T(placeholder, body(15f), Sg.c.faint, maxLines = 1)
            BasicTextField(value, onChange, textStyle = body(15f).copy(color = Sg.c.fg), singleLine = singleLine, cursorBrush = SolidColor(Sg.c.primary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboard),
                visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Подпись над полем в листе (.lbl2). */
@Composable
fun Lbl(text: String) = T(text, body(12.5f, FontWeight.Medium), Sg.c.muted, modifier = Modifier.padding(start = 2.dp, top = 12.dp, bottom = 7.dp))

/** Чип выбора в листе (.fchip на Android). */
@Composable
fun FChip(text: String, on: Boolean, dot: Color? = null, leading: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(Modifier.height(34.dp).clip(shape).background(if (on) Sg.c.sc else Color.Transparent).border(1.dp, if (on) Color.Transparent else Sg.c.outline, shape)
        .clickable(onClick = onClick).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        if (dot != null) Dot(dot)
        leading?.invoke()
        T(text, body(13.5f, FontWeight.Medium), if (on) Sg.c.onSc else Sg.c.fg, maxLines = 1)
    }
}

/** Строка выбора товара/роли (.prod). */
@Composable
fun ProdRow(title: String, right: String, on: Boolean, rightSmall: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(Modifier.fillMaxWidth().height(50.dp).clip(shape).background(if (on) Sg.c.sc else Color.Transparent).border(1.dp, if (on) Color.Transparent else Sg.c.ov, shape)
        .clickable(onClick = onClick).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        T(title, body(14.5f), if (on) Sg.c.onSc else Sg.c.fg, modifier = Modifier.weight(1f), maxLines = 1)
        T(right, if (rightSmall) body(12f) else body(14.5f, FontWeight.Bold), if (rightSmall) Sg.c.muted else (if (on) Sg.c.onSc else Sg.c.fg))
    }
}

/** Заголовок листа (.sh-title на Android: слева). */
@Composable
fun SheetTitle(text: String) = T(text, disp(19), modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))

/** Логотип-глаз (светлый/тёмный по теме). */
@Composable
fun EyeLogo(size: Dp, modifier: Modifier = Modifier, shape: Shape = CircleShape) {
    Image(painterResource(R.drawable.eye_logo), null, modifier.size(size).clip(shape), contentScale = ContentScale.Crop)
}

@Composable
fun EyeSmall(size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.eye_small), null, modifier.size(size).clip(RoundedCornerShape(size * 0.225f)), contentScale = ContentScale.Crop)
}

/** Карточка-плитка (.tilea). */
@Composable
fun Tile(icon: String, text: String, primary: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.height(72.dp).clip(RoundedCornerShape(16.dp)).background(if (primary) Sg.c.pc else Sg.c.cHigh).clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Ic(icon, if (primary) Sg.c.onPc else Sg.c.fg)
        Spacer(Modifier.height(5.dp))
        T(text, body(12f, FontWeight.SemiBold), if (primary) Sg.c.onPc else Sg.c.fg, maxLines = 1)
    }
}

@Composable
fun Hint(text: String, modifier: Modifier = Modifier) =
    T(text, body(12.5f).copy(lineHeight = 17.5.sp), Sg.c.muted, modifier = modifier.padding(start = 28.dp, end = 28.dp, top = 8.dp, bottom = 16.dp))

@Composable
fun Tag(text: String, bg: Color = Sg.c.fg.copy(alpha = .08f), fg: Color = Sg.c.fg, icon: String? = null) {
    Row(Modifier.height(22.dp).background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) { Ic(icon, fg, size = 12.dp); Spacer(Modifier.width(4.dp)) }
        T(text, body(11.5f, FontWeight.SemiBold), fg, maxLines = 1)
    }
}

@Composable
fun Pressable(modifier: Modifier = Modifier, pressed: Boolean, content: @Composable () -> Unit) {
    Box(modifier.scale(if (pressed) .95f else 1f)) { content() }
}

val NoPad = PaddingValues(0.dp)
@Composable fun VGap(h: Dp) = Spacer(Modifier.height(h))
@Composable fun HGap(w: Dp) = Spacer(Modifier.width(w))
@Composable fun RowScope.Fill() = Spacer(Modifier.weight(1f))
@Composable fun FullH(modifier: Modifier = Modifier) = Spacer(modifier.fillMaxHeight())
