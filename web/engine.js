export const W=(ru,kk)=>[ru,kk];
export const tools=[
 ['budget',W('Бюджет','Бюджет'),W('Доходы и расходы','Кірістер мен шығындар')],
 ['savings',W('Накопления','Жинақ'),W('Будущая сумма','Болашақтағы сома')],
 ['interest',W('Проценты','Пайыздар'),W('Простые и сложные','Жай және күрделі')],
 ['loan',W('Кредит','Несие'),W('Платёж и переплата','Төлем және артық төлем')],
 ['debt',W('Долговая нагрузка','Қарыз жүктемесі'),W('Доля платежей в доходе','Төлемдердің кірістегі үлесі')],
 ['investment',W('Инвестиции','Инвестициялар'),W('Доходность вложений','Салым табыстылығы')],
 ['inflation',W('Инфляция','Инфляция'),W('Разница ставок','Мөлшерлемелер айырмасы')],
 ['goal',W('Финансовая цель','Қаржылық мақсат'),W('Сколько нужно сегодня','Бүгін қанша қажет')]
];
export const modes=id=>id==='savings'?[W('Разовый взнос','Бір реттік салым'),W('Ежегодные взносы','Жыл сайынғы салым')]:id==='interest'?[W('Простые','Жай'),W('Сложные','Күрделі')]:[];
const f=(ru,kk,example,min=0,max=1e15,positive=false,integer=false)=>({label:W(ru,kk),example,min,max,positive,integer});
const rate=(ru,kk,v,signed=false)=>f(ru,kk,v,signed?-1000:0,1000);
const years=()=>f('Срок, лет','Мерзім, жыл','2',0,1000,true);
export function fields(id,m=0){switch(id){
case 'budget':return [f('Доход за месяц, ₸','Айлық кіріс, ₸','300000'),f('Расходы за месяц, ₸','Айлық шығындар, ₸','210000')];
case 'savings':return [f(m?'Взнос в конце года, ₸':'Начальная сумма, ₸',m?'Жыл соңындағы салым, ₸':'Бастапқы сома, ₸','100000'),rate('Ставка, % в год','Мөлшерлеме, жылдық %','10'),years()];
case 'interest':return [f('Начальная сумма, ₸','Бастапқы сома, ₸','100000'),rate('Ставка, % в год','Мөлшерлеме, жылдық %','10'),years(),...(m?[f('Начислений в год','Жылдық есептеу саны','12',0,365,true,true)]:[])];
case 'loan':return [f('Сумма кредита, ₸','Несие сомасы, ₸','1000000',0,1e15,true),rate('Ставка, % в месяц','Мөлшерлеме, айлық %','1,5'),f('Срок, месяцев','Мерзім, ай','24',0,1200,true,true)];
case 'debt':return [f('Платежи за месяц, ₸','Айлық төлемдер, ₸','60000'),f('Доход за месяц, ₸','Айлық кіріс, ₸','300000',0,1e15,true)];
case 'investment':return [f('Вложено, ₸','Салынған сома, ₸','100000',0,1e15,true),f('Получено всего, ₸','Барлық алынған сома, ₸','120000')];
case 'inflation':return [rate('Номинальная ставка, %','Номиналды мөлшерлеме, %','12',true),rate('Инфляция, %','Инфляция, %','8',true)];
case 'goal':return [f('Сумма цели, ₸','Мақсат сомасы, ₸','1000000',0,1e15,true),rate('Доходность, % в год','Табыстылық, жылдық %','10'),years()];
default:throw Error('tool');}}
export const parse=s=>{const t=String(s).trim().replace(/[ \u00a0\u202f]/g,'').replace(',','.');return /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)$/.test(t)&&Number.isFinite(Number(t))?Number(t):null;};
export class InputProblem extends Error{constructor(index,reason){super(reason);this.index=index;this.reason=reason;}}
export function calculate(id,m,raw){
 const specs=fields(id,m),v=specs.map((s,i)=>{if(!String(raw[i]??'').trim())throw new InputProblem(i,'required');const n=parse(raw[i]);if(n===null)throw new InputProblem(i,'number');if(n<s.min||n>s.max||s.positive&&n<=0)throw new InputProblem(i,'range');if(s.integer&&!Number.isInteger(n))throw new InputProblem(i,'integer');return n;});
 const P=W('Вложено','Салым'),I=W('Проценты','Пайыздар');
 const out=(label,value,details=[],graph=[],labels=[],note=null,unit=W('₸','₸'))=>({label,value,details,graph,labels,note,unit,v});let r;
 switch(id){
 case 'budget':{const b=v[0]-v[1];r=out(W('Остаток бюджета','Бюджет қалдығы'),b,[[W('50% от остатка','Қалдықтың 50%-ы'),b*.5],[W('30% от остатка','Қалдықтың 30%-ы'),b*.3],[W('20% от остатка','Қалдықтың 20%-ы'),b*.2]],v,[W('Доход','Кіріс'),W('Расходы','Шығындар')],W('Схема 50/30/20 рассчитана от остатка — как в исходной версии.','50/30/20 үлестері бастапқы нұсқадағыдай қалдықтан есептеледі.'));break;}
 case 'savings':{const rate=v[1]/100,paid=m?v[0]*v[2]:v[0],total=m?(rate===0?paid:v[0]*Math.expm1(v[2]*Math.log1p(rate))/rate):v[0]*Math.pow(1+rate,v[2]);r=out(W('Итоговая сумма','Қорытынды сома'),total,[[P,paid],[I,total-paid]],[paid,total-paid],[P,I],m?W('Одинаковые взносы в конце каждого года. Капитализация раз в год.','Бірдей салым әр жылдың соңында түседі. Капиталдандыру жыл сайын.'):W('Капитализация раз в год. Ставка в примере не является прогнозом.','Жыл сайын капиталданады. Мысалдағы мөлшерлеме болжам емес.'));break;}
 case 'interest':{const total=m?v[0]*Math.pow(1+v[1]/100/v[3],v[3]*v[2]):v[0]*(1+v[1]/100*v[2]);r=out(m?W('Итоговая сумма','Қорытынды сома'):W('Сумма процентов','Пайыз сомасы'),m?total:total-v[0],[[P,v[0]],[m?I:W('Итого с процентами','Пайызбен бірге'),m?total-v[0]:total]],[v[0],total-v[0]],[P,I]);break;}
 case 'loan':{const rate=v[1]/100,pay=rate===0?v[0]/v[2]:v[0]*rate/-Math.expm1(-v[2]*Math.log1p(rate)),extra=Math.max(0,pay*v[2]-v[0]);r=out(W('Ежемесячный платёж','Ай сайынғы төлем'),pay,[[W('Переплата','Артық төлем'),extra],[W('Всего к возврату','Барлық қайтарылатын сома'),pay*v[2]]],[v[0],extra],[W('Основной долг','Негізгі қарыз'),W('Переплата','Артық төлем')],W('Используется месячная ставка. Комиссии и страховка не включены.','Айлық мөлшерлеме қолданылады. Комиссия мен сақтандыру есепке алынбайды.'),W('₸/мес','₸/ай'));break;}
 case 'debt':r=out(W('Доля платежей в доходе','Төлемдердің кірістегі үлесі'),v[0]/v[1]*100,[[W('Платежи','Төлемдер'),v[0]],[W('Доход','Кіріс'),v[1]]],[v[0],Math.max(0,v[1]-v[0])],[W('Платежи','Төлемдер'),W('Остаток дохода','Кіріс қалдығы')],W('Показатель может превышать 100%. Это расчёт доли, а не решение банка.','Көрсеткіш 100%-дан асуы мүмкін. Бұл — үлес есебі, банктің шешімі емес.'),W('%','%'));break;
 case 'investment':r=out(W('Доходность вложений','Салым табыстылығы'),(v[1]-v[0])/v[0]*100,[[W('Прибыль / убыток','Пайда / шығын'),v[1]-v[0]],[W('Получено всего','Барлық алынған сома'),v[1]]],v,[P,W('Получено','Алынған')],null,W('%','%'));break;
 case 'inflation':r=out(W('Разница ставок','Мөлшерлемелер айырмасы'),v[0]-v[1],[],v,[W('Номинальная ставка','Номиналды мөлшерлеме'),W('Инфляция','Инфляция')],W('Упрощённая оценка: номинальная ставка минус инфляция. Не точная реальная доходность.','Жеңілдетілген баға: номиналды мөлшерлемеден инфляция шегеріледі. Бұл нақты табыстылық емес.'),W('п.п.','п.т.'));break;
 case 'goal':r=out(W('Нужно вложить сегодня','Бүгін салу қажет'),v[0]/Math.pow(1+v[1]/100,v[2]),[[W('Цель в будущем','Болашақ мақсат'),v[0]]],[],[],W('Разовый вклад сегодня, а не ежемесячный взнос. Капитализация раз в год; ставка — допущение расчёта.','Бұл айлық салым емес, бүгінгі бір реттік салым. Капиталдандыру жыл сайын; мөлшерлеме — есеп шарты.'));break;
 }
 if(!Number.isFinite(r.value)||r.value>1e18||r.details.some(x=>!Number.isFinite(x[1])))throw new InputProblem(0,'overflow');return r;
}
export const formula=(id,m)=>({budget:['B = D − E','50% = B × 0,5','30% = B × 0,3','20% = B × 0,2'],savings:m?['S = P × ((1 + r)ᵗ − 1) / r','r = 0: S = P × t']:['FV = P × (1 + r)ᵗ'],interest:[m?'A = P × (1 + r / n)ⁿᵗ':'I = P × r × t'],loan:['A = P × r / (1 − (1 + r)⁻ⁿ)','r = 0: A = P / n'],debt:['DTI = M / D × 100%'],investment:['ROI = (V − P) / P × 100%'],inflation:['Δ = R − i'],goal:['PV = F / (1 + r)ᵗ']})[id];
export function group(value){const raw=String(value).replace(/[ \u00a0\u202f]/g,'');if(!/^[+-]?\d*(?:[.,]\d*)?$/.test(raw))return String(value);const [integer,...fraction]=raw.split(/([.,])/);return integer.replace(/\B(?=(\d{3})+(?!\d))/g,' ')+fraction.join('');}

export function rateFraction(raw){const text=String(raw).trim().replace(/[ \u00a0\u202f]/g,'').replace(',','.');const m=text.match(/^([+-]?)(\d*)(?:\.(\d*))?$/);if(!m||!(m[2]||m[3]))return null;const fraction=m[3]||'',digits=((m[2]||'0')+fraction).replace(/^0+/,'')||'0',places=fraction.length+2,padded=digits.padStart(places+1,'0');const value=(padded.slice(0,-places)+'.'+padded.slice(-places)).replace(/0+$/,'').replace(/\.$/,'');return (m[1]==='-'&&digits!=='0'?'-':'')+value.replace('.',',');}
