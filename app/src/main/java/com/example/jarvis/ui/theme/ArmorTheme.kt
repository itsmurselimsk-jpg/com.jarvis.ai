package com.example.jarvis.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class MarkArmorTheme(
    val title: String,
    val subtitle: String,
    val primaryColor: Color,
    val accentColor: Color,
    val glowColor: Color,
    val surfaceColor: Color,
    val surfaceElevated: Color,
    val borderColor: Color
) {
    STEALTH_SHADOW(
        title = "STEALTH SHADOW",
        subtitle = "Pitch OLED Black & Electric Neon Cyan",
        primaryColor = Color(0xFF00E5FF),
        accentColor = Color(0xFF80F3FF),
        glowColor = Color(0xFF00B0FF),
        surfaceColor = Color(0xFF0A0F1D),
        surfaceElevated = Color(0xFF10172A),
        borderColor = Color(0x3300E5FF)
    ),
    MARK_85(
        title = "MARK 85 CLASSIC",
        subtitle = "Iron Man Crimson Red, Gold & Cyan Core",
        primaryColor = Color(0xFFFF2A4B),
        accentColor = Color(0xFFFFD700),
        glowColor = Color(0xFF00E5FF),
        surfaceColor = Color(0xFF1A0A0E),
        surfaceElevated = Color(0xFF281119),
        borderColor = Color(0x4DFF2A4B)
    ),
    WAR_MACHINE(
        title = "WAR MACHINE",
        subtitle = "Titanium Gunmetal & Tactical Laser Red",
        primaryColor = Color(0xFFFF1744),
        accentColor = Color(0xFFE2E8F0),
        glowColor = Color(0xFFFF5252),
        surfaceColor = Color(0xFF0F1218),
        surfaceElevated = Color(0xFF181C26),
        borderColor = Color(0x40FF1744)
    ),
    WAKANDA_VIBRANIUM(
        title = "WAKANDA VIBRANIUM",
        subtitle = "Deep Obsidian Purple & Plasma Blue",
        primaryColor = Color(0xFFD500F9),
        accentColor = Color(0xFF00E5FF),
        glowColor = Color(0xFF7C4DFF),
        surfaceColor = Color(0xFF110A1C),
        surfaceElevated = Color(0xFF1C112C),
        borderColor = Color(0x40D500F9)
    ),
    QUANTUM_EMERALD(
        title = "QUANTUM EMERALD",
        subtitle = "Matrix Cyber Green & Neon Mint",
        primaryColor = Color(0xFF00E676),
        accentColor = Color(0xFFB9F6CA),
        glowColor = Color(0xFF00C853),
        surfaceColor = Color(0xFF08140D),
        surfaceElevated = Color(0xFF0E2417),
        borderColor = Color(0x4000E676)
    ),
    SOLAR_AMBER(
        title = "SOLAR AMBER",
        subtitle = "Arc Reactor Gold, Topaz & Molten Sunburst",
        primaryColor = Color(0xFFFFAB00),
        accentColor = Color(0xFFFFD54F),
        glowColor = Color(0xFFFF8F00),
        surfaceColor = Color(0xFF161005),
        surfaceElevated = Color(0xFF261B0A),
        borderColor = Color(0x4DFFAB00)
    ),
    NEON_CYBERPUNK(
        title = "NEON CYBERPUNK",
        subtitle = "Electric Hot Pink & Ultraviolet Glow",
        primaryColor = Color(0xFFFF007F),
        accentColor = Color(0xFFE040FB),
        glowColor = Color(0xFFD500F9),
        surfaceColor = Color(0xFF170612),
        surfaceElevated = Color(0xFF260A1E),
        borderColor = Color(0x4DFF007F)
    ),
    ARCTIC_FROST(
        title = "ARCTIC FROST",
        subtitle = "Crystal Glacier Cyan & Polar Deep Navy",
        primaryColor = Color(0xFF00F5FF),
        accentColor = Color(0xFFE0F7FA),
        glowColor = Color(0xFF00B4D8),
        surfaceColor = Color(0xFF04101A),
        surfaceElevated = Color(0xFF081C2D),
        borderColor = Color(0x4000F5FF)
    )
}

object ThemeManager {
    private val _currentTheme = MutableStateFlow(MarkArmorTheme.STEALTH_SHADOW)
    val currentTheme: StateFlow<MarkArmorTheme> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences("jarvis_theme_prefs", Context.MODE_PRIVATE)
        val savedName = prefs.getString("armor_theme_name", MarkArmorTheme.STEALTH_SHADOW.name)
        val matched = MarkArmorTheme.entries.find { it.name == savedName } ?: MarkArmorTheme.STEALTH_SHADOW
        _currentTheme.value = matched
    }

    fun setTheme(theme: MarkArmorTheme, context: Context? = null) {
        _currentTheme.value = theme
        context?.let {
            val prefs = it.getSharedPreferences("jarvis_theme_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("armor_theme_name", theme.name).apply()
        }
    }

    fun cycleNextTheme(context: Context? = null): MarkArmorTheme {
        val all = MarkArmorTheme.entries
        val currentIndex = all.indexOf(_currentTheme.value)
        val nextTheme = all[(currentIndex + 1) % all.size]
        setTheme(nextTheme, context)
        return nextTheme
    }

    fun setThemeByName(name: String, context: Context? = null): Boolean {
        val clean = name.trim().lowercase()
        val matched = MarkArmorTheme.entries.find { 
            it.name.equals(clean, ignoreCase = true) || 
            it.title.lowercase().contains(clean) ||
            clean.contains(it.title.lowercase()) ||
            (clean.contains("red") && it == MarkArmorTheme.MARK_85) ||
            (clean.contains("cyan") && it == MarkArmorTheme.STEALTH_SHADOW) ||
            (clean.contains("blue") && it == MarkArmorTheme.STEALTH_SHADOW) ||
            (clean.contains("green") && it == MarkArmorTheme.QUANTUM_EMERALD) ||
            (clean.contains("emerald") && it == MarkArmorTheme.QUANTUM_EMERALD) ||
            (clean.contains("gold") && it == MarkArmorTheme.SOLAR_AMBER) ||
            (clean.contains("yellow") && it == MarkArmorTheme.SOLAR_AMBER) ||
            (clean.contains("pink") && it == MarkArmorTheme.NEON_CYBERPUNK) ||
            (clean.contains("purple") && it == MarkArmorTheme.WAKANDA_VIBRANIUM) ||
            (clean.contains("ice") && it == MarkArmorTheme.ARCTIC_FROST)
        }
        return if (matched != null) {
            setTheme(matched, context)
            true
        } else false
    }
}
