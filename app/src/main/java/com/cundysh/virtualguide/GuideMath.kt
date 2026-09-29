package com.cundysh.virtualguide
import kotlin.math.*
object GuideMath {
 fun distance(lat:Double,lon:Double,toLat:Double,toLon:Double):Double {
  val a=Math.toRadians(toLat-lat);val b=Math.toRadians(toLon-lon)
  val h=sin(a/2).pow(2)+cos(Math.toRadians(lat))*cos(Math.toRadians(toLat))*sin(b/2).pow(2)
  return 6371000*2*asin(sqrt(h.coerceIn(0.0,1.0)))
 }
 fun chunks(text:String):List<String> {
  val result=mutableListOf<String>();var rest=text.trim()
  while(rest.length>3000){val cut=rest.lastIndexOf(' ',3000).let{if(it>0)it else 3000};result+=rest.take(cut);rest=rest.drop(cut).trim()}
  if(rest.isNotEmpty())result+=rest
  return result
 }
}
