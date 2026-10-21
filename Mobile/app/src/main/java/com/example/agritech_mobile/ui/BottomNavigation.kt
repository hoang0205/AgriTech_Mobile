package com.example.agritech_mobile.ui/*
 * Copyright 2026 Kyriakos Georgiopoulos
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * AGSL runtime shader used by [BubbleBottomBar] to create a moving glass lens.
 *
 * The shader samples the composable behind the selected bubble, bends the UV coordinates through a
 * spherical normal approximation, and adds a thin-film interference highlight. The deformation
 * uniform is driven from the horizontal velocity of the selected tab, so fast tab changes stretch
 * the lens slightly while slow changes stay dense and controlled.
 *
 * The shader is only installed on Android 13 and newer. Older devices keep the same layout and use
 * a simpler translucent oval fallback.
 */
const val KINEMATIC_LENS_SHADER = """
uniform shader composable;
uniform float2 touchCenter;
uniform float radius;
uniform float progress; 
uniform float2 deformation; 
uniform float popProgress;
uniform float sysTime;

float hash(float2 p) {
    return fract(sin(dot(p, float2(12.9898, 78.233))) * 43758.5453);
}

float smoothNoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i + float2(0.0, 0.0)), hash(i + float2(1.0, 0.0)), u.x),
               mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
}

half4 main(float2 fragCoord) {
    float THICKNESS_BASE = 250.0;
    float THICKNESS_GRAVITY = 150.0;
    float THICKNESS_SWIRL = 120.0;
    float THICKNESS_DETAIL = 50.0;
    float COLOR_INTENSITY = 3.5; 
    float EDGE_FADE_END = 0.15;
    float ENV_REFLECTION_STRENGTH = 0.7; 
    float ENV_BLUR_RADIUS = 60.0;

    half4 rawBackground = composable.eval(fragCoord);
    if (popProgress >= 1.0) return rawBackground;

    float2 rawUv = fragCoord - touchCenter;
    
    float speed = length(deformation); 
    float2 moveDir = speed > 0.001 ? deformation / speed : float2(0.0, 1.0); 
    
    float parallelDist = dot(rawUv, moveDir);
    float2 perpVector = rawUv - moveDir * parallelDist;
    
    float stretch = 1.0 + speed; 
    float squash = 1.0 / sqrt(stretch); 
    
    float2 uv = (moveDir * (parallelDist / stretch)) + (perpVector / squash);
    float dist = length(uv);
    float activeRadius = radius * (1.0 + popProgress * 1.5);

    if (dist >= activeRadius) {
        return rawBackground;
    }

    float2 nUv = uv / activeRadius;
    float distSq = dot(nUv, nUv);
    float z = sqrt(max(0.0, 1.0 - distSq));
    float3 normal = normalize(float3(nUv, z));
    float3 viewDir = float3(0.0, 0.0, 1.0); 
    float NdotV = max(0.0, dot(normal, viewDir));

    float magnification = 0.45;
    float lensDeform = (1.0 - z) * magnification * (1.0 - popProgress);
    
    float2 refUvR = fragCoord - (nUv * activeRadius * (lensDeform * 0.88));
    float2 refUvG = fragCoord - (nUv * activeRadius * (lensDeform * 1.00));
    float2 refUvB = fragCoord - (nUv * activeRadius * (lensDeform * 1.12));

    half3 bgColor = half3(
        composable.eval(refUvR).r,
        composable.eval(refUvG).g,
        composable.eval(refUvB).b
    );

    float3 reflectionDir = reflect(-viewDir, normal);
    float3 lightDir1 = normalize(float3(0.6, 0.7, 0.8));
    float3 lightDir2 = normalize(float3(-0.5, -0.4, 0.6));
    
    float lightAlign1 = max(0.0, dot(reflectionDir, lightDir1));
    float lightAlign2 = max(0.0, dot(reflectionDir, lightDir2));

    float n_film = 1.33;       
    float n_air  = 1.0;
    float R0 = pow((n_film - n_air) / (n_film + n_air), 2.0); 
    float fresnel = R0 + (1.0 - R0) * pow(1.0 - NdotV, 5.0);

    float sinThetaI = sqrt(max(0.0, 1.0 - NdotV * NdotV));
    float sinThetaT = sinThetaI / n_film;
    float cosThetaT = sqrt(max(0.0, 1.0 - sinThetaT * sinThetaT));

    float swirl = smoothNoise(nUv * 3.0 + sysTime * 0.12);
    float thicknessNoise = smoothNoise(nUv * 5.0 - sysTime * 0.08);

    float baseThickness = THICKNESS_BASE + nUv.y * THICKNESS_GRAVITY;
    float thickness = baseThickness + swirl * THICKNESS_SWIRL + thicknessNoise * THICKNESS_DETAIL;
    thickness = clamp(thickness, 80.0, 900.0);

    float opd = 2.0 * n_film * thickness * cosThetaT;
    float lambda_R = 650.0;
    float lambda_G = 532.0;
    float lambda_B = 450.0;
    float TWO_PI = 6.2831853;

    float oscR = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_R);
    float oscG = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_G);
    float oscB = 0.5 + 0.5 * cos(TWO_PI * opd / lambda_B);

    half3 interferenceColor = half3(oscR, oscG, oscB);
    float interferenceStrength = smoothstep(0.0, EDGE_FADE_END, NdotV);
    half3 filmReflection = interferenceColor * fresnel * COLOR_INTENSITY;
    half3 whiteReflection = half3(fresnel);
    half3 thinFilmColor = mix(whiteReflection, filmReflection, interferenceStrength);

    float spec1 = pow(lightAlign1, 250.0) * 2.5;
    float spec2 = pow(lightAlign2, 60.0) * 0.5;
    half3 highlights = half3(spec1 + spec2);

    float2 reflectOffset = normal.xy * ENV_BLUR_RADIUS;
    float2 envCenter = fragCoord + reflectOffset;
    float blurStep = ENV_BLUR_RADIUS * 0.4;
    half3 envSample = composable.eval(envCenter).rgb * 0.4
        + composable.eval(envCenter + float2(blurStep, 0.0)).rgb * 0.15
        + composable.eval(envCenter - float2(blurStep, 0.0)).rgb * 0.15
        + composable.eval(envCenter + float2(0.0, blurStep)).rgb * 0.15
        + composable.eval(envCenter - float2(0.0, blurStep)).rgb * 0.15;

    half3 envReflection = envSample * fresnel * ENV_REFLECTION_STRENGTH;

    float rimShadow = smoothstep(0.90, 1.0, sqrt(distSq));
    bgColor *= (1.0 - rimShadow * 0.35);

    half3 finalColor = bgColor * (1.0 - half3(fresnel)) + thinFilmColor + envReflection + highlights;
    return half4(finalColor, rawBackground.a);
}
"""

/**
 * Visual phase for one tab in the liquid bottom bar.
 *
 * The selected item does not simply jump from inactive to active. It moves through a small
 * physical story:
 *
 * - [UNSELECTED] keeps the tab merged into the white surface.
 * - [SQUASHING] briefly compresses the droplet before launch, like stored elastic energy.
 * - [RISING] lets the bubble detach upward while the base lump follows.
 *
 * [SPAWNING] is kept as a useful intermediate phase for experiments with a fuller state machine.
 */
enum class DropletState { UNSELECTED, SPAWNING, SQUASHING, RISING }

/**
 * Minimal data needed by the blue liquid navigation.
 *
 * Keep this small and stable: the component animates every tab independently, so passing only the
 * title and vector keeps the public API clear and avoids binding this demo to a routing layer.
 */
data class BottomNavItem(val title: String, val icon: ImageVector)

/**
 * Animation state holder for a single tab.
 *
 * This intentionally stores [State] objects instead of sampled `Float` values. The parent can pass
 * the motion object down without eagerly reading all animated values in composition. Children and
 * canvas drawing then read the exact value they need in draw/layer phases, which keeps recomposition
 * pressure low while animations are running.
 */
@Stable
private class TabMotion(
    val lumpProgress: State<Float>,
    val bubbleYProgress: State<Float>,
    val baseScale: State<Float>,
    val presenceProgress: State<Float>,
    val squashProgress: State<Float>
)

/**
 * Fixed arc angles for the white surface bulge in [GooeyBottomNavigation].
 *
 * The arc spans the lower half of an implied circle. Its touch points sit below the bar baseline,
 * which gives the Bezier shoulders enough slope to look like a soft liquid connection instead of a
 * hard semicircular cutout.
 */
private const val GooeyLeftAngle = 210f
private const val GooeyRightAngle = 330f
private const val GooeySweep = 120f

private val GooeyLeftRadians = Math.toRadians(GooeyLeftAngle.toDouble()).toFloat()
private val GooeyRightRadians = Math.toRadians(GooeyRightAngle.toDouble()).toFloat()
private val GooeyLeftCos = cos(GooeyLeftRadians)
private val GooeyLeftSin = sin(GooeyLeftRadians)
private val GooeyRightCos = cos(GooeyRightRadians)
private val GooeyRightSin = sin(GooeyRightRadians)
private val GooeyLeftTangent = Offset(-GooeyLeftSin, GooeyLeftCos)
private val GooeyRightTangent = Offset(-GooeyRightSin, GooeyRightCos)

/**
 * Scale used to render inactive blue-bar icons from one fixed measured size.
 *
 * Keeping the measured size stable and animating scale in [graphicsLayer]
 * avoids relayout during icon-size animation.
 */
private const val InactiveIconScale = 28f / 34f

/**
 * Sample content for [GooeyBottomNavigation].
 *
 * These are intentionally top-level values so previews and demo screens do not allocate icon models
 * on every recomposition.
 */
val blueNavItems = listOf(
    BottomNavItem("News", Icons.Outlined.Newspaper),
    BottomNavItem("Messages", Icons.Outlined.Email),
    BottomNavItem("Events", Icons.Outlined.CalendarMonth),
    BottomNavItem("Profile", Icons.Outlined.SentimentSatisfied)
)

/**
 * Icons used by [AnchoredGreenBottomBar].
 *
 * This bar intentionally accepts only icons because its visual focus is the moving notch/circle
 * relationship, not labels.
 */
val greenNavIcons = listOf(
    Icons.Outlined.Newspaper,
    Icons.Outlined.AddBox,
    Icons.Outlined.LocationCity
)

/**
 * Icons used by [BubbleBottomBar].
 *
 * Each index has a small personality animation in the bar: spin, shake, squash, or jump. The list
 * size is therefore part of the demo choreography.
 */
val glassNavIcons = listOf(
    Icons.Outlined.AutoAwesome,
    Icons.Outlined.SmartToy,
    Icons.Outlined.Eco,
    Icons.Outlined.Lightbulb
)

/**
 * Preview/demo entry point for the three navigation experiments in this file.
 *
 * The demo cycles through a liquid blue bar, an anchored green notch bar, and a shader-backed glass
 * bar. It is kept in the same file so readers can see each component in a working screen without
 * hunting through app navigation code.
 */
@Preview(showBackground = true)
@Composable
fun GooeyNavDemo() {
    var currentRoute by remember { mutableStateOf("BlueLiquid") }

    CircularRevealRouter(currentRoute = currentRoute) { route ->
        when (route) {
            "BlueLiquid" -> {
                BlueLiquidScreen(
                    onTapScreen = { currentRoute = "GreenNews" }
                )
            }

            "GreenNews" -> {
                GreenNewsScreen(
                    onTapScreen = { currentRoute = "GlassShader" }
                )
            }

            "GlassShader" -> {
                BubbleScreen(
                    onTapScreen = { currentRoute = "BlueLiquid" }
                )
            }
        }
    }
}

/**
 * Small route switcher that reveals the next demo screen through an expanding circle.
 *
 * Both old and new content are composed during the transition. The old route remains underneath
 * while the target route is clipped by [CircularRevealShape], then the old content is removed when
 * progress reaches `1f`.
 */
@Composable
fun CircularRevealRouter(
    currentRoute: String,
    content: @Composable (String) -> Unit
) {
    var oldRoute by remember { mutableStateOf(currentRoute) }
    var targetRoute by remember { mutableStateOf(currentRoute) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(currentRoute) {
        if (currentRoute != targetRoute) {
            oldRoute = targetRoute
            targetRoute = currentRoute
            progress.snapTo(0f)
            progress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (progress.value < 1f) {
            Box(modifier = Modifier.fillMaxSize()) { content(oldRoute) }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    clip = true
                    shape = CircularRevealShape(progress.value)
                }
        ) {
            content(targetRoute)
        }
    }
}

/**
 * Shape whose outline is an expanding circle centered in the available bounds.
 *
 * It is useful for full-screen reveal transitions because the final radius is computed from the
 * diagonal distance to the farthest corner, guaranteeing that the target content fully covers the
 * previous content at progress `1f`.
 *
 * The internal [Path] is reused across calls to [createOutline] to avoid per-frame allocation
 * pressure during the reveal animation.
 */
class CircularRevealShape(private val progress: Float) : Shape {

    private val reusablePath = Path()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = hypot(center.x.toDouble(), center.y.toDouble()).toFloat()
        val currentRadius = maxRadius * progress

        reusablePath.reset()
        reusablePath.addOval(
            Rect(
                center.x - currentRadius,
                center.y - currentRadius,
                center.x + currentRadius,
                center.y + currentRadius
            )
        )
        return Outline.Generic(reusablePath)
    }
}

/**
 * Demo screen for [GooeyBottomNavigation].
 *
 * The screen background and droplet share the same blue color. That matching color is the trick
 * that makes the active bubble look like it has detached from the page and punched through the white
 * navigation surface.
 */
@Composable
fun BlueLiquidScreen(onTapScreen: () -> Unit) {
    val darkBlue = Color(0xFF1E28A0)
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBlue)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = onTapScreen
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${blueNavItems[selectedIndex].title} Screen",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap anywhere to go to Green Screen",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 16.sp
                )
            }
        }

        GooeyBottomNavigation(
            items = blueNavItems,
            selectedIndex = selectedIndex,
            backgroundColor = darkBlue,
            onItemSelected = { index -> selectedIndex = index }
        )
    }
}

/**
 * A liquid bottom navigation bar built from two coordinated layers:
 *
 * 1. A [Canvas] draws the white base shape and the colored droplet. The base uses an arc plus two
 *    quadratic shoulders to fake a soft metaball connection between the surface and the active tab.
 * 2. A row of icons/text sits above the canvas. Each item owns its visual layer transforms so the
 *    parent row does not need to recompute layout every animation frame.
 *
 * The motion is intentionally simple but expressive. When a new tab is selected, previous tabs
 * collapse to [DropletState.UNSELECTED], then the target tab briefly enters [DropletState.SQUASHING]
 * before [DropletState.RISING]. The squash gives the animation a readable anticipation phase: the
 * circle widens and flattens, then springs upward.
 *
 * Performance notes for Compose readers:
 *
 * - Animation specs are remembered, so springs/tweens are not allocated on every recomposition.
 * - Expensive trigonometry for the fixed gooey angles is precomputed at file level.
 * - The selected callback is wrapped with `rememberUpdatedState`, which keeps click lambdas stable.
 * - Animated icon movement, scale, and alpha are applied in `graphicsLayer`; this is cheaper than
 *   changing measured size/offset in composition for every frame.
 * - Path objects are remembered and reset each frame to avoid per-frame allocation pressure.
 *
 * The algorithm is O(n) per frame, where n is the number of tabs. That is appropriate for bottom
 * navigation because n is normally tiny. Avoid using this exact shape builder for hundreds of items;
 * it is designed for clarity and visual quality in a small navigation surface.
 *
 * @param items tabs to render. Empty input renders nothing.
 * @param selectedIndex requested active tab. Out-of-range values are clamped defensively.
 * @param backgroundColor color of the screen behind the white surface; also used for the droplet.
 * @param onItemSelected invoked when a tab is tapped.
 */
@Composable
fun GooeyBottomNavigation(
    items: List<BottomNavItem>,
    selectedIndex: Int,
    backgroundColor: Color,
    onItemSelected: (Int) -> Unit
) {
    if (items.isEmpty()) return

    val navBarHeight = 150.dp
    val baseYOffset = 70.dp
    val maxOuterRadius = 41.dp
    val activeCircleSize = 62.dp
    val activeIconSize = 34.dp

    val barColor = backgroundColor // Thanh Bar lấy màu Primary truyền từ MainScreen
    val dropletColor = MaterialTheme.colorScheme.background // Giọt nước lấy màu nền hệ thống (Trắng/Đen tùy theme)

    val safeSelectedIndex = selectedIndex.coerceIn(items.indices)
    val onItemSelectedState by rememberUpdatedState(onItemSelected)

    val risingSpring = remember { spring<Float>(dampingRatio = 0.7f, stiffness = 350f) }
    val settleSpring = remember { spring<Float>(dampingRatio = 0.9f, stiffness = 600f) }
    val baseSpring = remember { spring<Float>(dampingRatio = 0.65f, stiffness = 350f) }
    val squashSpring = remember { spring<Float>(dampingRatio = 0.65f, stiffness = 400f) }
    val baseExitTween = remember { tween<Float>(durationMillis = 250, easing = FastOutSlowInEasing) }
    val presenceTween = remember { tween<Float>(durationMillis = 200) }

    val tabStates = remember(items.size) {
        mutableStateMapOf<Int, DropletState>().apply {
            put(safeSelectedIndex, DropletState.RISING)
        }
    }
    var isInitialLaunch by remember { mutableStateOf(true) }

    LaunchedEffect(items.size, safeSelectedIndex) {
        if (isInitialLaunch) {
            isInitialLaunch = false; return@LaunchedEffect
        }

        val staleKeys = tabStates.keys.filter { it !in items.indices }
        staleKeys.forEach(tabStates::remove)

        tabStates.keys.toList().forEach { index ->
            if (index != safeSelectedIndex) tabStates[index] = DropletState.UNSELECTED
        }

        delay(100)
        tabStates[safeSelectedIndex] = DropletState.SQUASHING
        delay(120)
        tabStates[safeSelectedIndex] = DropletState.RISING
    }

    val evaluatedTabs = items.indices.map { index ->
        val currentState = tabStates[index] ?: DropletState.UNSELECTED
        val transition = updateTransition(targetState = currentState, label = "TabMotion_$index")

        val bubbleYProgress = transition.animateFloat(
            transitionSpec = {
                if (targetState == DropletState.RISING) risingSpring else settleSpring
            }, label = "BubbleY"
        ) { state -> if (state == DropletState.RISING) 1f else 0f }

        val lumpProgress = transition.animateFloat(
            transitionSpec = {
                if (targetState == DropletState.RISING) risingSpring else settleSpring
            }, label = "LumpY"
        ) { state -> if (state == DropletState.RISING) 1f else 0f }

        val baseScale = transition.animateFloat(
            transitionSpec = {
                if (targetState == DropletState.UNSELECTED) baseExitTween else baseSpring
            }, label = "BaseScale"
        ) { state ->
            when (state) {
                DropletState.UNSELECTED -> 0f; DropletState.SPAWNING -> 0.8f; DropletState.SQUASHING -> 0.7f; DropletState.RISING -> 1f
            }
        }

        val squashProgress = transition.animateFloat(
            transitionSpec = { squashSpring },
            label = "Squash"
        ) { state -> if (state == DropletState.SQUASHING) 1f else 0f }
        val presenceProgress = transition.animateFloat(
            transitionSpec = { presenceTween },
            label = "Presence"
        ) { state -> if (state == DropletState.UNSELECTED) 0f else 1f }
        TabMotion(lumpProgress, bubbleYProgress, baseScale, presenceProgress, squashProgress)
    }

    val whitePath = remember { Path() }
    val dropletPaths = remember(items.size) { Array(items.size) { Path() } }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(navBarHeight)
            .background(Color.Transparent)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseY = baseYOffset.toPx()
            val tabWidth = size.width / items.size
            val maxOuterRadiusPx = maxOuterRadius.toPx()
            val activeCircleSizePx = activeCircleSize.toPx()

            whitePath.reset()
            whitePath.moveTo(0f, baseY)

            items.indices.forEach { i ->
                val metrics = evaluatedTabs[i]
                val lumpProgress = metrics.lumpProgress.value.coerceAtLeast(0f)
                if (lumpProgress > 0.001f) {
                    val cx = (i * tabWidth) + (tabWidth / 2f)
                    val outerR = maxOuterRadiusPx * lumpProgress
                    val valleySpan = outerR * 1.35f
                    val tangentPull = outerR * 0.65f

                    val leftTouch = Offset(
                        x = cx + outerR * GooeyLeftCos,
                        y = baseY + outerR * GooeyLeftSin
                    )
                    val rightTouch = Offset(
                        x = cx + outerR * GooeyRightCos,
                        y = baseY + outerR * GooeyRightSin
                    )

                    whitePath.lineTo(leftTouch.x - valleySpan, baseY)
                    whitePath.quadraticTo(
                        leftTouch.x - GooeyLeftTangent.x * tangentPull,
                        leftTouch.y - GooeyLeftTangent.y * tangentPull,
                        leftTouch.x,
                        leftTouch.y
                    )
                    whitePath.arcTo(
                        Rect(cx - outerR, baseY - outerR, cx + outerR, baseY + outerR),
                        GooeyLeftAngle,
                        GooeySweep,
                        false
                    )
                    whitePath.quadraticTo(
                        rightTouch.x + GooeyRightTangent.x * tangentPull,
                        rightTouch.y + GooeyRightTangent.y * tangentPull,
                        rightTouch.x + valleySpan,
                        baseY
                    )
                } else {
                    whitePath.lineTo((i + 1) * tabWidth, baseY)
                }
            }
            whitePath.lineTo(size.width, baseY)
            whitePath.lineTo(size.width, size.height)
            whitePath.lineTo(0f, size.height)
            whitePath.close()

            val barGradient = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(
                    backgroundColor, // Màu Primary truyền vào
                    androidx.compose.ui.graphics.Color(0xFF4CAF50)
                )
            )

            drawPath(
                path = whitePath,
                brush = barGradient // Dùng brush thay vì color
            )

            items.indices.forEach { i ->
                val metrics = evaluatedTabs[i]
                val presenceProgress = metrics.presenceProgress.value
                if (presenceProgress > 0.001f) {
                    val cx = (i * tabWidth) + (tabWidth / 2f)
                    val whiteAreaCenterPx = baseY + (size.height - baseY) / 2f
                    val bubbleYProgress = metrics.bubbleYProgress.value
                    val currentCenterYPx =
                        whiteAreaCenterPx - ((whiteAreaCenterPx - baseY) * bubbleYProgress)

                    val radius = (activeCircleSizePx / 2f) * metrics.baseScale.value
                    val lumpProgress = metrics.lumpProgress.value.coerceAtLeast(0f)
                    val tailLength =
                        (bubbleYProgress - lumpProgress).coerceAtLeast(0f) * (activeCircleSizePx * 1.5f)

                    val dropletPath = dropletPaths[i]
                    dropletPath.reset()

                    val rect = Rect(-radius, -radius, radius, radius)
                    if (tailLength > 0.1f) {
                        dropletPath.arcTo(rect, 180f, 180f, forceMoveTo = true)
                        val bottomY = radius + tailLength
                        dropletPath.cubicTo(
                            radius,
                            radius * 0.5f + tailLength * 0.2f,
                            radius * 0.2f,
                            bottomY,
                            0f,
                            bottomY
                        )
                        dropletPath.cubicTo(
                            -radius * 0.2f,
                            bottomY,
                            -radius,
                            radius * 0.5f + tailLength * 0.2f,
                            -radius,
                            0f
                        )
                    } else {
                        dropletPath.addOval(rect)
                    }
                    dropletPath.close()

                    val squashProgress = metrics.squashProgress.value
                    val squashScaleX = 1f + (squashProgress * 0.25f)
                    val squashScaleY = 1f - (squashProgress * 0.15f)

                    translate(left = cx, top = currentCenterYPx) {
                        scale(scaleX = squashScaleX, scaleY = squashScaleY, pivot = Offset.Zero) {
                            drawPath(
                                dropletPath,
                                color = dropletColor,
                                alpha = presenceProgress
                            )
                        }
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, item ->
                GooeyNavigationItem(
                    item = item,
                    motion = evaluatedTabs[index],
                    navBarHeight = navBarHeight,
                    baseYOffset = baseYOffset,
                    activeCircleSize = activeCircleSize,
                    activeIconSize = activeIconSize,
                    textColor = Color.White,
                    activeIconColor = barColor,
                    inactiveIconColor = Color.White.copy(alpha = 0.6f),
                    onClick = { onItemSelectedState(index) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun GooeyNavigationItem(
    item: BottomNavItem,
    motion: TabMotion,
    navBarHeight: Dp,
    baseYOffset: Dp,
    activeCircleSize: Dp,
    activeIconSize: Dp,
    textColor: Color,
    activeIconColor: Color,
    inactiveIconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }

    val navBarHeightPx = remember(navBarHeight, density) { with(density) { navBarHeight.toPx() } }
    val baseYOffsetPx = remember(baseYOffset, density) { with(density) { baseYOffset.toPx() } }
    val activeCircleSizePx = remember(activeCircleSize, density) { with(density) { activeCircleSize.toPx() } }
    val activeIconSizePx = remember(activeIconSize, density) { with(density) { activeIconSize.toPx() } }
    val paddingBelowCirclePx = remember(density) { with(density) { 12.dp.toPx() } }

    val activeTextYPx = baseYOffsetPx + (activeCircleSizePx / 2f) + paddingBelowCirclePx
    val restingTextYPx = activeTextYPx + paddingBelowCirclePx
    val whiteAreaCenterPx = baseYOffsetPx + ((navBarHeightPx - baseYOffsetPx) / 2f)
    val iconTravelPx = whiteAreaCenterPx - baseYOffsetPx

    Box(
        modifier = modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        ),
        contentAlignment = Alignment.TopCenter
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = activeIconColor,
            modifier = Modifier
                .size(activeIconSize)
                .graphicsLayer {
                    val bubbleYProgress = motion.bubbleYProgress.value
                    val iconScale = InactiveIconScale + ((1f - InactiveIconScale) * bubbleYProgress)
                    val currentCenterY = whiteAreaCenterPx - (iconTravelPx * bubbleYProgress)

                    translationY = currentCenterY - (activeIconSizePx / 2f)
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = motion.presenceProgress.value
                }
        )
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.75f),
            modifier = Modifier
                .size(activeIconSize)
                .graphicsLayer {
                    val bubbleYProgress = motion.bubbleYProgress.value
                    val iconScale = InactiveIconScale + ((1f - InactiveIconScale) * bubbleYProgress)
                    val currentCenterY = whiteAreaCenterPx - (iconTravelPx * bubbleYProgress)

                    translationY = currentCenterY - (activeIconSizePx / 2f)
                    scaleX = iconScale
                    scaleY = iconScale
                    alpha = 1f - motion.presenceProgress.value
                }
        )
        Text(
            text = item.title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.graphicsLayer {
                val bubbleYProgress = motion.bubbleYProgress.value
                translationY = restingTextYPx - ((restingTextYPx - activeTextYPx) * bubbleYProgress)
                alpha = motion.presenceProgress.value
            }
        )
    }
}

/**
 * One tab in [GooeyBottomNavigation].
 *
 * The item draws two icons on top of each other: one in the active color and one in the background
 * color. Their alpha values are cross-faded in a layer instead of lerping tint in composition. This
 * keeps the animation in the render pipeline and avoids unnecessary recomposition while preserving
 * the same visual result.
 *
 * The icon size is also represented as a layer scale from [InactiveIconScale] to `1f`. Scaling a
 * fixed-size icon avoids repeated measurement during the spring animation. Text follows the same
 * principle: its vertical motion and opacity are layer properties.
 */

@Composable
fun GreenNewsScreen(onTapScreen: () -> Unit) {
    val greenColor = Color(0xFF4CAF50)
    val whiteBarColor = Color(0xFFF3F4F6)
    val darkIconColor = Color(0xFF455A64)

    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(greenColor)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = onTapScreen
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "News Dashboard",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tap anywhere here to go to Glass Shader",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp
                )
            }
        }

        AnchoredGreenBottomBar(
            items = greenNavIcons,
            selectedIndex = selectedIndex,
            barColor = whiteBarColor,
            activeColor = greenColor,
            inactiveColor = darkIconColor,
            onItemSelected = { selectedIndex = it }
        )
    }
}

/**
 * A bottom bar with an anchored floating circle and a matching cutout in the bar body.
 *
 * This version is less liquid than [GooeyBottomNavigation]. The selected tab moves a circular anchor
 * along the top edge, while the canvas path carves a cubic Bezier notch below it. The two animations
 * use the same spring values so the cutout, circle, and icon feel mechanically connected.
 *
 * Geometry notes:
 *
 * - `cutoutRadius` controls the size of the dip.
 * - `shoulderWidth` controls how much horizontal runway the Bezier curve gets before dipping.
 * - The left/right control points are clamped near rounded corners so the notch does not break the
 *   bar shape when the first or last tab is selected.
 */
@Composable
fun AnchoredGreenBottomBar(
    items: List<ImageVector>,
    selectedIndex: Int,
    barColor: Color,
    activeColor: Color,
    inactiveColor: Color,
    onItemSelected: (Int) -> Unit
) {
    val barHeight = 72.dp
    val topPadding = 52.dp
    val totalHeight = barHeight + topPadding

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val usableWidth = screenWidth - 32.dp
    val tabWidth = usableWidth / items.size

    val density = LocalDensity.current
    val tabWidthPx = with(density) { tabWidth.toPx() }
    val topYPx = remember(topPadding, density) { with(density) { topPadding.toPx() } }
    val barHeightPx = remember(barHeight, density) { with(density) { barHeight.toPx() } }

    val syncStiffness = 250f
    val syncDamping = 0.75f
    val syncSpring = remember { spring<Float>(dampingRatio = syncDamping, stiffness = syncStiffness) }

    val animatedCutoutX by animateFloatAsState(
        targetValue = (selectedIndex * tabWidthPx) + (tabWidthPx / 2f),
        animationSpec = syncSpring,
        label = "CutoutSlide"
    )

    val onItemSelectedState by rememberUpdatedState(onItemSelected)

    // Reusable Path for the bar shape
    val barPath = remember { Path() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .height(totalHeight)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerRadius = 16.dp.toPx()
            val circleRadius = 32.dp.toPx()
            val gap = 8.dp.toPx()
            val cutoutRadius = circleRadius + gap
            val dipDepth = cutoutRadius
            val shoulderWidth = 16.dp.toPx()

            val topY = topYPx
            val bottomYBar = topY + barHeightPx
            val cx = animatedCutoutX

            val safeLeft = cornerRadius
            val safeRight = size.width - cornerRadius
            val leftShoulderX = (cx - cutoutRadius - shoulderWidth).coerceAtLeast(safeLeft)
            val leftCp1X = (cx - cutoutRadius).coerceAtLeast(leftShoulderX)
            val leftCp2X = (cx - cutoutRadius).coerceAtLeast(leftShoulderX)
            val rightCp1X = (cx + cutoutRadius).coerceAtMost(safeRight)
            val rightCp2X = (cx + cutoutRadius).coerceAtMost(safeRight)
            val rightShoulderX =
                (cx + cutoutRadius + shoulderWidth).coerceAtMost(safeRight).coerceAtLeast(rightCp2X)

            barPath.reset()
            barPath.moveTo(0f, topY + cornerRadius)
            barPath.arcTo(
                Rect(0f, topY, 2 * cornerRadius, topY + 2 * cornerRadius), 180f, 90f, false
            )
            barPath.lineTo(leftShoulderX, topY)
            barPath.cubicTo(leftCp1X, topY, leftCp2X, topY + dipDepth, cx, topY + dipDepth)
            barPath.cubicTo(rightCp1X, topY + dipDepth, rightCp2X, topY, rightShoulderX, topY)
            barPath.lineTo(size.width - cornerRadius, topY)
            barPath.arcTo(
                Rect(size.width - 2 * cornerRadius, topY, size.width, topY + 2 * cornerRadius),
                -90f, 90f, false
            )
            barPath.lineTo(size.width, bottomYBar - cornerRadius)
            barPath.arcTo(
                Rect(
                    size.width - 2 * cornerRadius,
                    bottomYBar - 2 * cornerRadius,
                    size.width,
                    bottomYBar
                ),
                0f, 90f, false
            )
            barPath.lineTo(cornerRadius, bottomYBar)
            barPath.arcTo(
                Rect(0f, bottomYBar - 2 * cornerRadius, 2 * cornerRadius, bottomYBar),
                90f, 90f, false
            )
            barPath.close()

            drawPath(path = barPath, color = barColor)

            val circleCenterY = topY
            drawCircle(
                color = Color.White,
                radius = circleRadius,
                center = Offset(cx, circleCenterY)
            )
            drawCircle(
                color = activeColor,
                radius = circleRadius,
                center = Offset(cx, circleCenterY),
                style = Stroke(width = 3.dp.toPx())
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, icon ->
                val isSelected = selectedIndex == index
                val animatedIconSize by animateDpAsState(
                    targetValue = if (isSelected) 36.dp else 26.dp,
                    animationSpec = spring(dampingRatio = syncDamping, stiffness = syncStiffness),
                    label = "IconSize"
                )
                val animatedIconSizePx = with(density) { animatedIconSize.toPx() }
                val animatedIconColor by animateColorAsState(
                    targetValue = if (isSelected) activeColor else inactiveColor,
                    animationSpec = spring(dampingRatio = syncDamping, stiffness = syncStiffness),
                    label = "IconColor"
                )

                val targetCenterYPx = if (isSelected) topYPx else topYPx + (barHeightPx / 2f)
                val currentCenterYPx by animateFloatAsState(
                    targetValue = targetCenterYPx,
                    animationSpec = spring(dampingRatio = syncDamping, stiffness = syncStiffness),
                    label = "IconY"
                )

                val effectProgress by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = spring(dampingRatio = syncDamping, stiffness = syncStiffness),
                    label = "EffectProgress"
                )

                val iconRotation = when (index) {
                    0 -> (sin(effectProgress * Math.PI * 3) * 15f).toFloat(); 1 -> effectProgress * 360f; else -> 0f
                }
                val iconScaleX = when (index) {
                    2 -> 1f - (sin(effectProgress * Math.PI) * 0.15f).toFloat(); else -> 1f
                }
                val iconScaleY = when (index) {
                    2 -> 1f + (sin(effectProgress * Math.PI) * 0.4f).toFloat(); else -> 1f
                }
                val iconOrigin = when (index) {
                    2 -> TransformOrigin(
                        0.5f,
                        1f
                    ); else -> TransformOrigin.Center
                }

                val stableOnClick = remember(index, onItemSelectedState) {
                    { onItemSelectedState(index) }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = stableOnClick
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Icon(
                        imageVector = icon, contentDescription = null, tint = animatedIconColor,
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    0,
                                    (currentCenterYPx - animatedIconSizePx / 2f).roundToInt()
                                )
                            }
                            .size(animatedIconSize)
                            .graphicsLayer {
                                rotationZ = iconRotation; scaleX = iconScaleX; scaleY =
                                iconScaleY; transformOrigin = iconOrigin
                            }
                    )
                }
            }
        }
    }
}


/**
 * Demo screen for [BubbleBottomBar].
 *
 * The background is a slow rotating gradient so the glass lens has real color information to refract.
 * Selecting different tabs changes the gradient palette, making the shader response easier to study.
 */
@Composable
fun BubbleScreen(onTapScreen: () -> Unit) {
    val glassPillColor = Color(0x2AFFFFFF)
    val unselectedIconColor = Color.White.copy(alpha = 0.5f)

    var selectedIndex by remember { mutableIntStateOf(0) }

    val targetColor1 = when (selectedIndex) {
        0 -> Color(0xFF311B92)
        1 -> Color(0xFF01579B)
        2 -> Color(0xFF1B5E20)
        3 -> Color(0xFFBF360C)
        else -> Color.Black
    }

    val targetColor2 = when (selectedIndex) {
        0 -> Color(0xFF4A148C)
        1 -> Color(0xFF006064)
        2 -> Color(0xFF004D40)
        3 -> Color(0xFF880E4F)
        else -> Color.Black
    }

    val color1 by animateColorAsState(
        targetValue = targetColor1,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "bg1"
    )
    val color2 by animateColorAsState(
        targetValue = targetColor2,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "bg2"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "bg_rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing)),
        label = "rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.maxDimension * 0.8f

            val angleRad = Math.toRadians(rotation.toDouble())
            val startX = center.x + cos(angleRad).toFloat() * radius
            val startY = center.y + sin(angleRad).toFloat() * radius
            val endX = center.x - cos(angleRad).toFloat() * radius
            val endY = center.y - sin(angleRad).toFloat() * radius

            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(color1, color2),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY)
                )
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTapScreen
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Glass Shader Magic",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Watch the colors shift through the centered glass!",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap anywhere to return to Blue Liquid",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }

            BubbleBottomBar(
                items = glassNavIcons,
                selectedIndex = selectedIndex,
                pillColor = glassPillColor,
                activeIconColor = Color.White,
                inactiveIconColor = unselectedIconColor,
                onItemSelected = { selectedIndex = it }
            )
        }
    }
}

@Composable
fun BubbleBottomBar(
    items: List<ImageVector>,
    selectedIndex: Int,
    pillColor: Color,
    activeIconColor: Color,
    inactiveIconColor: Color,
    modifier: Modifier = Modifier,
    onItemSelected: (Int) -> Unit
) {
    val barHeight = 56.dp
    val topPadding = 20.dp
    val totalHeight = barHeight + topPadding

    val bubbleRadiusDp = 24.dp
    val cutoutGapDp = 2.dp

    val density = LocalDensity.current
    val barHeightPx = with(density) { barHeight.toPx() }
    val topYPx = with(density) { topPadding.toPx() }
    val centerYPx = topYPx + (barHeightPx / 2f)

    val syncStiffness = 180f
    val syncDamping = 0.65f

    BoxWithConstraints(
        modifier = modifier
            .height(totalHeight)
    ) {
        val usableWidth = maxWidth
        val tabWidth = usableWidth / items.size
        val tabWidthPx = with(density) { tabWidth.toPx() }

        val animatedBubbleX by animateFloatAsState(
            targetValue = (selectedIndex * tabWidthPx) + (tabWidthPx / 2f),
            animationSpec = spring(dampingRatio = syncDamping, stiffness = syncStiffness),
            label = "BubbleSlide"
        )

        val pillPath = remember { Path() }
        val cutoutPath = remember { Path() }
        val onItemSelectedState = rememberUpdatedState(onItemSelected)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val rx = (bubbleRadiusDp + cutoutGapDp).toPx()
            val ry = rx // Bong bóng tròn trịa

            pillPath.reset()
            pillPath.addRoundRect(
                RoundRect(
                    rect = Rect(0f, topYPx, size.width, topYPx + barHeightPx),
                    cornerRadius = CornerRadius(barHeightPx / 2f)
                )
            )

            cutoutPath.reset()
            cutoutPath.addOval(
                Rect(
                    left = animatedBubbleX - rx,
                    top = centerYPx - ry,
                    right = animatedBubbleX + rx,
                    bottom = centerYPx + ry
                )
            )

            // 1. Vẽ nền Pill trắng đặc
            drawPath(path = pillPath, color = pillColor)

            // 2. Vẽ bong bóng mờ lót dưới icon (không xuyên xuống background)
            clipPath(pillPath) {
                drawPath(
                    path = cutoutPath,
                    color = activeIconColor.copy(alpha = 0.12f)
                )
            }

            // 3. Vẽ viền bao quanh toàn bộ Pill
            drawPath(
                path = pillPath,
                color = Color.LightGray.copy(alpha = 0.4f),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, icon ->
                val isSelected = selectedIndex == index
                val animatedIconSize by animateDpAsState(
                    targetValue = if (isSelected) 32.dp else 24.dp,
                    animationSpec = spring(dampingRatio = syncDamping, stiffness = syncStiffness),
                    label = "IconSize"
                )
                val animatedIconColor by animateColorAsState(
                    targetValue = if (isSelected) activeIconColor else inactiveIconColor,
                    animationSpec = spring(dampingRatio = syncDamping, stiffness = syncStiffness),
                    label = "IconColor"
                )
                val effectProgress by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = spring(dampingRatio = syncDamping, stiffness = syncStiffness),
                    label = "Effect"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onItemSelectedState.value(index) }
                        ),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = animatedIconColor,
                        modifier = Modifier
                            .offset {
                                IntOffset(0, (centerYPx - with(density) { animatedIconSize.toPx() } / 2f).roundToInt())
                            }
                            .size(animatedIconSize)
                            .graphicsLayer {
                                // Hiệu ứng xoay/nhảy nhẹ khi chọn
                                rotationZ = if (index == 0) effectProgress * 360f else 0f
                                translationY = if (index == 1) -sin(effectProgress * Math.PI).toFloat() * 8f else 0f
                                scaleX = 1f + (sin(effectProgress * Math.PI) * 0.1f).toFloat()
                                transformOrigin = TransformOrigin.Center
                            }
                    )
                }
            }
        }
    }
}