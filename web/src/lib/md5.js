/**
 * 纯JavaScript MD5实现（无外部依赖）
 * 基于RFC 1321标准实现，适用于Next.js静态导出
 */

/**
 * MD5核心工具类
 */
class MD5 {
    /**
     * 左循环移位
     */
    static rotateLeft(value, amount) {
        return (value << amount) | (value >>> (32 - amount))
    }

    /**
     * 无符号加法
     */
    static addUnsigned(x, y) {
        const x4 = (x & 0x40000000)
        const y4 = (y & 0x40000000)
        const x8 = (x & 0x80000000)
        const y8 = (y & 0x80000000)
        const result = (x & 0x3FFFFFFF) + (y & 0x3FFFFFFF)

        if (x4 & y4) {
            return (result ^ 0x80000000 ^ x8 ^ y8)
        }
        if (x4 | y4) {
            if (result & 0x40000000) {
                return (result ^ 0xC0000000 ^ x8 ^ y8)
            } else {
                return (result ^ 0x40000000 ^ x8 ^ y8)
            }
        } else {
            return (result ^ x8 ^ y8)
        }
    }

    /**
     * MD5辅助函数F
     */
    static md5F(x, y, z) {
        return (x & y) | ((~x) & z)
    }

    /**
     * MD5辅助函数G
     */
    static md5G(x, y, z) {
        return (x & z) | (y & (~z))
    }

    /**
     * MD5辅助函数H
     */
    static md5H(x, y, z) {
        return (x ^ y ^ z)
    }

    /**
     * MD5辅助函数I
     */
    static md5I(x, y, z) {
        return (y ^ (x | (~z)))
    }

    /**
     * MD5第一轮运算
     */
    static md5FF(a, b, c, d, x, s, ac) {
        a = this.addUnsigned(a, this.addUnsigned(this.addUnsigned(this.md5F(b, c, d), x), ac))
        return this.addUnsigned(this.rotateLeft(a, s), b)
    }

    /**
     * MD5第二轮运算
     */
    static md5GG(a, b, c, d, x, s, ac) {
        a = this.addUnsigned(a, this.addUnsigned(this.addUnsigned(this.md5G(b, c, d), x), ac))
        return this.addUnsigned(this.rotateLeft(a, s), b)
    }

    /**
     * MD5第三轮运算
     */
    static md5HH(a, b, c, d, x, s, ac) {
        a = this.addUnsigned(a, this.addUnsigned(this.addUnsigned(this.md5H(b, c, d), x), ac))
        return this.addUnsigned(this.rotateLeft(a, s), b)
    }

    /**
     * MD5第四轮运算
     */
    static md5II(a, b, c, d, x, s, ac) {
        a = this.addUnsigned(a, this.addUnsigned(this.addUnsigned(this.md5I(b, c, d), x), ac))
        return this.addUnsigned(this.rotateLeft(a, s), b)
    }

    /**
     * 将字符串转换为字数组
     */
    static convertToWordArray(str) {
        let wordArray = []
        let messageLength = str.length
        let numberOfWords = (((messageLength + 8) - ((messageLength + 8) % 64)) / 64 + 1) * 16

        for (let i = 0; i < numberOfWords; i++) {
            wordArray[i] = 0
        }

        for (let i = 0; i < messageLength; i++) {
            let bytePosition = (i - (i % 4)) / 4
            let byteOffset = (i % 4) * 8
            wordArray[bytePosition] = (wordArray[bytePosition] | (str.charCodeAt(i) << byteOffset))
        }

        let bytePosition = (messageLength - (messageLength % 4)) / 4
        let byteOffset = (messageLength % 4) * 8
        wordArray[bytePosition] = wordArray[bytePosition] | (0x80 << byteOffset)
        wordArray[numberOfWords - 2] = messageLength << 3
        wordArray[numberOfWords - 1] = messageLength >>> 29

        return wordArray
    }

    /**
     * 将32位整数转换为十六进制字符串
     */
    static wordToHex(value) {
        let result = ""
        for (let i = 0; i <= 3; i++) {
            let byte = (value >>> (i * 8)) & 255
            result += ("0" + byte.toString(16)).substr(-2)
        }
        return result
    }

    /**
     * UTF-8编码
     */
    static utf8Encode(str) {
        return unescape(encodeURIComponent(str))
    }

    /**
     * 计算MD5哈希值
     * @param {string} str - 要计算哈希的字符串
     * @returns {string} MD5哈希值（32位十六进制字符串）
     */
    static hash(str) {
        // 将字符串转换为UTF-8编码
        const message = this.utf8Encode(str)
        const wordArray = this.convertToWordArray(message)

        // MD5初始状态
        let a = 0x67452301
        let b = 0xEFCDAB89
        let c = 0x98BADCFE
        let d = 0x10325476

        // 主循环
        for (let i = 0; i < wordArray.length; i += 16) {
            let aa = a, bb = b, cc = c, dd = d

            // 第一轮
            a = this.md5FF(a, b, c, d, wordArray[i + 0], 7, 0xD76AA478)
            d = this.md5FF(d, a, b, c, wordArray[i + 1], 12, 0xE8C7B756)
            c = this.md5FF(c, d, a, b, wordArray[i + 2], 17, 0x242070DB)
            b = this.md5FF(b, c, d, a, wordArray[i + 3], 22, 0xC1BDCEEE)
            a = this.md5FF(a, b, c, d, wordArray[i + 4], 7, 0xF57C0FAF)
            d = this.md5FF(d, a, b, c, wordArray[i + 5], 12, 0x4787C62A)
            c = this.md5FF(c, d, a, b, wordArray[i + 6], 17, 0xA8304613)
            b = this.md5FF(b, c, d, a, wordArray[i + 7], 22, 0xFD469501)
            a = this.md5FF(a, b, c, d, wordArray[i + 8], 7, 0x698098D8)
            d = this.md5FF(d, a, b, c, wordArray[i + 9], 12, 0x8B44F7AF)
            c = this.md5FF(c, d, a, b, wordArray[i + 10], 17, 0xFFFF5BB1)
            b = this.md5FF(b, c, d, a, wordArray[i + 11], 22, 0x895CD7BE)
            a = this.md5FF(a, b, c, d, wordArray[i + 12], 7, 0x6B901122)
            d = this.md5FF(d, a, b, c, wordArray[i + 13], 12, 0xFD987193)
            c = this.md5FF(c, d, a, b, wordArray[i + 14], 17, 0xA679438E)
            b = this.md5FF(b, c, d, a, wordArray[i + 15], 22, 0x49B40821)

            // 第二轮
            a = this.md5GG(a, b, c, d, wordArray[i + 1], 5, 0xF61E2562)
            d = this.md5GG(d, a, b, c, wordArray[i + 6], 9, 0xC040B340)
            c = this.md5GG(c, d, a, b, wordArray[i + 11], 14, 0x265E5A51)
            b = this.md5GG(b, c, d, a, wordArray[i + 0], 20, 0xE9B6C7AA)
            a = this.md5GG(a, b, c, d, wordArray[i + 5], 5, 0xD62F105D)
            d = this.md5GG(d, a, b, c, wordArray[i + 10], 9, 0x02441453)
            c = this.md5GG(c, d, a, b, wordArray[i + 15], 14, 0xD8A1E681)
            b = this.md5GG(b, c, d, a, wordArray[i + 4], 20, 0xE7D3FBC8)
            a = this.md5GG(a, b, c, d, wordArray[i + 9], 5, 0x21E1CDE6)
            d = this.md5GG(d, a, b, c, wordArray[i + 14], 9, 0xC33707D6)
            c = this.md5GG(c, d, a, b, wordArray[i + 3], 14, 0xF4D50D87)
            b = this.md5GG(b, c, d, a, wordArray[i + 8], 20, 0x455A14ED)
            a = this.md5GG(a, b, c, d, wordArray[i + 13], 5, 0xA9E3E905)
            d = this.md5GG(d, a, b, c, wordArray[i + 2], 9, 0xFCEFA3F8)
            c = this.md5GG(c, d, a, b, wordArray[i + 7], 14, 0x676F02D9)
            b = this.md5GG(b, c, d, a, wordArray[i + 12], 20, 0x8D2A4C8A)

            // 第三轮
            a = this.md5HH(a, b, c, d, wordArray[i + 5], 4, 0xFFFA3942)
            d = this.md5HH(d, a, b, c, wordArray[i + 8], 11, 0x8771F681)
            c = this.md5HH(c, d, a, b, wordArray[i + 11], 16, 0x6D9D6122)
            b = this.md5HH(b, c, d, a, wordArray[i + 14], 23, 0xFDE5380C)
            a = this.md5HH(a, b, c, d, wordArray[i + 1], 4, 0xA4BEEA44)
            d = this.md5HH(d, a, b, c, wordArray[i + 4], 11, 0x4BDECFA9)
            c = this.md5HH(c, d, a, b, wordArray[i + 7], 16, 0xF6BB4B60)
            b = this.md5HH(b, c, d, a, wordArray[i + 10], 23, 0xBEBFBC70)
            a = this.md5HH(a, b, c, d, wordArray[i + 13], 4, 0x289B7EC6)
            d = this.md5HH(d, a, b, c, wordArray[i + 0], 11, 0xEAA127FA)
            c = this.md5HH(c, d, a, b, wordArray[i + 3], 16, 0xD4EF3085)
            b = this.md5HH(b, c, d, a, wordArray[i + 6], 23, 0x04881D05)
            a = this.md5HH(a, b, c, d, wordArray[i + 9], 4, 0xD9D4D039)
            d = this.md5HH(d, a, b, c, wordArray[i + 12], 11, 0xE6DB99E5)
            c = this.md5HH(c, d, a, b, wordArray[i + 15], 16, 0x1FA27CF8)
            b = this.md5HH(b, c, d, a, wordArray[i + 2], 23, 0xC4AC5665)

            // 第四轮
            a = this.md5II(a, b, c, d, wordArray[i + 0], 6, 0xF4292244)
            d = this.md5II(d, a, b, c, wordArray[i + 7], 10, 0x432AFF97)
            c = this.md5II(c, d, a, b, wordArray[i + 14], 15, 0xAB9423A7)
            b = this.md5II(b, c, d, a, wordArray[i + 5], 21, 0xFC93A039)
            a = this.md5II(a, b, c, d, wordArray[i + 12], 6, 0x655B59C3)
            d = this.md5II(d, a, b, c, wordArray[i + 3], 10, 0x8F0CCC92)
            c = this.md5II(c, d, a, b, wordArray[i + 10], 15, 0xFFEFF47D)
            b = this.md5II(b, c, d, a, wordArray[i + 1], 21, 0x85845DD1)
            a = this.md5II(a, b, c, d, wordArray[i + 8], 6, 0x6FA87E4F)
            d = this.md5II(d, a, b, c, wordArray[i + 15], 10, 0xFE2CE6E0)
            c = this.md5II(c, d, a, b, wordArray[i + 6], 15, 0xA3014314)
            b = this.md5II(b, c, d, a, wordArray[i + 13], 21, 0x4E0811A1)
            a = this.md5II(a, b, c, d, wordArray[i + 4], 6, 0xF7537E82)
            d = this.md5II(d, a, b, c, wordArray[i + 11], 10, 0xBD3AF235)
            c = this.md5II(c, d, a, b, wordArray[i + 2], 15, 0x2AD7D2BB)
            b = this.md5II(b, c, d, a, wordArray[i + 9], 21, 0xEB86D391)

            // 添加到原始值
            a = this.addUnsigned(a, aa)
            b = this.addUnsigned(b, bb)
            c = this.addUnsigned(c, cc)
            d = this.addUnsigned(d, dd)
        }

        // 返回MD5哈希值
        return (this.wordToHex(a) + this.wordToHex(b) + this.wordToHex(c) + this.wordToHex(d)).toLowerCase()
    }

    /**
     * 生成签名（便捷方法）
     * @param {string} appId - 应用ID
     * @param {string} secretKey - 密钥
     * @param {string} timestamp - 时间戳
     * @returns {string} MD5签名
     */
    static generateSignature(appId, secretKey, timestamp) {
        // 根据后端SignUtil的逻辑生成签名
        const signString = "app" + appId + "secret" + secretKey + "timestamp" + timestamp
        return this.hash(signString)
    }
}

/**
 * 便捷函数：计算字符串的MD5哈希值
 * @param {string} str - 要计算哈希的字符串
 * @returns {string} MD5哈希值（32位十六进制字符串）
 */
function md5(str) {
    return MD5.hash(str)
}

/**
 * 便捷函数：生成API签名
 * @param {string} appId - 应用ID
 * @param {string} secretKey - 密钥
 * @param {string} timestamp - 时间戳
 * @returns {string} MD5签名
 */
function generateSignature(appId, secretKey, timestamp) {
    return MD5.generateSignature(appId, secretKey, timestamp)
}

// 导出
if (typeof module !== 'undefined' && module.exports) {
    // Node.js 环境
    module.exports = { MD5, md5, generateSignature }
} else if (typeof window !== 'undefined') {
    // 浏览器环境
    window.MD5 = MD5
    window.md5 = md5
    window.generateSignature = generateSignature
}

// ES6 模块导出（如果支持）
if (typeof exports !== 'undefined') {
    exports.MD5 = MD5
    exports.md5 = md5
    exports.generateSignature = generateSignature
}