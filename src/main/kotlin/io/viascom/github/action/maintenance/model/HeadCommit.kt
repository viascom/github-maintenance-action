package io.viascom.github.action.maintenance.model

import com.google.gson.annotations.SerializedName


data class HeadCommit(

    @SerializedName("id")
    var id: String? = null,

    @SerializedName("tree_id")
    var treeId: String? = null,

    @SerializedName("message")
    var message: String? = null,

    @SerializedName("timestamp")
    var timestamp: String? = null,

    @SerializedName("author")
    var author: Author? = Author(),

    @SerializedName("committer")
    var committer: Author? = Author()
)