package com.astraflow.Chizuru.ui.kit.icons

import androidx.compose.ui.graphics.vector.ImageVector
import zone.ien.hig.icons.CupertinoIcons
import zone.ien.hig.icons.filled.Moon
import zone.ien.hig.icons.filled.Pause
import zone.ien.hig.icons.filled.Play
import zone.ien.hig.icons.filled.RotateLeft
import zone.ien.hig.icons.outlined.AntennaRadiowavesLeftAndRight
import zone.ien.hig.icons.outlined.ArrowUpArrowDown
import zone.ien.hig.icons.outlined.Checkmark
import zone.ien.hig.icons.outlined.ChevronBackward
import zone.ien.hig.icons.outlined.ChevronDown
import zone.ien.hig.icons.outlined.ChevronForward
import zone.ien.hig.icons.outlined.Ellipsis
import zone.ien.hig.icons.outlined.Eye
import zone.ien.hig.icons.outlined.EyeSlash
import zone.ien.hig.icons.outlined.Questionmark
import zone.ien.hig.icons.outlined.Wifi
import zone.ien.hig.icons.outlined.Xmark

/**
 * 星流界面用的图标,按用处起名;颜色由使用处上色。
 * 功能、底栏、账号与提醒这些有图案的图标是星流自绘的一套([AstraDrawnGlyphs],同一套粗细与留白);
 * 箭头、对勾、关闭这类控件里的小记号用上游 compose-hig 的 iOS 图标。
 */
object AstraGlyphs {
    // ── 控件里的小记号(上游) ──
    val ChevronRight: ImageVector get() = CupertinoIcons.Outlined.ChevronForward
    val ChevronLeft: ImageVector get() = CupertinoIcons.Outlined.ChevronBackward
    val ChevronDown: ImageVector get() = CupertinoIcons.Outlined.ChevronDown
    val ChevronUpDown: ImageVector get() = CupertinoIcons.Outlined.ArrowUpArrowDown
    val Check: ImageVector get() = CupertinoIcons.Outlined.Checkmark
    val Close: ImageVector get() = CupertinoIcons.Outlined.Xmark
    val Question: ImageVector get() = CupertinoIcons.Outlined.Questionmark
    val More: ImageVector get() = CupertinoIcons.Outlined.Ellipsis
    val Sort: ImageVector get() = CupertinoIcons.Outlined.ArrowUpArrowDown
    val Wifi: ImageVector get() = CupertinoIcons.Outlined.Wifi
    val Bluetooth: ImageVector get() = CupertinoIcons.Outlined.AntennaRadiowavesLeftAndRight
    val Moon: ImageVector get() = CupertinoIcons.Filled.Moon
    val RotateLeft: ImageVector get() = CupertinoIcons.Filled.RotateLeft
    val Play: ImageVector get() = CupertinoIcons.Filled.Play
    val Pause: ImageVector get() = CupertinoIcons.Filled.Pause
    val PasswordShown: ImageVector get() = CupertinoIcons.Outlined.Eye
    val PasswordHidden: ImageVector get() = CupertinoIcons.Outlined.EyeSlash

    // ── 星河岛的功能(自绘) ──
    val Music: ImageVector get() = AstraDrawnGlyphs.Music
    val Message: ImageVector get() = AstraDrawnGlyphs.Message
    val LiveActivity: ImageVector get() = AstraDrawnGlyphs.LiveActivity
    val Face: ImageVector get() = AstraDrawnGlyphs.Face
    val ExternalApps: ImageVector get() = AstraDrawnGlyphs.ExternalApps
    val ResidentIsland: ImageVector get() = AstraDrawnGlyphs.ResidentIsland
    val ChargeIsland: ImageVector get() = AstraDrawnGlyphs.ChargeIsland
    val Weather: ImageVector get() = AstraDrawnGlyphs.Weather
    val Palette: ImageVector get() = AstraDrawnGlyphs.Palette
    val Move: ImageVector get() = AstraDrawnGlyphs.Move
    val Eye: ImageVector get() = AstraDrawnGlyphs.Eye

    // ── 系统的功能(自绘) ──
    val Droplet: ImageVector get() = AstraDrawnGlyphs.Droplet
    val NotificationReply: ImageVector get() = AstraDrawnGlyphs.NotificationReply
    val NotificationStack: ImageVector get() = AstraDrawnGlyphs.NotificationStack
    val Sun: ImageVector get() = AstraDrawnGlyphs.Sun
    val Ring: ImageVector get() = AstraDrawnGlyphs.Ring
    val StatusBarOrder: ImageVector get() = AstraDrawnGlyphs.StatusBarOrder
    val Lock: ImageVector get() = AstraDrawnGlyphs.Lock
    val Sidebar: ImageVector get() = AstraDrawnGlyphs.Sidebar
    val Launch: ImageVector get() = AstraDrawnGlyphs.Launch
    val Gamepad: ImageVector get() = AstraDrawnGlyphs.Gamepad
    val Record: ImageVector get() = AstraDrawnGlyphs.Record

    // ── 底栏(自绘) ──
    val TabToday: ImageVector get() = AstraDrawnGlyphs.TabToday
    val TabIsland: ImageVector get() = AstraDrawnGlyphs.TabIsland
    val TabSystem: ImageVector get() = AstraDrawnGlyphs.TabSystem
    val TabDiscover: ImageVector get() = AstraDrawnGlyphs.TabDiscover
    val Search: ImageVector get() = AstraDrawnGlyphs.Search

    // ── 账号、商店与提醒(自绘) ──
    val Mail: ImageVector get() = AstraDrawnGlyphs.Mail
    val Gift: ImageVector get() = AstraDrawnGlyphs.Gift
    val CloudBackup: ImageVector get() = AstraDrawnGlyphs.CloudBackup
    val Gear: ImageVector get() = AstraDrawnGlyphs.Gear
    val Wrench: ImageVector get() = AstraDrawnGlyphs.Wrench
    val Feedback: ImageVector get() = AstraDrawnGlyphs.Feedback
    val Info: ImageVector get() = AstraDrawnGlyphs.Info
    val Megaphone: ImageVector get() = AstraDrawnGlyphs.Megaphone
    val Bell: ImageVector get() = AstraDrawnGlyphs.Bell
    val Warning: ImageVector get() = AstraDrawnGlyphs.Warning
    val Plugin: ImageVector get() = AstraDrawnGlyphs.Plugin
    val Config: ImageVector get() = AstraDrawnGlyphs.Config
    val People: ImageVector get() = AstraDrawnGlyphs.People
    val Person: ImageVector get() = AstraDrawnGlyphs.Person
    val Sparkle: ImageVector get() = AstraDrawnGlyphs.Sparkle
}
