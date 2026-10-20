package com.example.financecalc1

import android.content.Context
import android.graphics.*
import android.view.View

/** Authored finance objects: glossy color for Wave, cut facets for Prism. */
class TileObjectIconView(context:Context,private val symbol:String,source:Palette,private val style:InterfaceStyle,hue:Int):View(context){
    private val palette=source.copy(
        accent=androidx.core.graphics.ColorUtils.blendARGB(if(style==InterfaceStyle.PRISM)source.accent else hue,Color.BLACK,if(style==InterfaceStyle.PRISM).58f else .36f),
        highlight=if(style==InterfaceStyle.PRISM)source.accent else hue
    )
    private val p=Paint(Paint.ANTI_ALIAS_FLAG)
    private fun face(x:Float,y:Float,w:Float,h:Float):Path {
        val cut=minOf(4f,minOf(w,h)/4)
        return Path().apply {moveTo(x+cut,y);lineTo(x+w-cut,y);lineTo(x+w,y+cut);lineTo(x+w,y+h-cut);lineTo(x+w-cut,y+h);lineTo(x+cut,y+h);lineTo(x,y+h-cut);lineTo(x,y+cut);close()}
    }
    private fun body(c:Canvas,x:Float,y:Float,w:Float,h:Float,r:Float=6f,teal:Boolean=false){
        val a=if(teal)palette.accent else palette.highlight
        p.shader=null;p.style=Paint.Style.FILL
        if(style==InterfaceStyle.PRISM){
            val under=face(x+3,y+4,w,h)
            p.color=androidx.core.graphics.ColorUtils.blendARGB(a,Color.BLACK,.48f);c.drawPath(under,p)
            val front=face(x,y,w,h)
            p.shader=LinearGradient(x,y,x+w,y+h,androidx.core.graphics.ColorUtils.blendARGB(a,Color.WHITE,.27f),a,Shader.TileMode.CLAMP)
            c.drawPath(front,p);p.shader=null
            p.style=Paint.Style.STROKE;p.strokeWidth=.8f;p.color=androidx.core.graphics.ColorUtils.blendARGB(a,Color.WHITE,.32f);c.drawPath(front,p);p.style=Paint.Style.FILL
        }else{
            p.color=androidx.core.graphics.ColorUtils.blendARGB(a,Color.BLACK,.23f)
            c.drawRoundRect(x+1.5f,y+3,w+x+1.5f,h+y+3,r,r,p)
            p.shader=LinearGradient(x,y,x+w,y+h,androidx.core.graphics.ColorUtils.blendARGB(a,Color.WHITE,.40f),a,Shader.TileMode.CLAMP)
            c.drawRoundRect(x,y,x+w,y+h,r,r,p);p.shader=null
            if(h>12){p.style=Paint.Style.STROKE;p.strokeWidth=1f;p.color=androidx.core.graphics.ColorUtils.blendARGB(a,Color.WHITE,.58f);c.drawLine(x+r,y+2,x+w-r,y+2,p);p.style=Paint.Style.FILL}
        }
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
