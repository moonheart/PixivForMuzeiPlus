package moe.democyann.pixivformuzeiplus

open class PixivException(message:String):Exception(message) {
}

class LoginExpiredException:PixivException("登陆过期"){}