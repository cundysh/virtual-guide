package com.cundysh.virtualguide
import org.junit.Assert.*
import org.junit.Test
class GuideMathTest {
 @Test fun sameLocation(){assertEquals(0.0,GuideMath.distance(41.3,19.8,41.3,19.8),0.001)}
 @Test fun oneDegreeAtEquator(){assertEquals(111195.0,GuideMath.distance(0.0,0.0,0.0,1.0),2.0)}
 @Test fun wrapDateline(){assertEquals(22239.0,GuideMath.distance(0.0,179.9,0.0,-179.9),2.0)}
 @Test fun narrationChunks(){val original=(1..2000).joinToString(" "){"story"};val chunks=GuideMath.chunks(original);assertTrue(chunks.all{it.length<=3000});assertEquals(original,chunks.joinToString(" "))}
 @Test fun emptyNarration(){assertTrue(GuideMath.chunks("  ").isEmpty())}
}
