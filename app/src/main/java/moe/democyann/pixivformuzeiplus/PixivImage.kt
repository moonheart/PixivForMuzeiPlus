package moe.democyann.pixivformuzeiplus

/**
 * 图片详细信息
 */
class PixivImage {
    lateinit var title: String
    lateinit var author: String
    lateinit var token: String
    lateinit var pixivUrl: String
    lateinit var description: String
    lateinit var illustId:String
    lateinit var tags: List<String>
    var isR18: Boolean = false
    lateinit var authorId: String
    lateinit var url_original :String
    var height = 0
    var width = 0
    var bookmark_user_total = 0
    var rating_count = 0
    var rating_view = 0

    constructor(title: String, author: String, token: String, pixivUrl: String?, description: String) {
        this.title = title
        this.author = author
        this.token = token
        this.pixivUrl = pixivUrl!!
        this.description = description
    }

    constructor()

}