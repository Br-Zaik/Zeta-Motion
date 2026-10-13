import com.zeta.fondo.model.EffectClip
import com.zeta.fondo.model.EffectTimeline
import kotlin.math.abs

private fun eq(expected: Float, actual: Float) { check(abs(expected-actual) < 0.0001f) {"Expected $expected, found $actual"} }

fun main() {
    val full = EffectClip(1L,"natural",0f,6f,strength=.7f)
    eq(.7f,EffectTimeline.sample(listOf(full),.5f,6f).gate)
    val clips = listOf(EffectClip(1,"gust",0f,6f,strength=.4f), EffectClip(2,"gust",2f,2f,strength=.9f))
    eq(.4f,EffectTimeline.sample(clips,.1f,6f).gate)
    eq(1.3f,EffectTimeline.sample(clips,.5f,6f).gate)
    val partial = EffectClip(3,"bolt",1f,3f)
    eq(0f,EffectTimeline.sample(listOf(partial),0f,6f).gate)
    eq(0f,EffectTimeline.sample(listOf(partial),1f/6f,6f).gate)
    check(EffectTimeline.sample(listOf(partial),.35f,6f).gate > .98f)
    eq(0f,EffectTimeline.sample(listOf(partial),.7f,6f).gate)
    val normalized = EffectTimeline.normalized(EffectClip(4,"natural",-4f,99f,strength=9f),6f)
    eq(0f,normalized.startSec);eq(6f,normalized.durationSec);eq(2f,normalized.strength)
    println("Timeline: 9 assertions passed")
}
