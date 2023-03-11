package com.gdsc.eyekey

import com.google.gson.annotations.SerializedName
import java.io.File

data class ResultImg(
    @SerializedName("resultImg")
    private val resultImg: String){
    override fun toString() = "PostResult{ ${resultImg}}}"
}


