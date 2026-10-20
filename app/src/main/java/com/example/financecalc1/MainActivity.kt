package com.example.financecalc1

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.RippleDrawable
import androidx.core.graphics.ColorUtils
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("esep_preferences", Context.MODE_PRIVATE) }
    private var language = "ru"
    private lateinit var palette: Palette
    private var route = "home"
    private var tool = Tool.BUDGET
    private val drafts = mutableMapOf<String, MutableList<String>>()
    private val modes = mutableMapOf<Tool, Int>()
    private val outputs = mutableMapOf<String, Calculation>()
    private lateinit var root: LinearLayout
    private lateinit var fieldsBody: LinearLayout
    private lateinit var resultsBody: LinearLayout
    private lateinit var formScroll: ScrollView
    private lateinit var resultNumber: TextView
    private lateinit var resultLabel: TextView
    private lateinit var copyButton: View
    private val inputs = mutableListOf<TextInputEditText>()
    private val layouts = mutableListOf<TextInputLayout>()
    private var activeInput = 0
    private var building = false
    private var formulaOpen = false
    private var resultRing: RingView? = null
    private lateinit var resultPanelView: View
    private var nextLabel: TextView? = null
    private var ringCaption: TextView? = null
    private val interfaceStyle get() = InterfaceStyle.from(prefs.getString("interface", "wave"))
    private val chromeColor get() = if (interfaceStyle == InterfaceStyle.WAVE) palette.header else palette.background
    private val chromeInk get() = if (interfaceStyle == InterfaceStyle.WAVE) palette.headerInk else palette.ink
    private val onAccent get() = if (ColorUtils.calculateContrast(Color.BLACK, palette.accent) >= 4.5) Color.BLACK else Color.WHITE
    private fun resolvePalette() = Palette.forStyle(interfaceStyle,prefs.getString("palette_${interfaceStyle.id}", interfaceStyle.defaultPalette))
    private val mode get() = modes[tool] ?: 0
    private val draftKey get() = "${tool.name}:$mode"
    private val customPad get() = prefs.getBoolean("custom_pad", interfaceStyle == InterfaceStyle.WAVE)
    private val largeLayout get() = resources.configuration.screenWidthDp >= 720
    private fun dp(n: Int) = (n * resources.displayMetrics.density + .5f).toInt()
    private fun w(ru: String, kk: String) = if (language == "kk") kk else ru
    private fun Words.local() = inLanguage(language)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!prefs.contains("interface")) prefs.edit().putString("interface", "wave").putString("palette_wave", prefs.getString("theme", "wave")).apply()
        palette = resolvePalette()
        setTheme(when(interfaceStyle){InterfaceStyle.PRISM->R.style.Theme_Esep_Dark;InterfaceStyle.ORBIT->R.style.Theme_Esep_Mint;else->R.style.Theme_Esep_Light})
        super.onCreate(savedInstanceState)
        language = prefs.getString("language", if (Locale.getDefault().language == "kk") "kk" else "ru") ?: "ru"
        savedInstanceState?.let { state ->
            route = state.getString("route", "home")
            tool = Tool.entries.firstOrNull { it.name == state.getString("tool") } ?: Tool.BUDGET
            formulaOpen = state.getBoolean("formula")
            activeInput = state.getInt("active")
            Tool.entries.forEach { t ->
                modes[t] = state.getInt("mode_${t.name}")
                for (m in 0..1) {
                    val key = "${t.name}:$m"
                    state.getStringArrayList(key)?.let { drafts[key] = it.toMutableList() }
                    if (state.getBoolean("result_$key")) runCatching { FinanceEngine.calculate(t, m, drafts[key].orEmpty()) }.getOrNull()?.let { outputs[key] = it }
                }
            }
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (route != "home") { route = "home"; hideKeyboard(); render() }
                else { isEnabled = false; onBackPressedDispatcher.onBackPressed() }
            }
        })
        render()
        if (!prefs.getBoolean("welcome_seen", false)) showWelcome()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("route", route); outState.putString("tool", tool.name)
        outState.putBoolean("formula", formulaOpen); outState.putInt("active", activeInput)
        modes.forEach { (t, m) -> outState.putInt("mode_${t.name}", m) }
        drafts.forEach { (key, values) -> outState.putStringArrayList(key, ArrayList(values)) }
        outputs.keys.forEach { outState.putBoolean("result_$it", true) }
    }

    private fun vertical() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun horizontal() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    private fun text(value: String, size: Float = 16f, color: Int = palette.ink, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        includeFontPadding = false
    }
    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }
    private fun LinearLayout.add(view: View, width: Int = Int.MIN_VALUE, height: Int = Int.MIN_VALUE) {
        val previous = view.layoutParams
        val actualWidth = if (width == Int.MIN_VALUE) previous?.width ?: if (orientation == LinearLayout.HORIZONTAL) -2 else -1 else if (width >= 0) dp(width) else width
        val actualHeight = if (height == Int.MIN_VALUE) previous?.height ?: -2 else if (height >= 0) dp(height) else height
        addView(view, LinearLayout.LayoutParams(actualWidth, actualHeight))
    }
    private fun pad(view: View, x: Int, y: Int = x) { view.setPadding(dp(x), dp(y), dp(x), dp(y)) }
    private fun action(label: String, onClick: () -> Unit, fill: Int = Color.TRANSPARENT, ink: Int = palette.accent): TextView = text(label, 14f, ink, true).apply {
        minHeight = dp(48); gravity = Gravity.CENTER; pad(this, 14, 10)
        background = shape(fill, dp(14).toFloat()); isClickable = true; isFocusable = true
        foreground = feedback(14, ink)
        setOnClickListener { onClick() }; contentDescription = label
    }
    private fun iconAction(symbol: String, label: String, color: Int, onClick: () -> Unit): FrameLayout = FrameLayout(this).apply {
        val icon = SymbolView(this@MainActivity, symbol, color)
        addView(icon, FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER))
        minimumWidth = dp(48); minimumHeight = dp(48)
        isClickable = true; isFocusable = true; contentDescription = label
        foreground = feedback(24, color)
        setOnClickListener { onClick() }
    }

    private fun render() {
        building = true
        nextLabel = null
        palette = resolvePalette()
        root = vertical().apply { setBackgroundColor(palette.background) }
        setContentView(root)
        @Suppress("DEPRECATION")
        window.statusBarColor = chromeColor
        @Suppress("DEPRECATION")
        window.navigationBarColor = palette.background
        WindowCompat.getInsetsController(window, root).apply {
            isAppearanceLightStatusBars = ColorUtils.calculateLuminance(chromeColor) > .45
            isAppearanceLightNavigationBars = interfaceStyle != InterfaceStyle.PRISM
        }
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            root.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, ime.bottom))
            root.background = object : android.graphics.drawable.Drawable() {
                private val paint = android.graphics.Paint()
                override fun draw(canvas: android.graphics.Canvas) {
                    canvas.drawColor(palette.background)
                    paint.color = chromeColor
                    canvas.drawRect(0f, 0f, bounds.width().toFloat(), bars.top.toFloat(), paint)
                }
                override fun setAlpha(alpha: Int) {}
                override fun setColorFilter(filter: android.graphics.ColorFilter?) {}
                @Suppress("DEPRECATION") override fun getOpacity() = android.graphics.PixelFormat.OPAQUE
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
        when (route) { "calculator" -> calculator(); "settings" -> settings(); else -> home() }
        building = false
    }

    private fun topBar(title: String, color: Int = chromeInk): LinearLayout = horizontal().apply {
        setBackgroundColor(chromeColor); pad(this, 8, 3)
        add(iconAction("back", w("Назад", "Артқа"), color) { onBackPressedDispatcher.onBackPressed() }, 48, 48)
        addView(text(title, 19f, color, true), LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = dp(8) })
    }

    private fun feedback(radius: Int = 16, ink: Int = palette.ink) = RippleDrawable(ColorStateList.valueOf(ColorUtils.setAlphaComponent(ink, 24)), null, shape(Color.WHITE, dp(radius).toFloat()))
    private fun clickable(view: View, radius: Int = 16, description: String? = null, click: () -> Unit) {
        view.isClickable=true;view.isFocusable=true;view.foreground=feedback(radius)
        description?.let {view.contentDescription=it};view.setOnClickListener {click()}
    }
    private fun sectionTitle(body: LinearLayout, ru: String, kk: String) {body.add(space(23));body.add(text(w(ru,kk),18f,bold=true));body.add(space(12))}
    private fun openTool(value: Tool) {tool=value;activeInput=0;route="calculator";render()}
    private fun example() {FinanceEngine.inputs(tool,mode).forEachIndexed {i,spec->inputs[i].setText(spec.example)};runCalculation()}
    private fun modeTabs(): View? {
        val variants=FinanceEngine.modes(tool);if(variants.isEmpty())return null
        return horizontal().apply {
            background=shape(palette.surface,dp(if(interfaceStyle==InterfaceStyle.PRISM)12 else 25).toFloat());pad(this,3,3)
            variants.forEachIndexed {i,label->addView(action(label.local(),{hideKeyboard();modes[tool]=i;activeInput=0;render()},if(mode==i)palette.accent else Color.TRANSPARENT,if(mode==i)onAccent else palette.ink).apply {isSelected=mode==i;tag="mode_$i"},LinearLayout.LayoutParams(0,-2,1f))}
        }
    }
    private fun home() {
        val style=interfaceStyle
        val hero=vertical().apply {setBackgroundColor(chromeColor);pad(this,20,10)}
        val nav=horizontal()
        nav.addView(text("Есеп",32f,if(style==InterfaceStyle.PRISM)palette.accent else chromeInk,true),LinearLayout.LayoutParams(0,-2,1f))
        nav.add(action(if(language=="kk")"ҚАЗ · RU" else "RU · ҚАЗ",{changeLanguage(if(language=="kk")"ru" else "kk")},ink=chromeInk))
        nav.add(iconAction("settings",w("Настройки","Баптаулар"),chromeInk){route="settings";render()},48,48)
        hero.add(nav);hero.add(space(8));hero.add(text(w("Финансовые калькуляторы","Қаржы калькуляторлары"),if(style==InterfaceStyle.PRISM)24f else 16f,chromeInk,style==InterfaceStyle.PRISM))
        hero.add(space(if(style==InterfaceStyle.WAVE)12 else 4));root.add(hero)
        if(style==InterfaceStyle.WAVE)root.add(WaveView(this,palette),height=26)
        val scroll=ScrollView(this).apply {clipToPadding=false};val body=vertical().apply {pad(this,16,12)}
        val columns=if(largeLayout)4 else 2;val grid=GridLayout(this).apply {columnCount=columns}
        val hues=if(style==InterfaceStyle.WAVE)listOf("#AE43C7","#318BD0","#CC347F","#358A83","#AF8021","#C63680","#7360C9","#B73C9A").map {Color.parseColor(it)} else List(8){palette.accent}
        Tool.entries.forEachIndexed {i,t->
            val ink=if(style==InterfaceStyle.WAVE)hues[i] else palette.accent
            val tile=vertical().apply {
                gravity=if(style==InterfaceStyle.ORBIT)Gravity.START else Gravity.CENTER;pad(this,14,if(style==InterfaceStyle.ORBIT)12 else 16);minimumHeight=dp(if(style==InterfaceStyle.ORBIT)140 else 122)
                background=shape(if(style==InterfaceStyle.WAVE)ColorUtils.setAlphaComponent(ink,14) else if(style==InterfaceStyle.ORBIT)Color.WHITE else palette.surface,dp(if(style==InterfaceStyle.PRISM)14 else 20).toFloat())
                add(ArtworkIconView(this@MainActivity,t.icon,style,palette),56,56)
                add(space(10));add(text(t.title.local(),15f,bold=true).apply {gravity=if(style==InterfaceStyle.ORBIT)Gravity.START else Gravity.CENTER;maxLines=3})
                if(style==InterfaceStyle.ORBIT){add(space(5));add(text(t.description.local(),12f,palette.muted).apply {gravity=if(style==InterfaceStyle.ORBIT)Gravity.START else Gravity.CENTER;maxLines=3})}
                tag="tool_${t.name}";clickable(this,if(style==InterfaceStyle.PRISM)14 else 20,"${t.title.local()}. ${t.description.local()}"){openTool(t)}
            }
            grid.addView(tile,GridLayout.LayoutParams(GridLayout.spec(i/columns),GridLayout.spec(i%columns,1f)).apply {width=0;height=-2;setGravity(Gravity.FILL);setMargins(dp(5),0,dp(5),dp(10))})
        }
        body.add(grid);body.add(text(w("Все расчёты — на вашем устройстве","Барлық есеп — өз құрылғыңызда"),12f,palette.muted).apply {gravity=Gravity.CENTER;pad(this,4,15)})
        scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    }

    private fun calculator() {
        inputs.clear();layouts.clear();resultRing=null;ringCaption=null
        val style=interfaceStyle
        val bar=topBar(tool.title.local())
        bar.add(iconAction("formula",w("Формула и обозначения","Формула және белгілер"),chromeInk){showFormula()},48,48)
        if(style==InterfaceStyle.ORBIT)bar.add(action(w("Пример","Мысал"),{example()},ink=chromeInk)) else bar.add(iconAction("keyboard",w("Сменить клавиатуру","Пернетақтаны ауыстыру"),chromeInk){hideKeyboard();prefs.edit().putBoolean("custom_pad",!customPad).apply();render()},48,48)
        root.add(bar);resultPanelView=resultPanel()
        if(style==InterfaceStyle.WAVE){root.add(resultPanelView);root.add(WaveView(this,palette),height=25)}
        val split=largeLayout&&customPad
        val workspace=LinearLayout(this).apply {orientation=if(split)LinearLayout.HORIZONTAL else LinearLayout.VERTICAL}
        root.addView(workspace,LinearLayout.LayoutParams(-1,0,1f))
        formScroll=ScrollView(this).apply {clipToPadding=false};fieldsBody=vertical().apply {pad(this,20,8)};resultsBody=vertical()
        if(style==InterfaceStyle.WAVE)fieldsBody.add(resultsBody)
        else {modeTabs()?.let {fieldsBody.add(it);fieldsBody.add(space(10))};if(style!=InterfaceStyle.ORBIT)fieldsBody.add(horizontal().apply {gravity=Gravity.END;add(action(w("Пример","Мысал"),{example()}))})}
        val specs=FinanceEngine.inputs(tool,mode);val values=drafts.getOrPut(draftKey){MutableList(specs.size){""}}
        specs.forEachIndexed {i,spec->
            val layout=TextInputLayout(this).apply {
                hint=spec.label.local();boxBackgroundMode=if(style==InterfaceStyle.WAVE)TextInputLayout.BOX_BACKGROUND_OUTLINE else TextInputLayout.BOX_BACKGROUND_FILLED
                boxStrokeWidth=if(style==InterfaceStyle.WAVE)dp(1) else 0;boxStrokeWidthFocused=if(style==InterfaceStyle.WAVE)dp(2) else 0
                boxBackgroundColor=if(style==InterfaceStyle.WAVE)palette.background else if(style==InterfaceStyle.ORBIT&&i==0)palette.focusFill else palette.surface
                setBoxStrokeColorStateList(ColorStateList(arrayOf(intArrayOf(android.R.attr.state_focused),intArrayOf()),intArrayOf(palette.accent,if(style==InterfaceStyle.WAVE)palette.line else Color.TRANSPARENT)))
                defaultHintTextColor=ColorStateList.valueOf(palette.muted);hintTextColor=ColorStateList.valueOf(palette.accent)
                val radius=dp(if(style==InterfaceStyle.ORBIT)18 else 11).toFloat();setBoxCornerRadii(radius,radius,radius,radius)
                endIconMode=TextInputLayout.END_ICON_CLEAR_TEXT;endIconContentDescription=w("Очистить поле","Өрісті тазарту");setEndIconTintList(ColorStateList.valueOf(palette.muted))
            }
            val field=TextInputEditText(layout.context).apply {
                id=View.generateViewId();tag="field_$i";setTextColor(palette.ink);textSize=20f;minimumHeight=dp(if(style==InterfaceStyle.ORBIT)56 else 60)
                typeface=Typeface.create("sans-serif-medium",Typeface.NORMAL);fontFeatureSettings="tnum";setSingleLine(true)
                val numericType=InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or if(spec.min<0)InputType.TYPE_NUMBER_FLAG_SIGNED else 0
                keyListener=android.text.method.DigitsKeyListener.getInstance(if(spec.min<0)"0123456789,.- " else "0123456789,. ");setRawInputType(numericType)
                showSoftInputOnFocus=!customPad;imeOptions=if(i==specs.lastIndex)android.view.inputmethod.EditorInfo.IME_ACTION_DONE else android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
                setText(NumberEditing.group(values.getOrElse(i){""}))
                setOnEditorActionListener {_,action,_->when(action){android.view.inputmethod.EditorInfo.IME_ACTION_DONE->{hideKeyboard();runCalculation();true};android.view.inputmethod.EditorInfo.IME_ACTION_NEXT->{nextInput();true};else->false}}
            }
            var formatting=false;var old="";var changedAt=0;var removed=0;var inserted=0;var oldCaret=0
            field.addTextChangedListener(object:TextWatcher{
                override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){if(!formatting){old=s.toString();changedAt=start;removed=count;inserted=after;oldCaret=field.selectionStart}}
                override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){}
                override fun afterTextChanged(s:Editable?){
                    if(formatting)return
                    val current=s.toString();val edit=NumberEditing.update(current,if(field.selectionStart<0)current.length else field.selectionStart,old,changedAt,removed,inserted,oldCaret)
                    if(edit.text!=current){formatting=true;field.setText(edit.text);field.setSelection(edit.caret.coerceIn(0,field.length()));formatting=false}
                    values[i]=field.text.toString();layout.error=null
                    if(!building){outputs.remove(draftKey);clearResult()}
                }
            })
            layout.addView(field,LinearLayout.LayoutParams(-1,-2));inputs.add(field);layouts.add(layout)
            val materialFocus=field.onFocusChangeListener
            field.setOnFocusChangeListener {view,focused->materialFocus?.onFocusChange(view,focused);if(focused){activeInput=i;updateNextLabel()}}
            fieldsBody.add(layout);fieldsBody.add(space(if(style==InterfaceStyle.ORBIT)8 else 12))
        }
        if(style!=InterfaceStyle.WAVE){fieldsBody.add(space(8));fieldsBody.add(resultPanelView);fieldsBody.add(space(12));fieldsBody.add(resultsBody)}
        fieldsBody.add(space(12));formScroll.addView(fieldsBody)
        workspace.addView(formScroll,if(split)LinearLayout.LayoutParams(0,-1,1f) else LinearLayout.LayoutParams(-1,0,1f))
        if(customPad)workspace.addView(keypad(),if(split)LinearLayout.LayoutParams(dp(310),-1) else LinearLayout.LayoutParams(-1,-2))
        else {
            val footer=vertical().apply {pad(this,20,10)}
            footer.add(action(w("Рассчитать","Есептеу"),{hideKeyboard();runCalculation()},if(style==InterfaceStyle.PRISM)palette.accent else palette.key,if(style==InterfaceStyle.PRISM)onAccent else palette.keyInk).apply {tag="calculate";textSize=17f;minimumHeight=dp(54)})
            workspace.add(footer)
        }
        outputs[draftKey]?.let {displayResult(it)}
        if(customPad)inputs.getOrNull(activeInput.coerceAtMost(inputs.lastIndex))?.requestFocus()
        updateNextLabel()
    }
    private fun resultPanel(): View {
        val style=interfaceStyle
        val ink=if(style==InterfaceStyle.WAVE)palette.headerInk else if(style==InterfaceStyle.PRISM)onAccent else palette.ink
        resultLabel=text(w("Заполните поля ниже","Төмендегі өрістерді толтырыңыз"),14f,ink).apply {gravity=if(style==InterfaceStyle.PRISM)Gravity.START else Gravity.CENTER}
        resultNumber=text("—",if(style==InterfaceStyle.ORBIT)32f else 36f,ink,true).apply {tag="result_value";fontFeatureSettings="tnum";gravity=resultLabel.gravity;pad(this,0,6);accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE}
        copyButton=action(w("Копировать","Көшіру"),{copyResult()},ink=ink).apply {visibility=View.GONE;tag="copy_result"}
        if(style==InterfaceStyle.ORBIT)return vertical().apply {
            val disk=FrameLayout(this@MainActivity);val ring=RingView(this@MainActivity,palette).apply {solid=tool==Tool.BUDGET||tool==Tool.DEBT;reversed=tool==Tool.DEBT};resultRing=ring
            disk.addView(ring,FrameLayout.LayoutParams(dp(218),dp(218),Gravity.CENTER))
            val center=vertical().apply {gravity=Gravity.CENTER;pad(this,10);if(tool!=Tool.BUDGET&&tool!=Tool.DEBT){add(SymbolView(this@MainActivity,tool.icon,palette.accent),27,27);add(space(8))};add(resultLabel);add(resultNumber);ringCaption=text("",12f,palette.muted).apply {gravity=Gravity.CENTER};add(ringCaption!!)}
            disk.addView(center,FrameLayout.LayoutParams(dp(170),-2,Gravity.CENTER));add(disk,height=224);add(copyButton)
        }
        return vertical().apply {
            setBackgroundColor(if(style==InterfaceStyle.WAVE)palette.header else palette.accent)
            if(style==InterfaceStyle.PRISM)background=shape(palette.accent,dp(14).toFloat())
            pad(this,if(style==InterfaceStyle.WAVE)22 else 18,if(style==InterfaceStyle.WAVE)8 else 17)
            if(style==InterfaceStyle.WAVE)modeTabs()?.let {add(it);add(space(12))}
            add(resultLabel);add(resultNumber)
            val actions=horizontal().apply {gravity=if(style==InterfaceStyle.WAVE)Gravity.CENTER else Gravity.END}
            if(style==InterfaceStyle.WAVE)actions.add(action(w("Пример","Мысал"),{example()},ink=ink))
            actions.add(copyButton);add(actions)
        }
    }

    private fun clearResult() {
        if (!::resultNumber.isInitialized) return
        resultNumber.text = "—"; resultLabel.text = if(interfaceStyle==InterfaceStyle.WAVE)w("Нажмите = для расчёта", "Есептеу үшін = басыңыз") else w("Нажмите «Рассчитать»", "«Есептеу» түймесін басыңыз")
        resultRing?.fraction=null
        copyButton.visibility = View.GONE; resultsBody.removeAllViews()
    }

    private fun keyboardControl(icon: String, label: String, click: () -> Unit): View = horizontal().apply {
        gravity=Gravity.CENTER;pad(this,12,7);background=shape(palette.surface,dp(12).toFloat());minimumHeight=dp(46)
        add(SymbolView(this@MainActivity,icon,palette.muted),19,19);addView(View(this@MainActivity),LinearLayout.LayoutParams(dp(7),1))
        val caption=text(label,14f,palette.ink,true);add(caption);if(icon=="next")nextLabel=caption
        clickable(this,12,label,click)
    }
    private fun keypad(): LinearLayout {
        val panel=vertical().apply {pad(this,14,8);setBackgroundColor(palette.background)}
        val tools=horizontal()
        tools.addView(keyboardControl("reset",w("Очистить","Тазарту")){inputs.forEach {it.text?.clear()};inputs.firstOrNull()?.requestFocus()},LinearLayout.LayoutParams(0,-2,1f).apply {marginEnd=dp(8)})
        if(tool==Tool.INFLATION)tools.add(action("±",{typeKey("±")},palette.surface,palette.ink),48,48)
        tools.addView(keyboardControl("next",w("Далее","Келесі")){nextInput()},LinearLayout.LayoutParams(0,-2,1f))
        panel.add(tools);panel.add(space(8));val row=horizontal();val keys=vertical()
        val h=if(resources.configuration.screenHeightDp<700)48 else 52
        listOf(listOf("1","2","3"),listOf("4","5","6"),listOf("7","8","9"),listOf(",","0","delete")).forEach {labels->
            val line=horizontal()
            labels.forEach {label->
                val key=if(label=="delete")iconAction("delete",w("Удалить цифру","Цифрды өшіру"),palette.ink){typeKey("delete")}.apply {background=shape(palette.surface,dp(12).toFloat());setOnLongClickListener {inputs.getOrNull(activeInput)?.text?.clear();true}}
                else action(label,{typeKey(label)},palette.surface,palette.ink).apply {textSize=23f;fontFeatureSettings="tnum";tag="key_$label"}
                line.addView(key,LinearLayout.LayoutParams(0,dp(h),1f).apply {setMargins(dp(2),dp(2),dp(2),dp(2))})
            };keys.add(line)
        }
        row.addView(keys,LinearLayout.LayoutParams(0,-2,1f))
        row.addView(action("=",{runCalculation()},if(interfaceStyle==InterfaceStyle.PRISM)palette.accent else palette.key,if(interfaceStyle==InterfaceStyle.PRISM)onAccent else palette.keyInk).apply {tag="calculate";textSize=34f;contentDescription=w("Рассчитать","Есептеу")},LinearLayout.LayoutParams(dp(68),dp((h+4)*4-4)).apply {marginStart=dp(8);marginEnd=dp(2)})
        panel.add(row);return panel
    }
    private fun updateNextLabel() {nextLabel?.text=if(activeInput==inputs.lastIndex)w("Готово","Дайын") else w("Далее","Келесі")}
    private fun typeKey(key: String) {
        val field = inputs.getOrNull(activeInput) ?: return
        if (!field.hasFocus()) field.requestFocus()
        val editable = field.text ?: return
        val start = field.selectionStart.coerceAtLeast(0); val end = field.selectionEnd.coerceAtLeast(start)
        when (key) {
            "delete" -> if(end>start)editable.delete(start,end) else NumberEditing.backspaceRange(editable.toString(),start)?.let {editable.delete(it.first,it.last+1)}
            "±" -> if (editable.startsWith("-")) editable.delete(0, 1) else editable.insert(0, "-")
            else -> {
                val remaining=editable.toString().removeRange(start,end)
                if(key==","&&remaining.any {it==','||it=='.'})return
                if(remaining.count {!it.isWhitespace()}<20)editable.replace(start,end,key)
            }
        }
    }
    private fun nextInput() {
        if(activeInput==inputs.lastIndex){hideKeyboard();runCalculation();return}
        activeInput=(activeInput+1).coerceAtMost(inputs.lastIndex)
        inputs[activeInput].requestFocus();inputs[activeInput].setSelection(inputs[activeInput].length());updateNextLabel()
    }
    private fun runCalculation() {
        layouts.forEach { it.error = null }
        try {
            val result = FinanceEngine.calculate(tool, mode, inputs.map { it.text.toString() })
            outputs[draftKey] = result; displayResult(result)
            if(interfaceStyle==InterfaceStyle.WAVE)formScroll.smoothScrollTo(0,0) else formScroll.post {formScroll.smoothScrollTo(0,if(interfaceStyle==InterfaceStyle.ORBIT)(resultPanelView.top+dp(224)-formScroll.height+dp(8)).coerceAtLeast(0) else (resultPanelView.top-dp(12)).coerceAtLeast(0))}
            if (android.os.Build.VERSION.SDK_INT < 26 || android.animation.ValueAnimator.areAnimatorsEnabled()) resultNumber.apply { alpha = .45f; animate().alpha(1f).setDuration(160).start() }
        } catch (problem: InputProblem) {
            val i = problem.index.coerceIn(inputs.indices)
            val spec = FinanceEngine.inputs(tool, mode)[i]
            val message = when (problem.reason) {
                "required" -> w("Введите значение", "Мәнді енгізіңіз")
                "number" -> w("Введите число, например 1,5", "Сан енгізіңіз, мысалы 1,5")
                "integer" -> w("Нужно целое число", "Бүтін сан қажет")
                "overflow" -> w("Слишком большой результат. Уменьшите значения.", "Нәтиже тым үлкен. Мәндерді азайтыңыз.")
                else -> w("Допустимо: ${if (spec.strictlyPositive) "> 0" else format(spec.min)}…${format(spec.max)}", "Рұқсат етілген: ${if (spec.strictlyPositive) "> 0" else format(spec.min)}…${format(spec.max)}")
            }
            layouts[i].error = message; inputs[i].requestFocus(); layouts[i].announceForAccessibility(message)
        }
    }
    private fun format(value: Double): String = NumberFormat.getNumberInstance(if (language == "kk") Locale.forLanguageTag("kk-KZ") else Locale.forLanguageTag("ru-RU")).apply {
        maximumFractionDigits = 2; minimumFractionDigits = 0
    }.format(if (kotlin.math.abs(value) < .0000001) 0.0 else value)

    private fun displayResult(result: Calculation) {
        resultLabel.text=if(interfaceStyle==InterfaceStyle.ORBIT&&tool==Tool.BUDGET)w("Остаток","Қалдық") else result.label.local();resultNumber.text="${format(result.value)} ${result.unit.local()}"
        resultNumber.textSize=if(interfaceStyle==InterfaceStyle.ORBIT){if(resultNumber.text.length>12)23f else 29f} else if(resultNumber.text.length>21)27f else 36f
        copyButton.visibility=View.VISIBLE;resultsBody.removeAllViews()
        resultRing?.fraction=when(tool){
            Tool.DEBT->(result.value/100).toFloat().coerceIn(0f,1f)
            Tool.BUDGET->FinanceEngine.parse(inputs[0].text.toString())?.takeIf {it>0}?.let {(result.value/it).toFloat().coerceIn(0f,1f)}
            Tool.SAVINGS,Tool.INTEREST,Tool.LOAN->result.graphValues.takeIf {it.size==2&&it.sum()>0}?.let {(it[1]/it.sum()).toFloat().coerceIn(0f,1f)}
            Tool.INVESTMENT->null
            else->null
        }
        ringCaption?.text=if(tool==Tool.BUDGET)FinanceEngine.parse(inputs[0].text.toString())?.takeIf {it>0}?.let {"${format(result.value/it*100)}% "+w("от дохода","кірістен")}?:"" else if(tool==Tool.LOAN)w("в месяц","айына") else ""
        if(result.details.isNotEmpty()) {
            if(interfaceStyle==InterfaceStyle.WAVE){
                val items=horizontal().apply {gravity=Gravity.TOP}
                result.details.forEach {(label,value)->
                    val item=vertical().apply {pad(this,10,12);background=shape(palette.surface,dp(12).toFloat());add(text(label.local(),11f,palette.muted));add(space(6));add(text("${format(value)} ₸",15f,bold=true).apply {fontFeatureSettings="tnum"})}
                    items.addView(item,LinearLayout.LayoutParams(0,-2,1f).apply {setMargins(dp(3),0,dp(3),0)})
                };resultsBody.add(items);resultsBody.add(space(12))
            } else if(interfaceStyle==InterfaceStyle.ORBIT) {
                val pairs=when(tool){
                    Tool.BUDGET->listOf(Words("Доходы","Кіріс") to FinanceEngine.parse(inputs[0].text.toString())!!,Words("Расходы","Шығын") to FinanceEngine.parse(inputs[1].text.toString())!!,Words("Остаток","Қалдық") to result.value)
                    Tool.DEBT->{val income=FinanceEngine.parse(inputs[1].text.toString())!!;val payment=FinanceEngine.parse(inputs[0].text.toString())!!;listOf(Words("Платежи","Төлемдер") to payment,Words("Остаток дохода","Қалған кіріс") to (income-payment))}
                    else->result.details
                }
                pairs.forEachIndexed {i,(label,value)->
                    val row=horizontal().apply {pad(this,0,8)}
                    row.add(View(this).apply {background=shape(if((tool==Tool.BUDGET&&i==1)||(tool!=Tool.BUDGET&&i==0))palette.accent else palette.highlight,dp(7).toFloat())},12,12)
                    row.addView(text(label.local(),14f),LinearLayout.LayoutParams(0,-2,1f).apply {marginStart=dp(9);marginEnd=dp(8)})
                    row.add(text("${format(value)} ₸",15f,bold=true));resultsBody.add(row)
                };resultsBody.add(space(8))
            } else {
                val table=vertical().apply {pad(this,14,7);background=shape(palette.surface,dp(14).toFloat())}
                result.details.forEach {(label,value)->table.add(horizontal().apply {pad(this,0,9);addView(text(label.local(),13f,palette.muted),LinearLayout.LayoutParams(0,-2,1f).apply {marginEnd=dp(12)});add(text("${format(value)} ₸",16f,bold=true).apply {fontFeatureSettings="tnum"})})}
                resultsBody.add(table);resultsBody.add(space(12))
            }
        }
        if(result.graphValues.isNotEmpty()&&interfaceStyle!=InterfaceStyle.ORBIT){
            val percent=tool==Tool.INFLATION
            resultsBody.add(ComparisonView(this,result.graphValues,result.graphLabels.map {it.local()},palette){"${format(it)}${if(percent)"%" else " ₸"}"},height=126);resultsBody.add(space(12))
        }
        result.note?.let {resultsBody.add(text(it.local(),12f,palette.muted));resultsBody.add(space(12))}
    }
    private fun copyResult() {
        val result = outputs[draftKey] ?: return
        val copy = "${tool.title.local()}\n${result.label.local()}: ${format(result.value)} ${result.unit.local()}\n${result.details.joinToString("\n") { "${it.first.local()}: ${format(it.second)} ₸" }}"
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Есеп", copy))
        Toast.makeText(this, w("Результат скопирован", "Нәтиже көшірілді"), Toast.LENGTH_SHORT).show()
    }
    private fun hideKeyboard() { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(root.windowToken, 0) }
    private fun changeLanguage(value: String) { language = value; prefs.edit().putString("language", value).apply(); render() }

    private fun showFormula() {
        hideKeyboard();val dialog=BottomSheetDialog(this)
        val sheet=vertical().apply {background=shape(palette.background,dp(26).toFloat());pad(this,20,10)}
        sheet.add(View(this).apply {background=shape(palette.line,dp(2).toFloat())},width=38,height=4)
        val heading=horizontal()
        heading.addView(text(w("Формула и обозначения","Формула және белгілер"),20f,bold=true),LinearLayout.LayoutParams(0,-2,1f))
        heading.add(iconAction("close",w("Закрыть формулы","Формуланы жабу"),palette.ink){dialog.dismiss()},48,48)
        sheet.add(heading);sheet.add(text(tool.title.local(),14f,palette.muted));sheet.add(space(18))
        val scroll=ScrollView(this);val content=vertical()
        val formulas=if(tool==Tool.BUDGET)listOf("B = D − E","50% = B × 0,5","30% = B × 0,3","20% = B × 0,2") else FinanceEngine.formula(tool,mode).split("\n")
        formulas.forEach {expression->
            val line=HorizontalScrollView(this).apply {isHorizontalScrollBarEnabled=false;background=shape(palette.surface,dp(14).toFloat());pad(this,16,15);addView(text(expression,22f,palette.ink,true).apply {setTextIsSelectable(true)})}
            content.add(line);content.add(space(8))
        }
        content.add(space(12));content.add(text(w("Что означает каждый символ","Әр белгі нені білдіреді"),16f,bold=true));content.add(space(10))
        val raw=inputs.map {it.text.toString()};val calculation=runCatching {FinanceEngine.calculate(tool,mode,raw)}.getOrNull()
        val mapping=when(tool){Tool.BUDGET->mapOf("D" to 0,"E" to 1);Tool.SAVINGS,Tool.INTEREST->mapOf("P" to 0,"r" to 1,"t" to 2,"n" to 3);Tool.LOAN->mapOf("P" to 0,"r" to 1,"n" to 2);Tool.DEBT->mapOf("M" to 0,"D" to 1);Tool.INVESTMENT->mapOf("P" to 0,"V" to 1);Tool.INFLATION->mapOf("R" to 0,"i" to 1);Tool.GOAL->mapOf("F" to 0,"r" to 1,"t" to 2,"PV" to -1)}
        FinanceEngine.symbols(tool,mode).local().split(';').forEach {entry->
            val parts=entry.trim().split('—',limit=2)
            if(parts.size==2){
                val symbol=parts[0].trim()
                if(!(tool==Tool.INTEREST&&mode==0&&symbol=="n")){
                    val row=horizontal().apply {pad(this,0,12)};row.add(text(symbol,22f,palette.accent,true),width=38)
                    row.addView(text(parts[1].trim().trimEnd('.'),14f,palette.muted),LinearLayout.LayoutParams(0,-2,1f).apply {marginEnd=dp(10)})
                    val index=mapping[symbol]
                    val number=if(index!=null&&index>=0)raw.getOrNull(index)?.let {FinanceEngine.parse(it)} else if(symbol=="B"||symbol=="PV")calculation?.value else null
                    if(number!=null)row.add(text(if(symbol=="r"&&index!=null)NumberEditing.rateFraction(raw[index]).orEmpty() else format(number),15f,bold=true).apply {maxWidth=dp(130)})
                    content.add(row);content.add(View(this).apply {setBackgroundColor(palette.line)},height=1)
                }
            } else if(entry.isNotBlank())content.add(text(entry.trim(),13f,palette.muted))
        }
        calculation?.let {value->
            content.add(space(20));content.add(text(w("По вашим значениям","Сіздің мәндеріңіз бойынша"),14f,palette.muted));content.add(space(8));content.add(text("${format(value.value)} ${value.unit.local()}",28f,palette.accent,true))
            value.note?.let {content.add(space(9));content.add(text(it.local(),13f,palette.muted))}
        }
        content.add(space(24));scroll.addView(content);sheet.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));dialog.setContentView(sheet)
        dialog.setOnShowListener {
            val parent=sheet.parent as View;parent.setBackgroundColor(Color.TRANSPARENT)
            sheet.layoutParams=sheet.layoutParams.apply {height=dp((resources.configuration.screenHeightDp*.82f).toInt().coerceAtMost(720))}
            parent.layoutParams=parent.layoutParams.apply {height=dp((resources.configuration.screenHeightDp*.82f).toInt().coerceAtMost(720))}
            BottomSheetBehavior.from(parent).apply {state=BottomSheetBehavior.STATE_EXPANDED;skipCollapsed=true}
            dialog.window?.let {win->WindowCompat.getInsetsController(win,sheet).isAppearanceLightNavigationBars=interfaceStyle!=InterfaceStyle.PRISM;win.navigationBarColor=palette.background}
        };dialog.show()
    }
    private fun settings() {
        root.add(topBar(w("Настройки","Баптаулар")))
        val scroll=ScrollView(this);val body=vertical().apply {pad(this,20,6)}
        sectionTitle(body,"Оформление","Безендіру")
        val styles=horizontal().apply {gravity=Gravity.TOP}
        InterfaceStyle.entries.forEach {style->
            val p=Palette.forStyle(style,prefs.getString("palette_${style.id}",style.defaultPalette))
            val selected=interfaceStyle==style
            val cell=vertical().apply {
                gravity=Gravity.CENTER;pad(this,8,10);background=shape(palette.surface,dp(16).toFloat(),if(selected)palette.accent else palette.line)
                add(InterfacePreview(this@MainActivity,style,p),54,84);add(space(9));add(text(style.title.local(),14f,bold=true))
                add(space(8));if(selected)add(SymbolView(this@MainActivity,"check",palette.accent),18,18) else add(View(this@MainActivity).apply {background=shape(Color.TRANSPARENT,dp(9).toFloat(),palette.line)},18,18)
                tag="interface_${style.id}";isSelected=selected
                clickable(this,16,style.title.local()+if(selected)w(", выбрано",", таңдалған") else "") {if(interfaceStyle!=style){prefs.edit().putString("interface",style.id).apply();recreate()}}
            };styles.addView(cell,LinearLayout.LayoutParams(0,-2,1f).apply {setMargins(dp(3),0,dp(3),0)})
        };body.add(styles);body.add(space(12))
        body.add(text(interfaceStyle.description.local(),13f,palette.muted));body.add(space(18))
        body.add(text(w("Цвет акцента","Акцент түсі"),16f,bold=true));body.add(space(10))
        val colors=horizontal()
        val labels=when(interfaceStyle){
            InterfaceStyle.WAVE->listOf(Words("Ягодный","Жидек"),Words("Бирюза","Көгілдір"),Words("Индиго","Индиго"))
            InterfaceStyle.ORBIT->listOf(Words("Аква","Аква"),Words("Лайм","Лайм"),Words("Песочный","Құм"))
            InterfaceStyle.PRISM->listOf(Words("Лаванда","Лаванда"),Words("Лёд","Мұз"),Words("Лайм","Лайм"))
        }
        listOf("wave","mint","graphite").forEachIndexed {i,id->
            val p=Palette.forStyle(interfaceStyle,id);val selected=palette.id==id
            val cell=horizontal().apply {
                gravity=Gravity.CENTER;pad(this,5,11);minimumHeight=dp(48)
                background=shape(palette.surface,dp(13).toFloat(),if(selected)palette.accent else palette.line)
                add(ArtworkIconView(this@MainActivity,"budget",interfaceStyle,p),28,28)
                addView(text(labels[i].local(),12f,bold=selected),LinearLayout.LayoutParams(-2,-2).apply {marginStart=dp(6)})
                tag="palette_$id";isSelected=selected
                clickable(this,13,labels[i].local()+if(selected)w(", выбрано",", таңдалған") else ""){if(!selected){prefs.edit().putString("palette_${interfaceStyle.id}",id).apply();recreate()}}
            };colors.addView(cell,LinearLayout.LayoutParams(0,-2,1f).apply {setMargins(dp(3),0,dp(3),0)})
        };body.add(colors);body.add(space(9));body.add(text(w("Меняется акцент. Фон и вид интерфейса сохраняются.","Акцент қана өзгереді. Фон мен интерфейс түрі сақталады."),12f,palette.muted))
        body.add(space(15))
        val icon=horizontal().apply {
            pad(this,12,10);background=shape(palette.surface,dp(15).toFloat())
            val id=LauncherIcons.selected(this@MainActivity)
            add(ImageView(this@MainActivity).apply {setImageResource(launcherArt(id))},42,42)
            val copy=vertical().apply {add(text(w("Значок приложения","Қолданба белгішесі"),16f,bold=true));add(space(4));add(text(InterfaceStyle.from(id).title.local(),12f,palette.muted))}
            addView(copy,LinearLayout.LayoutParams(0,-2,1f).apply {marginStart=dp(12)})
            add(SymbolView(this@MainActivity,"next",palette.muted),22,22);tag="launcher_picker"
            clickable(this,15,w("Выбрать значок приложения","Қолданба белгішесін таңдау")){showLauncherPicker()}
        };body.add(icon)
        sectionTitle(body,"Язык и ввод","Тіл және енгізу")
        val langs=horizontal().apply {background=shape(palette.surface,dp(14).toFloat());pad(this,3,3)}
        listOf("ru" to "Русский","kk" to "Қазақша").forEach {(code,label)->langs.addView(action(label,{changeLanguage(code)},if(language==code)palette.accent else Color.TRANSPARENT,if(language==code)onAccent else palette.ink).apply {isSelected=language==code},LinearLayout.LayoutParams(0,-2,1f))};body.add(langs);body.add(space(12))
        body.add(androidx.appcompat.widget.SwitchCompat(this).apply {
            text=w("Встроенная клавиатура","Кірістірілген пернетақта");setTextColor(palette.ink);textSize=15f;minimumHeight=dp(54);isChecked=customPad
            thumbTintList=ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked),intArrayOf()),intArrayOf(palette.accent,palette.muted))
            trackTintList=ColorStateList.valueOf(palette.line)
            setOnCheckedChangeListener {_,checked->prefs.edit().putBoolean("custom_pad",checked).apply()}
        })
        body.add(text(w("Группировка тысяч включена: 1 000 000,50","Мыңдықтар топтастырылады: 1 000 000,50"),12f,palette.muted))
        sectionTitle(body,"О проекте","Жоба туралы")
        body.add(action(w("Есеп · 2.5    О приложении и авторе","Есеп · 2.5    Қолданба және автор"),{showAbout()},palette.surface,palette.ink).apply {tag="about_project"})
        body.add(space(24));scroll.addView(body);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
    }
    private fun launcherArt(id:String)=when(id){"orbit"->R.drawable.ic_esep_orbit;"prism"->R.drawable.ic_esep_prism;else->R.drawable.ic_esep}
    private fun simpleSheet(title:String,build:(LinearLayout,BottomSheetDialog)->Unit){
        val dialog=BottomSheetDialog(this);val body=vertical().apply {pad(this,20,16);background=shape(palette.background,dp(24).toFloat())}
        val heading=horizontal();heading.addView(text(title,21f,bold=true),LinearLayout.LayoutParams(0,-2,1f))
        heading.add(iconAction("close",w("Закрыть","Жабу"),palette.ink){dialog.dismiss()},48,48)
        body.add(heading);body.add(space(14));build(body,dialog);body.add(space(24))
        dialog.setContentView(ScrollView(this).apply {addView(body)})
        dialog.setOnShowListener {val parent=body.parent.parent as View;parent.setBackgroundColor(Color.TRANSPARENT);BottomSheetBehavior.from(parent).apply {state=BottomSheetBehavior.STATE_EXPANDED;skipCollapsed=true}}
        dialog.show()
    }
    private fun showLauncherPicker()=simpleSheet(w("Значок приложения","Қолданба белгішесі")){body,dialog->
        body.add(text(w("Можно выбрать независимо от оформления.","Безендіруден бөлек таңдауға болады."),14f,palette.muted));body.add(space(18))
        val icons=horizontal();val current=LauncherIcons.selected(this)
        LauncherIcons.ids.forEach {id->
            val cell=vertical().apply {
                gravity=Gravity.CENTER;pad(this,8,14);background=shape(palette.surface,dp(16).toFloat(),if(current==id)palette.accent else palette.line)
                add(ImageView(this@MainActivity).apply {setImageResource(launcherArt(id))},66,66);add(space(12));add(text(InterfaceStyle.from(id).title.local(),14f,bold=true))
                tag="launcher_$id";isSelected=current==id
                clickable(this,16,w("Значок ","Белгіше ")+InterfaceStyle.from(id).title.local()){
                    runCatching {LauncherIcons.select(this@MainActivity,id)}.onSuccess {dialog.dismiss();render();Toast.makeText(this@MainActivity,w("Значок обновлён","Белгіше жаңартылды"),Toast.LENGTH_SHORT).show()}.onFailure {Toast.makeText(this@MainActivity,w("Не удалось сменить значок","Белгішені өзгерту мүмкін болмады"),Toast.LENGTH_SHORT).show()}
                }
            };icons.addView(cell,LinearLayout.LayoutParams(0,-2,1f).apply {setMargins(dp(3),0,dp(3),0)})
        };body.add(icons)
    }
    private fun showWelcome()=simpleSheet(w("Добро пожаловать","Қош келдіңіз")){body,dialog->
        dialog.setOnDismissListener { prefs.edit().putBoolean("welcome_seen",true).apply() }
        body.add(ImageView(this).apply {setImageResource(launcherArt(interfaceStyle.id));scaleType=ImageView.ScaleType.FIT_CENTER},-1,76)
        body.add(space(16));body.add(text("Есеп",32f,bold=true).apply {gravity=Gravity.CENTER})
        body.add(space(12));body.add(text(w("Большие планы начинаются с простого расчёта.","Үлкен жоспар қарапайым есептен басталады."),22f,bold=true).apply {gravity=Gravity.CENTER})
        body.add(space(18));body.add(text(w("Я создал Есеп, чтобы ежедневные расчёты были проще. Если приложение пригодится, поддержать мои новые проекты можно подпиской.","Күнделікті есептеуді жеңілдету үшін Есепті жасадым. Қолданба пайдалы болса, Instagram-ға жазылып, жаңа жобаларымды қолдай аласыз."),15f,palette.muted).apply {gravity=Gravity.CENTER})
        body.add(space(22));body.add(action(w("Открыть Instagram","Instagram-ды ашу"),{dialog.dismiss();openProfile("https://www.instagram.com/solomon_19__/")},palette.accent,onAccent).apply {tag="welcome_instagram"})
        body.add(space(8));body.add(action(w("Продолжить","Жалғастыру"),{dialog.dismiss()},palette.surface,palette.ink).apply {tag="welcome_continue"})
        body.add(space(16));body.add(text(w("Подписка — по желанию. Все калькуляторы доступны бесплатно.","Жазылу — өз еркіңізде. Барлық есептегіш тегін қолжетімді."),13f,palette.muted).apply {gravity=Gravity.CENTER})
    }
    private fun showAbout()=simpleSheet(w("О приложении","Қолданба туралы")){body,_->
        body.add(text("Есеп",30f,bold=true));body.add(space(6));body.add(text(w("Версия 2.5 · Восемь финансовых калькуляторов","2.5 нұсқасы · Сегіз қаржы калькуляторы"),14f,palette.muted));body.add(space(18))
        body.add(text(w("Расчёты выполняются на устройстве. Без интернета и аккаунта. История введённых сумм не записывается.","Есептеулер құрылғыда орындалады. Интернет пен тіркелгі қажет емес. Енгізілген сомалар тарихы жазылмайды."),15f))
        sectionTitle(body,"Автор проекта","Жоба авторы")
        body.add(text(w("Мои проекты и новые работы","Менің жобаларым және жаңа жұмыстарым"),14f,palette.muted));body.add(space(12))
        body.add(action("Instagram · @solomon_19__",{openProfile("https://www.instagram.com/solomon_19__/")},palette.surface,palette.ink).apply {tag="author_instagram"});body.add(space(8))
        body.add(action("TikTok · @suleyman_0805",{openProfile("https://www.tiktok.com/@suleyman_0805")},palette.surface,palette.ink).apply {tag="author_tiktok"});body.add(space(20))
        body.add(text(w("Линейные иконки: Lucide · ISC. Лицензия включена в приложение.","Сызықтық белгішелер: Lucide · ISC. Лицензия қолданбаға қосылған."),12f,palette.muted))
    }
    private fun openProfile(url:String){
        runCatching {startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(url)))}.onFailure {Toast.makeText(this,w("Не найден браузер для открытия ссылки","Сілтемені ашатын браузер табылмады"),Toast.LENGTH_SHORT).show()}
    }
}
