package net.afpro.dorm

import net.afpro.dorm.annotation.Dorm
import net.afpro.dorm.annotation.DormTarget
import org.junit.Assert
import org.junit.Test
import org.xml.sax.InputSource

class SimpleTest {
    @Test
    fun testParse() {
        val inst = content.byteInputStream().use {
            DormSink.sink<Content>(InputSource(it))
        }

        Assert.assertEquals("test", inst.name)
        Assert.assertEquals("sub string", inst.subStr)
        Assert.assertEquals(1, inst.subs.size)
        Assert.assertEquals("sub", inst.subs[0].name)
        Assert.assertEquals("sub string", inst.subs[0].text)
        Assert.assertEquals(10, inst.subs[0].index)
        Assert.assertArrayEquals(
            intArrayOf(10, 20),
            inst.ints)
    }

    @DormTarget
    class Content {
        @Dorm("./root/@name")
        var name: String = ""

        @Dorm("./root/sub/text()")
        var subStr: String = ""

        @Dorm("./root/sub")
        var subs: Array<Sub> = emptyArray()

        @Dorm("./root/int")
        var ints: IntArray = intArrayOf()
    }

    @DormTarget
    class Sub {
        @Dorm("./@name")
        var name: String = ""

        @Dorm("./text()")
        var text: String = ""

        @Dorm("./@index")
        var index: Int = 0
    }

    companion object {
        private val content = """
            <root name="test">
                <sub name="sub" index="10">
                sub string
                </sub>
                <int>
                10
                </int>
                <int>20</int>
            </root>
        """.trimIndent()
    }
}