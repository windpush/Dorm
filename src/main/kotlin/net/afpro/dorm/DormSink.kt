package net.afpro.dorm

import net.afpro.dorm.annotation.Dorm
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

object DormSink {
    inline fun <reified T : Any> sink(input: InputSource?): T {
        return sink(input, T::class.java)
    }

    @JvmStatic
    @Throws(DormParseException::class)
    fun <T : Any> sink(input: InputSource?, outputType: Class<T>?): T {
        if (input == null) {
            throw DormParseException(NullPointerException("input is null"))
        }

        if (outputType == null) {
            throw DormParseException(NullPointerException("output type is null"))
        }

        val output = wrap(
            { "create $outputType failed" },
            { outputType.newInstance() })
        sinkInto(input, output)
        return output
    }

    @JvmStatic
    @Throws(DormParseException::class)
    fun sinkInto(input: InputSource?, output: Any?) {
        if (input == null) {
            throw DormParseException(NullPointerException("input is null"))
        }

        if (output == null) {
            throw DormParseException(NullPointerException("output is null"))
        }

        val doc = wrap(
            { "parse xml document failed" },
            { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input) })

        val xpath = wrap(
            { "create xpath failed" },
            { XPathFactory.newInstance().newXPath() })

        parse(doc, xpath, output)
    }

    private fun parse(node: Node, xpath: XPath, into: Any) {
        var t: Class<*>? = into.javaClass
        while (t != null) {
            t.declaredFields.forEach { field ->
                val dorm = field.getAnnotation(Dorm::class.java) ?: return@forEach
                parse(node, xpath, into, field, dorm)
            }
            t.declaredMethods.forEach { method ->
                val dorm = method.getAnnotation(Dorm::class.java) ?: return@forEach
                parse(node, xpath, into, method, dorm)
            }
            t = t.superclass
        }
    }

    private fun parse(node: Node, xpath: XPath, into: Any, field: Field, dorm: Dorm) {
        if (!field.isAccessible) {
            wrap(
                { "field $field is inaccessible" },
                { field.isAccessible = true })
        }

        val cur = wrap(
            { "get field $field on $into failed" },
            { field.get(into) })

        val parsed = parse(node, xpath, cur, dorm, field.type)
        if (cur !== parsed) {
            if (Modifier.isFinal(field.modifiers)) {
                throw DormParseException("$field is final, try use 'append' attribute of Dorm?")
            }

            wrap(
                { "set field $field on $into failed" },
                { field.set(into, parsed) }
            )
        }
    }

    private fun parse(node: Node, xpath: XPath, into: Any, method: Method, dorm: Dorm) {
        if (!method.isAccessible) {
            wrap(
                { "method $method is inaccessible" },
                { method.isAccessible = true })
        }

        val params = method.parameterTypes
        when (params.size) {
            0 -> {
                wrap(
                    { "invoke method $method on $into failed" },
                    { method.invoke(into) })
            }
            1 -> {
                val desiredType = method.parameterTypes[0]
                val parsed = parse(node, xpath, null, dorm, desiredType)
                wrap(
                    { "invoke method $method on $into failed" },
                    { method.invoke(into, parsed) })
            }
            2 -> {
                throw DormParseException("method $method has too many params")
            }
        }
    }

    private fun parse(node: Node, xpath: XPath, value: Any?, dorm: Dorm, desiredType: Class<*>): Any? {
        val checkResult = TypeUtils.checkType(desiredType)
        return when {
            (checkResult and TypeUtils.TYPE_PRIMITIVE) != 0 -> {
                if (dorm.append) {
                    throw DormParseException("can't append to primitive types")
                }
                val text = xpath.evaluate(dorm.value, node).trim()
                wrap(
                    { "parse '$text' to $desiredType failed" },
                    { TypeUtils.parse(desiredType, text) })
            }
            checkResult == TypeUtils.TYPE_STRING -> {
                if (dorm.append) {
                    throw DormParseException("can't append to string")
                }
                return xpath.evaluate(dorm.value, node).let {
                    if (dorm.trim) {
                        it.trim()
                    } else {
                        it
                    }
                }
            }
            checkResult == TypeUtils.TYPE_ARRAY -> {
                (xpath.evaluate(dorm.value, node, XPathConstants.NODESET) as? NodeList)
                    ?.let { nodeList ->
                        val itemType = desiredType.componentType
                        val array = Array.newInstance(itemType, nodeList.length)
                        (0 until nodeList.length)
                            .asSequence()
                            .map { nodeList.item(it) }
                            .forEachIndexed { index, itemNode ->
                                val item = parseArrayItem(itemNode, xpath, dorm, itemType)
                                wrap(
                                    { "set array item failed" },
                                    { Array.set(array, index, item) })
                            }
                        array
                    }
            }
            checkResult == TypeUtils.TYPE_DORM_TARGET -> {
                if (value == null || dorm.append) {
                    wrap(
                        { "create $desiredType failed" },
                        { desiredType.newInstance() })
                } else {
                    value
                }.also { targetObj ->
                    (xpath.evaluate(dorm.value, node, XPathConstants.NODE) as? Node)
                        ?.also { targetNode ->
                            parse(targetNode, xpath, targetObj)
                        }
                }
            }
            else -> {
                throw DormParseException("unsupported type $desiredType")
            }
        }
    }

    private fun parseArrayItem(node: Node, xpath: XPath, dorm: Dorm, desiredType: Class<*>): Any? {
        val checkResult = TypeUtils.checkType(desiredType)
        return when {
            (checkResult and TypeUtils.TYPE_PRIMITIVE) != 0 -> {
                val text = xpath.evaluate("./text()", node).trim()
                wrap(
                    { "parse '$text' to $desiredType failed" },
                    { TypeUtils.parse(desiredType, text) })
            }
            checkResult == TypeUtils.TYPE_STRING -> {
                return xpath.evaluate("./text()", node).let {
                    if (dorm.trim) {
                        it.trim()
                    } else {
                        it
                    }
                }
            }
            checkResult == TypeUtils.TYPE_ARRAY -> {
                throw DormParseException("array of array unsupported")
            }
            checkResult == TypeUtils.TYPE_DORM_TARGET -> {
                wrap(
                    { "create $desiredType failed" },
                    { desiredType.newInstance() })
                    .also { targetObj ->
                        parse(node, xpath, targetObj)
                    }
            }
            else -> {
                throw DormParseException("unsupported type $desiredType")
            }
        }
    }

    private inline fun <R> wrap(msg: () -> String, f: () -> R): R {
        try {
            return f()
        } catch (e: Throwable) {
            throw DormParseException(msg(), e)
        }
    }
}