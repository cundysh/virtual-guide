package com.cundysh.virtualguide

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.location.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity:AppCompatActivity(),LocationListener {
 private val prefs by lazy{getSharedPreferences("guide",MODE_PRIVATE)}
 private val lm by lazy{getSystemService(LOCATION_SERVICE) as LocationManager}
 private val handler=Handler(Looper.getMainLooper())
 private lateinit var root:LinearLayout
 private var status="Choose Find nearby to begin."
 private var nearby=JSONArray()
 private var location:Location?=null
 private var searchCenter:Location?=null
 private var lastSearch=0L
 private var busy=false
 private var generation=0
 private var page="nearby"
 private var active:JSONObject?=null
 private var tts:TextToSpeech?=null
 private var speechReady=false
 private var tracking=false
 private val bg=0xFF0A1822.toInt();private val card=0xFF142B37.toInt();private val accent=0xFF71DEC0.toInt();private val muted=0xFFA9BDC7.toInt()
 private val lang get()=prefs.getString("lang","en")?:"en"
 private val radius get()=prefs.getInt("radius",5000)
 private val timeout=Runnable{if(location==null){status="Waiting for GPS. Check Location is enabled, or try outside.";render()}}
 override fun onCreate(state:Bundle?){
  androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
  super.onCreate(state)
  nearby=jsonArray(prefs.getString("nearby_$lang",null))
  if(nearby.length()>0)status="Last saved nearby results · reconnect to update."
  tts=TextToSpeech(this){speechReady=it==TextToSpeech.SUCCESS}
  render()
 }
 private fun jsonArray(s:String?)=try{JSONArray(s?:"[]")}catch(_:Exception){JSONArray()}
 private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
 private fun round(color:Int)=GradientDrawable().apply{setColor(color);cornerRadius=dp(22).toFloat()}
 private fun column()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
 private fun text(value:String,size:Float=16f,color:Int=0xFFF4F8FA.toInt(),bold:Boolean=false)=TextView(this).apply{this.text=value;textSize=size;setTextColor(color);if(bold)setTypeface(typeface,Typeface.BOLD);setPadding(0,dp(4),0,dp(8))}
 private fun button(label:String,primary:Boolean=false,action:()->Unit)=Button(this).apply{text=label;isAllCaps=false;setTextColor(if(primary)bg else 0xFFF4F8FA.toInt());textSize=16f;minHeight=dp(54);background=round(if(primary)accent else 0xFF203F4D.toInt());setOnClickListener{action()};layoutParams=LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(10);bottomMargin=dp(4)}}
 private fun panel()=column().apply{background=round(card);setPadding(dp(20),dp(18),dp(20),dp(18));layoutParams=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)}}
 private fun render(){
  if(isFinishing||isDestroyed)return
  root=column().apply{setPadding(dp(20),dp(24),dp(20),dp(28));setBackgroundColor(bg)}
  setContentView(ScrollView(this).apply{setBackgroundColor(bg);isFillViewport=true;addView(root)})
  if(page=="story"&&active!=null){renderStory(active!!);return}
  root.addView(text("VIRTUAL GUIDE",12f,accent,true));root.addView(text("Every place\nhas a story.",34f,bold=true))
  root.addView(text("Discover what's around you. Listen at your own pace.",16f,muted))
  val tabs=LinearLayout(this)
  listOf("nearby" to "◎ Nearby","saved" to "♡ Saved").forEach{(key,label)->tabs.addView(button(label,page==key){page=key;render()},LinearLayout.LayoutParams(0,dp(56),1f).apply{setMargins(dp(2),0,dp(2),dp(16))})};root.addView(tabs)
  if(page=="nearby"){
   val control=panel();control.addView(text(status,14f,muted))
   control.addView(button(if(busy)"Finding stories…" else "Find nearby",true){findLocation()}.apply{isEnabled=!busy})
   control.addView(text("While this screen is open, refreshes after you move about 500 m. Location is sent to Wikipedia only to find places nearby.",12f,muted))
   val filters=LinearLayout(this)
   filters.addView(button("${radius/1000} km"){android.app.AlertDialog.Builder(this).setTitle("Search radius").setItems(arrayOf("2 km","5 km","10 km")){_,which->prefs.edit().putInt("radius",listOf(2000,5000,10000)[which]).apply();location?.let{search(it)}?:render()}.show()},LinearLayout.LayoutParams(0,-2,1f))
   filters.addView(button(if(lang=="en")"English" else "Shqip"){android.app.AlertDialog.Builder(this).setTitle("Story language").setItems(arrayOf("English","Shqip")){_,which->generation++;busy=false;prefs.edit().putString("lang",if(which==0)"en" else "sq").apply();nearby=jsonArray(prefs.getString("nearby_$lang",null));location?.let{search(it)}?:render()}.show()},LinearLayout.LayoutParams(0,-2,1f))
   control.addView(filters);root.addView(control)
  }
  val items=if(page=="saved")jsonArray(prefs.getString("saved",null))else nearby
  root.addView(text(if(page=="saved")"Your saved stories" else "Nearby stories",23f,bold=true))
  if(items.length()==0)root.addView(panel().apply{addView(text(if(page=="saved")"Save a story to read or listen offline." else "No stories here yet. Try Find nearby or a wider radius.",16f,muted))})
  for(i in 0 until items.length()){
   val item=items.getJSONObject(i);val c=panel()
   c.addView(text(item.optString("title"),23f,bold=true))
   val meters=location?.let{GuideMath.distance(it.latitude,it.longitude,item.optDouble("lat"),item.optDouble("lon"))}
   c.addView(text(if(meters!=null)String.format(Locale.getDefault(),"%.1f km away · straight-line distance",meters/1000)else "Saved location · distance unavailable",13f,accent))
   c.addView(button("Read / listen →",true){openStory(item)})
   root.addView(c)
  }
  root.addView(text("Stories: Wikipedia contributors · CC BY-SA\nCoverage varies by location and language. Map directions open in your maps app.",12f,muted))
 }
 private fun findLocation(){
  if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),10);return}
  status="Getting your location…";render();tracking=true
  try{
   val providers=listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER).filter{lm.isProviderEnabled(it)}
   if(providers.isEmpty()){status="Turn on phone Location, then tap Find nearby.";render();return}
   for(p in providers)lm.requestLocationUpdates(p,10000L,50f,this)
   val recent=providers.mapNotNull{lm.getLastKnownLocation(it)}.filter{System.currentTimeMillis()-it.time<120000}.minByOrNull{it.accuracy}
   if(recent!=null){location=recent;search(recent)}
   handler.removeCallbacks(timeout);handler.postDelayed(timeout,20000)
  }catch(_:SecurityException){status="Location permission is needed to find nearby places.";render()}
 }
 override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,results:IntArray){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==10){if(results.any{it==PackageManager.PERMISSION_GRANTED})findLocation()else{status="Location wasn't allowed. Saved stories still work.";render()}}}
 override fun onLocationChanged(fix:Location){
  location=fix;handler.removeCallbacks(timeout)
  if(page=="nearby"&&!busy&&(searchCenter==null||(fix.distanceTo(searchCenter!!)>500&&System.currentTimeMillis()-lastSearch>60000)))search(fix)
 }
 private fun request(language:String,query:String):JSONObject{
  val connection=URL("https://$language.wikipedia.org/w/api.php?format=json&formatversion=2&$query").openConnection() as HttpURLConnection
  connection.setRequestProperty("User-Agent","VirtualGuideTest/0.1 (Android location guide; user-initiated)")
  connection.connectTimeout=10000;connection.readTimeout=15000
  try{if(connection.responseCode!=200)throw IllegalStateException("Service returned ${connection.responseCode}");val data=connection.inputStream.bufferedReader().use{JSONObject(it.readText())};if(data.has("error"))throw IllegalStateException(data.getJSONObject("error").optString("info","Wikipedia error"));return data}finally{connection.disconnect()}
 }
 private fun search(fix:Location){
  val language=lang;val range=radius;val token=++generation;busy=true;status="Finding nearby stories…";render();lastSearch=System.currentTimeMillis()
  thread{
   try{
    val coord=Uri.encode(String.format(Locale.US,"%.4f|%.4f",fix.latitude,fix.longitude))
    val result=request(language,"action=query&list=geosearch&gscoord=$coord&gsradius=$range&gslimit=20&gsnamespace=0").getJSONObject("query").getJSONArray("geosearch")
    for(i in 0 until result.length())result.getJSONObject(i).put("lang",language)
    runOnUiThread{if(token==generation&&!isDestroyed){nearby=result;searchCenter=Location(fix);busy=false;status="${result.length()} places within ${range/1000} km · ${if(language=="en")"English" else "Shqip"}";prefs.edit().putString("nearby_$language",result.toString()).apply();render()}}
   }catch(_:Exception){runOnUiThread{if(token==generation&&!isDestroyed){busy=false;status="Couldn't update nearby places. Check internet and try again. Saved results remain available.";render()}}}
  }
 }
 private fun cacheKey(p:JSONObject)="story_${p.optString("lang","en")}_${p.getLong("pageid") }"
 private fun openStory(item:JSONObject){
  tts?.stop();val cached=prefs.getString(cacheKey(item),null)
  active=if(cached!=null)JSONObject(cached)else JSONObject(item.toString());page="story";render()
  if(active!!.optString("extract").isNotBlank())return
  val id=item.getLong("pageid");val language=item.optString("lang","en")
  thread{
   try{
    val response=request(language,"action=query&prop=extracts&exintro=1&explaintext=1&pageids=$id")
    val extract=response.getJSONObject("query").getJSONArray("pages").getJSONObject(0).optString("extract")
    val full=JSONObject(item.toString()).put("extract",extract).put("saved_at",System.currentTimeMillis())
    if(extract.isNotBlank())prefs.edit().putString(cacheKey(item),full.toString()).apply()
    runOnUiThread{if(page=="story"&&active?.getLong("pageid")==id&&active?.optString("lang")==language){active=full;if(extract.isBlank())active!!.put("load_error","No introduction is available for this place. Open the source article.");render()}}
   }catch(_:Exception){runOnUiThread{if(page=="story"&&active?.getLong("pageid")==id){active!!.put("load_error","Story couldn't load. Reopen it when online.");render()}}}
  }
 }
 private fun renderStory(p:JSONObject){
  root.addView(button("← Nearby stories"){tts?.stop();page="nearby";active=null;render()})
  root.addView(text("DISCOVER & LISTEN",12f,accent,true));root.addView(text(p.getString("title"),32f,bold=true))
  val extract=p.optString("extract")
  root.addView(text(extract.ifBlank{p.optString("load_error","Loading the story…")},18f))
  if(extract.isNotBlank()){
   root.addView(button("▶ Listen to this story",true){
    if(!speechReady){toast("Voice engine is not ready on this phone.");return@button}
    val result=tts!!.setLanguage(Locale.forLanguageTag(p.optString("lang","en")))
    if(result==TextToSpeech.LANG_MISSING_DATA||result==TextToSpeech.LANG_NOT_SUPPORTED){toast("Install a voice for this language in Android text-to-speech settings.");return@button}
    tts!!.stop();GuideMath.chunks(p.getString("title")+". "+extract).forEachIndexed{i,part->tts!!.speak(part,if(i==0)TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD,null,"guide-$i")}
   })
   root.addView(button("■ Stop narration"){tts?.stop()})
   val saved=jsonArray(prefs.getString("saved",null));val exists=(0 until saved.length()).any{cacheKey(saved.getJSONObject(it))==cacheKey(p)}
   root.addView(button(if(exists)"♥ Saved for offline" else "♡ Save for offline"){
    if(!exists){saved.put(p);prefs.edit().putString("saved",saved.toString()).apply();render()}else toast("Already saved on this phone.")
   })
  }
  root.addView(button("↗ Open map directions"){openUrl("geo:${p.getDouble("lat")},${p.getDouble("lon")}?q=${p.getDouble("lat")},${p.getDouble("lon")}(${Uri.encode(p.getString("title"))})")})
  val source="https://${p.optString("lang","en")}.wikipedia.org/?curid=${p.getLong("pageid") }"
  root.addView(button("Wikipedia source & contributors"){openUrl(source)})
  root.addView(button("CC BY-SA license"){openUrl("https://creativecommons.org/licenses/by-sa/4.0/")})
  root.addView(text("Introduction from Wikipedia contributors, CC BY-SA. The voice uses your phone's installed text-to-speech engine. Offline narration requires an installed offline voice.",12f,muted))
 }
 private fun openUrl(url:String){try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)))}catch(_:Exception){toast("No app is available to open this link.")}}
 private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
 override fun onPause(){super.onPause();lm.removeUpdates(this);handler.removeCallbacks(timeout);tts?.stop()}
 override fun onResume(){super.onResume();if(tracking&&page=="nearby")findLocation()}
 override fun onDestroy(){generation++;tts?.shutdown();handler.removeCallbacksAndMessages(null);super.onDestroy()}
 @Deprecated("Deprecated in Java") override fun onBackPressed(){if(page=="story"){tts?.stop();page="nearby";active=null;render()}else super.onBackPressed()}
 override fun onProviderEnabled(provider:String){}
 override fun onProviderDisabled(provider:String){}
 @Deprecated("Deprecated in Java") override fun onStatusChanged(provider:String?,status:Int,extras:Bundle?){}
}
