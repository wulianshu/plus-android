package com.zhiyi.rxdownload3.database

import io.reactivex.Maybe
import com.zhiyi.rxdownload3.core.Mission
import com.zhiyi.rxdownload3.core.RealMission


interface DbActor {

    fun init()

    fun isExists(mission: RealMission): Boolean

    fun create(mission: RealMission)

    fun read(mission: RealMission)

    fun update(mission: RealMission)

    fun updateStatus(mission: RealMission)

    fun delete(mission: RealMission)

    fun getAllMission(): Maybe<List<Mission>>
}