package com.example.financecalc1

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.View
import kotlin.math.abs

data class Palette(val id: String, val background: Int, val surface: Int, val ink: Int, val muted: Int, val accent: Int, val header: Int, val headerInk: Int, val key: Int, val keyInk: Int, val line: Int, val highlight: Int = accent, val focusFill: Int = surface) {
    companion object {
        private fun c(s: String) = Color.parseColor(s)
        val wave = Palette("wave", c("#FFFFFF"), c("#F6F4F8"), c("#232032"), c("#6F697A"), c("#A73C8F"), c("#A73C8F"), Color.WHITE, c("#19171E"), Color.WHITE, c("#DED9E4"))
        val mint = Palette("mint", c("#F0F8F4"), c("#E3F0E9"), c("#123E3C"), c("#526C65"), c("#17645D"), c("#D1E8DD"), c("#123E3C"), c("#164B43"), c("#ECF8EC"), c("#B9D3C4"))
        val graphite = Palette("graphite", c("#101113"), c("#202226"), c("#F5F5EE"), c("#B4B7AF"), c("#DFF44A"), c("#DFF44A"), c("#181B11"), c("#DFF44A"), c("#181B11"), c("#41443D"))
        fun from(id: String?) = when(id) { "mint" -> mint; "graphite" -> graphite; else -> wave }
        /** A swatch changes hue inside a layout family, never its light/dark identity. */
        fun forStyle(style: InterfaceStyle, id: String?): Palette {
            val choice=id ?: style.defaultPalette
            return when(style) {
                InterfaceStyle.WAVE -> {
                    val a=c(when(choice){"mint"->"#16776D";"graphite"->"#5364B0";else->"#A73C8F"})
                    wave.copy(id=choice,accent=a,header=a)
                }
                InterfaceStyle.ORBIT -> {
                    val h=c(when(choice){"wave"->"#83D7CF";"graphite"->"#EDC28C";else->"#A5DC52"})
                    Palette(choice,c("#F1FAF8"),c("#E4F3F0"),c("#073E43"),c("#42686B"),c("#07545A"),c("#F1FAF8"),c("#073E43"),c("#064D53"),Color.WHITE,c("#CBE5E1"),h,androidx.core.graphics.ColorUtils.blendARGB(h,Color.WHITE,.40f))
                }
                InterfaceStyle.PRISM -> {
                    val a=c(when(choice){"wave"->"#BDACFF";"mint"->"#7EDBD5";else->"#DFF44A"})
                    graphite.copy(id=choice,accent=a,header=a,key=a)
                }
            }
        }

    }
}

fun shape(color: Int, radius: Float, stroke: Int? = null) = GradientDrawable().apply {
    setColor(color); cornerRadius = radius
    if (stroke != null) setStroke(2, stroke)
}

/** The layered boundary is drawn at any size, including large accessibility fonts. */
class WaveView(context: Context, private val palette: Palette) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(palette.header)
        paint.color = palette.background; paint.alpha = 55
        val first = Path().apply { moveTo(0f,h*.28f); cubicTo(w*.3f,-h*.25f,w*.64f,h*.95f,w,h*.3f); lineTo(w,h); lineTo(0f,h); close() }
        canvas.drawPath(first,paint)
        paint.alpha = 140
        val second = Path().apply { moveTo(0f,h*.7f); cubicTo(w*.4f,h*.25f,w*.65f,h*1.2f,w,h*.55f); lineTo(w,h); lineTo(0f,h); close() }
        canvas.drawPath(second,paint)
        paint.alpha = 255
        val last = Path().apply { moveTo(0f,h*.9f); cubicTo(w*.3f,h*.55f,w*.6f,h*1.12f,w,h*.79f); lineTo(w,h); lineTo(0f,h); close() }
        canvas.drawPath(last,paint)
    }
}

/** Licensed Lucide vectors; consistent paths with different framing per interface. */
class SymbolView(context: Context, symbol: String, tint: Int, private val style: String = "plain") : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ink = tint
    private val drawable = androidx.appcompat.content.res.AppCompatResources.getDrawable(context, when(symbol) {
        "budget" -> R.drawable.glyph_budget; "savings" -> R.drawable.glyph_savings
        "interest" -> R.drawable.glyph_interest; "loan" -> R.drawable.glyph_loan
        "debt" -> R.drawable.glyph_debt; "investment" -> R.drawable.glyph_investment
        "inflation" -> R.drawable.glyph_inflation; "goal" -> R.drawable.glyph_goal
        "formula" -> R.drawable.glyph_formula; "reset" -> R.drawable.glyph_reset
        "next" -> R.drawable.glyph_next; "close" -> R.drawable.glyph_close
        "back" -> R.drawable.glyph_back; "settings" -> R.drawable.glyph_settings
        "keyboard" -> R.drawable.glyph_keyboard; "delete" -> R.drawable.glyph_delete
        else -> R.drawable.glyph_check
    })!!.mutate().apply { setTint(tint) }
    override fun onDraw(canvas: Canvas) {
        val size = minOf(width,height).toFloat()
        var inset = 0f
        if (style == "orbit") {
            paint.color = androidx.core.graphics.ColorUtils.setAlphaComponent(ink, 22)
            canvas.drawCircle(width/2f,height/2f,size/2f,paint); inset = size*.20f
        } else if (style == "prism") {
            paint.color = androidx.core.graphics.ColorUtils.setAlphaComponent(ink, 18)
            canvas.drawRoundRect(0f,0f,width.toFloat(),height.toFloat(),size*.22f,size*.22f,paint); inset = size*.16f
        }
        drawable.setBounds(((width-size)/2+inset).toInt(),((height-size)/2+inset).toInt(),((width+size)/2-inset).toInt(),((height+size)/2-inset).toInt())
        drawable.draw(canvas)
    }
}

class RingView(context: Context, private val palette: Palette) : View(context) {
    var fraction: Float? = null
        set(value) { field=value; invalidate() }
    var solid = false
    var reversed = false
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val d=resources.displayMetrics.density
        val r=minOf(width,height)/2f-12*d;val x=width/2f;val y=height/2f
        paint.style=Paint.Style.FILL
        paint.shader=RadialGradient(x-r*.3f,y-r*.4f,r*1.8f,intArrayOf(Color.WHITE,palette.background,palette.line),null,Shader.TileMode.CLAMP)
        canvas.drawCircle(x,y,r,paint);paint.shader=null
        paint.style=Paint.Style.STROKE;paint.strokeWidth=if(solid)23*d else 10*d
        paint.color=if(solid) {if(reversed)palette.highlight else palette.accent} else palette.surface
        val arc=RectF(x-r+12*d,y-r+12*d,x+r-12*d,y+r-12*d)
        canvas.drawOval(arc,paint)
        fraction?.let {
            val hue=if(reversed)palette.accent else palette.highlight
            paint.shader=LinearGradient(arc.left,arc.top,arc.right,arc.bottom,hue,androidx.core.graphics.ColorUtils.blendARGB(hue,Color.WHITE,.20f),Shader.TileMode.CLAMP)
            canvas.drawArc(arc,-90f,360f*it.coerceIn(0f,1f),false,paint);paint.shader=null
        }
        paint.style=Paint.Style.FILL
    }
}

/** Small physical-looking vector objects: no baked image backgrounds, sharp at every density. */
class OrbitIconView(context:Context,private val symbol:String,private val palette:Palette):View(context){
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private fun body(c:Canvas,x:Float,y:Float,w:Float,h:Float,r:Float=6f,teal:Boolean=false){
        val a=if(teal)palette.accent else palette.highlight
        p.shader=null;p.color=androidx.core.graphics.ColorUtils.blendARGB(a,Color.BLACK,.25f)
        c.drawRoundRect(x+2,y+3,x+w+2,y+h+3,r,r,p)
        p.shader=LinearGradient(x,y,x+w,y+h,androidx.core.graphics.ColorUtils.blendARGB(a,Color.WHITE,.30f),a,Shader.TileMode.CLAMP)
        c.drawRoundRect(x,y,x+w,y+h,r,r,p);p.shader=null
    }
    override fun onDraw(c:Canvas){
        c.save();val z=minOf(width,height)/64f;c.translate((width-z*64)/2,(height-z*64)/2);c.scale(z,z)
        when(symbol){
            "budget"->{body(c,18f,8f,29f,32f,4f,true);body(c,8f,20f,46f,34f);body(c,38f,31f,19f,14f,4f,true);p.color=palette.highlight;c.drawCircle(45f,38f,2f,p)}
            "savings"->{for(i in 0..3)body(c,12f,41f-i*7,25f,9f,5f);for(i in 0..4)body(c,31f,43f-i*7,22f,8f,5f)}
            "interest"->{body(c,8f,8f,46f,46f,24f);p.color=palette.accent;p.style=Paint.Style.STROKE;p.strokeWidth=4f;c.drawCircle(22f,23f,5f,p);c.drawCircle(42f,41f,5f,p);c.drawLine(43f,19f,21f,45f,p);p.style=Paint.Style.FILL}
            "loan"->{body(c,6f,15f,50f,35f,6f);body(c,6f,23f,50f,7f,0f,true);body(c,13f,36f,10f,7f,2f,true);body(c,37f,38f,11f,3f,1f,true)}
            "debt"->{p.color=palette.accent;c.drawCircle(31f,34f,23f,p);p.shader=LinearGradient(20f,10f,50f,40f,Color.WHITE,palette.highlight,Shader.TileMode.CLAMP);c.drawArc(10f,5f,57f,52f,-90f,90f,true,p);p.shader=null}
            "investment"->{body(c,9f,37f,11f,18f,3f);body(c,25f,25f,11f,30f,3f);body(c,41f,10f,11f,45f,3f)}
            "inflation"->{p.color=palette.accent;p.strokeWidth=10f;p.style=Paint.Style.STROKE;p.strokeJoin=Paint.Join.ROUND;p.strokeCap=Paint.Cap.ROUND;val a=Path().apply{moveTo(10f,49f);lineTo(25f,30f);lineTo(35f,38f);lineTo(51f,16f)};c.drawPath(a,p);p.color=palette.highlight;p.strokeWidth=7f;c.drawPath(a,p);c.drawLine(40f,17f,51f,16f,p);c.drawLine(51f,16f,51f,28f,p);p.style=Paint.Style.FILL;p.strokeCap=Paint.Cap.BUTT}
            else->{for(i in 0..3){p.color=if(i%2==0)palette.highlight else palette.accent;c.drawCircle(31f,33f,25f-i*6,p)};p.color=palette.accent;p.strokeWidth=4f;c.drawLine(32f,32f,52f,12f,p);body(c,47f,8f,9f,11f,2f)}
        };c.restore()
    }
}

/** Actual miniature layouts, so the style chooser explains the structural difference. */
class InterfacePreview(context: Context, private val style: InterfaceStyle, private val p: Palette) : View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        val sx=width/100f;val sy=height/156f
        canvas.save();canvas.scale(sx,sy)
        fun rect(x:Float,y:Float,w:Float,h:Float,color:Int,r:Float=5f) {paint.color=color;canvas.drawRoundRect(x,y,x+w,y+h,r,r,paint)}
        rect(0f,0f,100f,156f,p.background,9f)
        if(style==InterfaceStyle.WAVE) {
            rect(0f,0f,100f,39f,p.header,9f);rect(10f,14f,55f,4f,p.headerInk)
            rect(9f,49f,82f,17f,p.surface);rect(9f,73f,82f,17f,p.surface)
            for(y in 0..3) for(x in 0..2) rect(9f+x*21,101f+y*11,17f,8f,p.surface,2f)
            rect(75f,101f,16f,41f,p.key)
        } else {
            rect(10f,12f,58f,4f,p.ink)
            rect(9f,28f,82f,18f,if(style==InterfaceStyle.ORBIT)p.focusFill else p.surface);rect(9f,52f,82f,18f,p.surface)
            if(style==InterfaceStyle.ORBIT) {paint.style=Paint.Style.STROKE;paint.strokeWidth=5f;paint.color=p.accent;canvas.drawCircle(50f,101f,22f,paint);paint.color=p.highlight;canvas.drawArc(28f,79f,72f,123f,-90f,110f,false,paint);paint.style=Paint.Style.FILL;rect(9f,136f,82f,13f,p.key,7f)}
            else {rect(9f,79f,82f,28f,p.accent,3f);rect(15f,90f,49f,8f,if(androidx.core.graphics.ColorUtils.calculateLuminance(p.accent)>.4) Color.BLACK else Color.WHITE,2f);rect(9f,136f,82f,13f,p.accent,3f)}
        }
        canvas.restore()
    }
}

/** Signed bars retain a shared zero and scale; values are never fabricated. */
class ComparisonView(context: Context, private val values: List<Double>, private val labels: List<String>, private val palette: Palette, private val format: (Double)->String) : View(context) {
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    init { contentDescription=labels.zip(values).joinToString(". ") { "${it.first}: ${format(it.second)}" } }
    override fun onDraw(c: Canvas) {
        if(values.isEmpty()) return
        val density=resources.displayMetrics.density
        val max=values.maxOf { abs(it) }.coerceAtLeast(1.0)
        val hasNegative=values.any { it<0 }
        val top=26*density; val bottom=height-26*density
        val zero=if(hasNegative) (top+bottom)/2 else bottom
        val span=if(hasNegative) (bottom-top)/2 else bottom-top
        val slot=width.toFloat()/values.size
        p.color=palette.line; p.strokeWidth=density; c.drawLine(12*density,zero,width-12*density,zero,p)
        values.forEachIndexed { i,v ->
            val x=slot*(i+.5f); val end=zero-(v/max*span).toFloat()
            p.color=if(i==0) palette.accent else palette.muted
            c.drawRoundRect(x-slot*.24f,minOf(end,zero),x+slot*.24f,maxOf(end,zero)+density,5*density,5*density,p)
            p.color=palette.ink; p.textSize=12*resources.displayMetrics.scaledDensity; p.textAlign=Paint.Align.CENTER; p.typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL)
            c.drawText(format(v),x,if(v>=0) end-7*density else end+17*density,p)
            p.color=palette.muted; p.textSize=11*resources.displayMetrics.scaledDensity
            c.drawText(labels.getOrElse(i){""},x,height-6*density,p)
        }
    }
}
