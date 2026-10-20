package com.example.financecalc1

import android.content.Context
import android.graphics.*
import android.util.LruCache
import android.view.View
import kotlin.math.abs

/** Three actual 3D art families. Theme shifts only the colored material, keeping alpha and light. */
class ArtworkIconView(context:Context,symbol:String,style:InterfaceStyle,palette:Palette):View(context){
    private val art=artwork(context,style,palette)
    private val index=listOf("budget","savings","interest","loan","debt","investment","inflation","goal").indexOf(symbol).coerceAtLeast(0)
    private val bounds=when(style){
        InterfaceStyle.WAVE->arrayOf(Rect(102,24,453,391),Rect(561,64,977,405),Rect(99,413,446,760),Rect(586,426,968,753),Rect(102,778,454,1112),Rect(583,786,953,1109),Rect(76,1159,504,1476),Rect(579,1126,1004,1498))
        InterfaceStyle.PRISM->arrayOf(Rect(102,24,460,393),Rect(565,52,971,392),Rect(94,410,448,769),Rect(568,425,961,778),Rect(96,783,457,1124),Rect(573,792,929,1122),Rect(74,1137,487,1474),Rect(567,1121,1003,1501))
        else->arrayOf(Rect(114,21,437,377),Rect(576,61,950,367),Rect(103,408,439,745),Rect(588,427,946,732),Rect(104,783,445,1109),Rect(578,792,929,1108),Rect(80,1160,468,1462),Rect(576,1124,967,1482))
    }[index]
    private val source=Rect(bounds.left*art.width/1024,bounds.top*art.height/1536,bounds.right*art.width/1024,bounds.bottom*art.height/1536)
    private val target=RectF()
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    init {importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_NO}
    override fun onDraw(canvas:Canvas){
        val scale=minOf(width.toFloat()/source.width(),height.toFloat()/source.height())
        val w=source.width()*scale;val h=source.height()*scale
        target.set((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2)
        canvas.drawBitmap(art,source,target,paint)
    }
    companion object {
        private val originals=mutableMapOf<InterfaceStyle,Bitmap>()
        private val variants=LruCache<String,Bitmap>(9)
        private fun artwork(context:Context,style:InterfaceStyle,palette:Palette):Bitmap {
            val key="${style.id}:${palette.id}"
            variants.get(key)?.let {return it}
            val original=originals.getOrPut(style){BitmapFactory.decodeResource(context.resources,when(style){InterfaceStyle.WAVE->R.drawable.finance_artwork_wave;InterfaceStyle.PRISM->R.drawable.finance_artwork_prism;else->R.drawable.finance_artwork_atlas},BitmapFactory.Options().apply {inScaled=false;inSampleSize=2})}
            val sourceHue=when(style){InterfaceStyle.WAVE->330f;InterfaceStyle.ORBIT->80f;InterfaceStyle.PRISM->70f}
            val hsv=FloatArray(3)
            Color.colorToHSV(if(style==InterfaceStyle.ORBIT)palette.highlight else palette.accent,hsv)
            val shift=(hsv[0]-sourceHue+540f)%360f-180f
            if(abs(shift)<1f){variants.put(key,original);return original}
            val pixels=IntArray(original.width*original.height)
            original.getPixels(pixels,0,original.width,0,0,original.width,original.height)
            for(i in pixels.indices){
                val pixel=pixels[i];val alpha=Color.alpha(pixel)
                if(alpha==0)continue
                Color.colorToHSV(pixel,hsv)
                val distance=abs((hsv[0]-sourceHue+540f)%360f-180f)
                if(hsv[1]>.10f&&distance<65f){
                    val weight=if(distance<40f)1f else (65f-distance)/25f
                    hsv[0]=(hsv[0]+shift*weight+360f)%360f
                    pixels[i]=Color.HSVToColor(alpha,hsv)
                }
            }
            val result=Bitmap.createBitmap(pixels,original.width,original.height,Bitmap.Config.ARGB_8888)
            variants.put(key,result);return result
        }
    }
}
